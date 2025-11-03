package de.zonlykroks.massasmer;

import de.zonlykroks.massasmer.api.TransformerRegistration;
import de.zonlykroks.massasmer.filter.impl.NamePatternFilter;
import de.zonlykroks.massasmer.filter.api.TransformerFilter;
import de.zonlykroks.massasmer.util.LoggerWrapper;
import de.zonlykroks.massasmer.util.TransformerException;
import de.zonlykroks.massasmer.util.UnrecoverableMassASMRuntimeError;
import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;

public class MassASMTransformer extends GameTransformer {
    private static final LoggerWrapper LOGGER = new LoggerWrapper(LogManager.getLogger("MassASMTransformer"), MassasmerPreLaunch.configManager.isLogEnabled());

    private static final Map<String, List<NamedTransformerEntry>> EXACT_TRANSFORMERS = new HashMap<>();
    private static final Map<String, List<NamedTransformerEntry>> PREFIX_TRANSFORMERS = new HashMap<>();
    private static final Map<String, List<NamedTransformerEntry>> SUFFIX_TRANSFORMERS = new HashMap<>();
    private static final List<NamedTransformerEntry> CONTAINS_TRANSFORMERS = new ArrayList<>();
    private static final List<NamedTransformerEntry> OTHER_TRANSFORMERS = new ArrayList<>();

    private final Map<String, byte[]> additionalTransformedClasses = new HashMap<>();

    public MassASMTransformer(
            List<GamePatch> originalPatches,
            Map<String, byte[]> originalPatchedClasses,
            boolean originalEntrypointsLocated
    ) {
        super(originalPatches.toArray(new GamePatch[0]));

        try {
            Field patchesField = GameTransformer.class
                    .getDeclaredField("patches");
            patchesField.setAccessible(true);

            patchesField.set(this, originalPatches);

            Field classesField = GameTransformer.class
                    .getDeclaredField("patchedClasses");
            classesField.setAccessible(true);
            classesField.set(this, originalPatchedClasses);

            Field locatedField = GameTransformer.class
                    .getDeclaredField("entrypointsLocated");
            locatedField.setAccessible(true);
            locatedField.setBoolean(this, originalEntrypointsLocated);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            // something went horribly wrong if we can't set these
            throw new UnrecoverableMassASMRuntimeError(
                    "Failed to steal GameTransformer state", e);
        }

        LOGGER.info("MassASMTransformer initialized with {} patches",
                originalPatches.size());
    }

    @Override
    public byte[] transform(String className) {
        if (!MassasmerPreLaunch.configManager.getTransformerExclusionFilter().matches(className)) {
            return null;
        }

        if (additionalTransformedClasses.containsKey(className)) {
            return additionalTransformedClasses.get(className);
        }

        try {
            byte[] originalResult = super.transform(className);
            if (originalResult != null) {
                byte[] transformed = applyTransformers(className, originalResult);
                if (transformed != null) {
                    additionalTransformedClasses.put(className, transformed);
                    return transformed;
                }
                return originalResult;
            }

            byte[] classBytes = getClassBytesFromClassLoader(className);
            if (classBytes != null) {
                byte[] transformed = applyTransformers(className, classBytes);
                if (transformed != null) {
                    additionalTransformedClasses.put(className, transformed);
                    return transformed;
                }
            }
        } catch (Exception e) {
            throw new UnrecoverableMassASMRuntimeError("Error transforming " + className, e);
        }

        return null;
    }

    private byte[] getClassBytesFromClassLoader(String className) {
        try {
            String resourceName = className.replace('.', '/') + ".class";
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
                if (is == null) return null;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toByteArray();
            }
        } catch (IOException e) {
            return null;
        }
    }

    private byte[] applyTransformers(String className, byte[] classBytes) {
        if (classBytes == null) return null;

        byte[] result = classBytes;
        boolean modified = false;

        // Check exact matches first (fastest path)
        if (!EXACT_TRANSFORMERS.isEmpty()) {
            List<NamedTransformerEntry> exact = EXACT_TRANSFORMERS.get(className);
            if (exact != null) {
                for (NamedTransformerEntry entry : exact) {
                    byte[] transformed = entry.transform(className, result);
                    if (transformed != null) {
                        result = transformed;
                        modified = true;
                    }
                }
            }
        }

        // Check prefixes
        if (!PREFIX_TRANSFORMERS.isEmpty()) {
            for (Map.Entry<String, List<NamedTransformerEntry>> entry : PREFIX_TRANSFORMERS.entrySet()) {
                if (className.startsWith(entry.getKey())) {
                    for (NamedTransformerEntry transformer : entry.getValue()) {
                        byte[] transformed = transformer.transform(className, result);
                        if (transformed != null) {
                            result = transformed;
                            modified = true;
                        }
                    }
                }
            }
        }

        // Check suffixes
        if (!SUFFIX_TRANSFORMERS.isEmpty()) {
            for(Map.Entry<String, List<NamedTransformerEntry>> entry : SUFFIX_TRANSFORMERS.entrySet()) {
                if (className.endsWith(entry.getKey())) {
                    for (NamedTransformerEntry transformer : entry.getValue()) {
                        byte[] transformed = transformer.transform(className, result);
                        if (transformed != null) {
                            result = transformed;
                            modified = true;
                        }
                    }
                }
            }
        }

        // Check contains patterns
        if (!CONTAINS_TRANSFORMERS.isEmpty()) {
            for(NamedTransformerEntry transformer : CONTAINS_TRANSFORMERS) {
                if (transformer.matches(className)) {
                    byte[] transformed = transformer.transform(className, result);
                    if (transformed != null) {
                        result = transformed;
                        modified = true;
                    }
                }
            }
        }

        // Check other/custom filters
        if (!OTHER_TRANSFORMERS.isEmpty()) {
            for(NamedTransformerEntry transformer : OTHER_TRANSFORMERS) {
                if (transformer.matches(className)) {
                    byte[] transformed = transformer.transform(className, result);
                    if (transformed != null) {
                        result = transformed;
                        modified = true;
                    }
                }
            }
        }

        return modified ? result : null;
    }

    /**
     * Register a raw bytecode transformer with a class filter and name
     * @param name Unique name for the transformer
     * @param filter Filter to determine which classes to transform
     * @param transformer The transformer implementation
     * @return TransformerRegistration handle for controlling this transformer
     */
    public static TransformerRegistration register(String name, TransformerFilter filter, ClassTransformer transformer) {
        return register(name, filter, transformer, 0);
    }

    /**
     * Register a raw bytecode transformer with a class filter, name, and priority
     * @param name Unique name for the transformer
     * @param filter Filter to determine which classes to transform
     * @param transformer The transformer implementation
     * @param priority Priority (higher executes first, default is 0)
     * @return TransformerRegistration handle for controlling this transformer
     */
    public static TransformerRegistration register(String name, TransformerFilter filter, ClassTransformer transformer, int priority) {
        MassasmerPreLaunch.requireUnfrozen();
        LOGGER.info("Registering transformer '{}' with priority {} for {}", name, priority, filter);
        final NamedTransformerEntry entry = new NamedTransformerEntry(name, filter, transformer, priority);

        if (filter instanceof NamePatternFilter(String pattern, NamePatternFilter.MatchStrategy strategy)) {

            switch (strategy) {
                case EXACT -> addSortedByPriority(EXACT_TRANSFORMERS.computeIfAbsent(pattern, k -> new ArrayList<>()), entry);
                case STARTS_WITH -> addSortedByPriority(PREFIX_TRANSFORMERS.computeIfAbsent(pattern, k -> new ArrayList<>()), entry);
                case ENDS_WITH -> addSortedByPriority(SUFFIX_TRANSFORMERS.computeIfAbsent(pattern, k -> new ArrayList<>()), entry);
                case CONTAINS -> addSortedByPriority(CONTAINS_TRANSFORMERS, entry);
            }
        } else {
            LOGGER.warn("Transformer '{}' has no NamePatternFilter, it will be applied to all matching classes", name);
            addSortedByPriority(OTHER_TRANSFORMERS, entry);
        }

        return entry;
    }

    /**
     * Helper method to add transformers sorted by priority (higher priority first)
     */
    private static void addSortedByPriority(List<NamedTransformerEntry> list, NamedTransformerEntry entry) {
        int insertIndex = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).priority() < entry.priority()) {
                insertIndex = i;
                break;
            }
            insertIndex = i + 1;
        }
        list.add(insertIndex, entry);
    }

    /**
     * Register a ClassNode based transformer with a class filter and name
     * @param name Unique name for the transformer
     * @param filter Filter to determine which classes to transform
     * @param transformer The transformer implementation
     * @return TransformerRegistration handle for controlling this transformer
     */
    public static TransformerRegistration registerNodeTransformer(String name,
                                               TransformerFilter filter,
                                               ClassNodeTransformer transformer) {
        return registerNodeTransformer(name, filter, transformer, 0);
    }

    /**
     * Register a ClassNode based transformer with a class filter, name, and priority
     * @param name Unique name for the transformer
     * @param filter Filter to determine which classes to transform
     * @param transformer The transformer implementation
     * @param priority Priority (higher executes first, default is 0)
     * @return TransformerRegistration handle for controlling this transformer
     */
    public static TransformerRegistration registerNodeTransformer(String name,
                                               TransformerFilter filter,
                                               ClassNodeTransformer transformer,
                                               int priority) {
        return register(name, filter, (className, classBytes) -> {
            ClassReader reader = new ClassReader(classBytes);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.EXPAND_FRAMES);

            boolean modified = transformer.transform(className, node);

            if (modified) {
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                node.accept(writer);
                return writer.toByteArray();
            }
            return null;
        }, priority);
    }

    /**
     * Register a visitor-based transformer with a class filter and name
     * @param name Unique name for the transformer
     * @param filter Filter to determine which classes to transform
     * @param visitorProvider Provider for creating the ClassVisitor
     * @return TransformerRegistration handle for controlling this transformer
     */
    public static TransformerRegistration registerVisitor(String name,
                                       TransformerFilter filter,
                                       VisitorProvider visitorProvider) {
        return registerVisitor(name, filter, visitorProvider, 0);
    }

    /**
     * Register a visitor-based transformer with a class filter, name, and priority
     * @param name Unique name for the transformer
     * @param filter Filter to determine which classes to transform
     * @param visitorProvider Provider for creating the ClassVisitor
     * @param priority Priority (higher executes first, default is 0)
     * @return TransformerRegistration handle for controlling this transformer
     */
    public static TransformerRegistration registerVisitor(String name,
                                       TransformerFilter filter,
                                       VisitorProvider visitorProvider,
                                       int priority) {
        return register(name, filter, (className, classBytes) -> {
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassVisitor visitor = visitorProvider.createVisitor(className, writer);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        }, priority);
    }

    /**
     * Conditionally register a transformer only if the condition is true
     * @param name Unique name for the transformer
     * @param filter Filter to determine which classes to transform
     * @param transformer The transformer implementation
     * @param condition Only register if this is true
     * @return TransformerRegistration handle, or a no-op registration if condition is false
     */
    public static TransformerRegistration registerConditional(String name,
                                                              TransformerFilter filter,
                                                              ClassTransformer transformer,
                                                              boolean condition) {
        return registerConditional(name, filter, transformer, 0, condition);
    }

    /**
     * Conditionally register a transformer only if the condition is true
     * @param name Unique name for the transformer
     * @param filter Filter to determine which classes to transform
     * @param transformer The transformer implementation
     * @param priority Priority (higher executes first, default is 0)
     * @param condition Only register if this is true
     * @return TransformerRegistration handle, or a no-op registration if condition is false
     */
    public static TransformerRegistration registerConditional(String name,
                                                              TransformerFilter filter,
                                                              ClassTransformer transformer,
                                                              int priority,
                                                              boolean condition) {
        if (condition) {
            return register(name, filter, transformer, priority);
        }
        LOGGER.info("Skipping transformer '{}' due to condition", name);
        return NoOpRegistration.INSTANCE;
    }

    /**
     * Create a builder for more complex transformer registrations
     * @param name Unique name for the transformer
     * @return A new TransformerBuilder
     */
    public static TransformerBuilder builder(String name) {
        return new TransformerBuilder(name);
    }

    // Internal named entry implementing TransformerRegistration
    private static class NamedTransformerEntry implements TransformerRegistration {
        private final String name;
        private final TransformerFilter filter;
        private final ClassTransformer transformer;
        private final int priority;
        private final AtomicBoolean enabled;

        public NamedTransformerEntry(String name, TransformerFilter filter, ClassTransformer transformer, int priority) {
            this.name = name;
            this.filter = filter;
            this.transformer = transformer;
            this.priority = priority;
            this.enabled = new AtomicBoolean(true);
        }

        boolean matches(String className) {
            return enabled.get() && filter.matches(className);
        }

        byte[] transform(String className, byte[] bytes) {
            if (!enabled.get()) {
                return null;
            }
            try {
                return transformer.transform(className, bytes);
            } catch (Exception e) {
                throw new TransformerException(name, className, e);
            }
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public TransformerFilter filter() {
            return filter;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public boolean isEnabled() {
            return enabled.get();
        }

        @Override
        public void disable() {
            enabled.set(false);
            LOGGER.info("Disabled transformer '{}'", name);
        }

        @Override
        public void enable() {
            enabled.set(true);
            LOGGER.info("Enabled transformer '{}'", name);
        }
    }

    private static class NoOpRegistration implements TransformerRegistration {
        static final NoOpRegistration INSTANCE = new NoOpRegistration();

        private NoOpRegistration() {}

        @Override
        public String name() {
            return "noop";
        }

        @Override
        public TransformerFilter filter() {
            return className -> false;
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void disable() {
            // No-op
        }

        @Override
        public void enable() {
            // No-op
        }
    }

    public static class TransformerBuilder {
        private final String name;
        private TransformerFilter filter;
        private int priority = 0;
        private boolean condition = true;
        private ClassTransformer transformer;
        private ClassNodeTransformer nodeTransformer;
        private VisitorProvider visitorProvider;

        private TransformerBuilder(String name) {
            this.name = name;
        }

        /**
         * Set the filter for matching classes
         */
        public TransformerBuilder filter(TransformerFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Set the priority (higher executes first)
         */
        public TransformerBuilder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Set a condition - only register if true
         */
        public TransformerBuilder condition(boolean condition) {
            this.condition = condition;
            return this;
        }

        /**
         * Only register in development mode
         */
        public TransformerBuilder onlyInDev() {
            return condition(net.fabricmc.loader.impl.launch.FabricLauncherBase.getLauncher().isDevelopment());
        }

        /**
         * Only register in production mode
         */
        public TransformerBuilder onlyInProduction() {
            return condition(!net.fabricmc.loader.impl.launch.FabricLauncherBase.getLauncher().isDevelopment());
        }

        /**
         * Set a raw bytecode transformer
         */
        public TransformerBuilder transformer(ClassTransformer transformer) {
            this.transformer = transformer;
            return this;
        }

        /**
         * Set a ClassNode transformer
         */
        public TransformerBuilder nodeTransformer(ClassNodeTransformer nodeTransformer) {
            this.nodeTransformer = nodeTransformer;
            return this;
        }

        /**
         * Set a visitor provider
         */
        public TransformerBuilder visitor(VisitorProvider visitorProvider) {
            this.visitorProvider = visitorProvider;
            return this;
        }

        /**
         * Register the transformer with the configured settings
         */
        public TransformerRegistration register() {
            if (filter == null) {
                throw new IllegalStateException("Filter must be set before registering");
            }

            if (!condition) {
                LOGGER.info("Skipping transformer '{}' due to condition", name);
                return NoOpRegistration.INSTANCE;
            }

            // Determine which type of transformer to register
            if (transformer != null) {
                return MassASMTransformer.register(name, filter, transformer, priority);
            } else if (nodeTransformer != null) {
                return registerNodeTransformer(name, filter, nodeTransformer, priority);
            } else if (visitorProvider != null) {
                return registerVisitor(name, filter, visitorProvider, priority);
            } else {
                throw new IllegalStateException("One of transformer, nodeTransformer, or visitorProvider must be set");
            }
        }
    }

    /**
     * Interface for bytecode transformers
     */
    public interface ClassTransformer {
        byte[] transform(String className, byte[] classBytes);
    }

    /**
     * Interface for ASM ClassNode transformers
     */
    public interface ClassNodeTransformer {
        boolean transform(String className, ClassNode classNode);
    }

    /**
     * Interface for creating ASM visitors
     */
    public interface VisitorProvider {
        ClassVisitor createVisitor(String className, ClassVisitor writer);
    }
}
