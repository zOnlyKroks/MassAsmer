package de.zonlykroks.massasmer;

import de.zonlykroks.massasmer.filter.impl.NamePatternFilter;
import de.zonlykroks.massasmer.filter.api.TransformerFilter;
import de.zonlykroks.massasmer.util.LoggerWrapper;
import de.zonlykroks.massasmer.util.UnrecoverableMassASMRuntimeError;
import lombok.experimental.Delegate;
import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;

import org.apache.logging.log4j.LogManager;

public class MassASMTransformer extends GameTransformer {
    private static final LoggerWrapper LOGGER = new LoggerWrapper(LogManager.getLogger("MassASMTransformer"), MassasmerPreLaunch.configManager.isLogEnabled());

    private static final Map<String, List<NamedTransformerEntry>> EXACT_TRANSFORMERS = new HashMap<>();
    private static final Map<String, List<NamedTransformerEntry>> PREFIX_TRANSFORMERS = new HashMap<>();
    private static final Map<String, List<NamedTransformerEntry>> SUFFIX_TRANSFORMERS = new HashMap<>();
    private static final List<NamedTransformerEntry> CONTAINS_TRANSFORMERS = new ArrayList<>();
    private static final List<NamedTransformerEntry> OTHER_TRANSFORMERS = new ArrayList<>();

    private final Map<String, byte[]> additionalTransformedClasses = new HashMap<>();
    private boolean massTransformersApplied = false;
    private boolean entrypointsLocated = false;

    @Delegate(excludes = TransformExclusions.class)
    private final GameTransformer delegate;

    public MassASMTransformer(GamePatch... patches) {
        super(patches);
        this.delegate = this;
    }

    private interface TransformExclusions {
        byte[] transform(String className);
    }

    @Override
    public void locateEntrypoints(FabricLauncher launcher, List<Path> gameJars) {
        super.locateEntrypoints(launcher, gameJars);
        entrypointsLocated = true;
        applyMassTransformers();
    }

    @Override
    public byte[] transform(String className) {
        if (additionalTransformedClasses.containsKey(className)) {
            return additionalTransformedClasses.get(className);
        }

        if (!entrypointsLocated) {
            try {
                byte[] classBytes = getClassBytesFromClassLoader(className);
                if (classBytes != null) {
                    byte[] transformed = applyTransformers(className, classBytes);
                    if (transformed != null) {
                        additionalTransformedClasses.put(className, transformed);
                        return transformed;
                    }
                }
            } catch (Exception e) {
                throw new UnrecoverableMassASMRuntimeError("Error transforming " + className + " before entrypoints located", e);
            }
            return null;
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

    private void applyMassTransformers() {
        if (massTransformersApplied) return;
        try {
            Field patchedClassesField = GameTransformer.class.getDeclaredField("patchedClasses");
            patchedClassesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, byte[]> patchedClasses = (Map<String, byte[]>) patchedClassesField.get(this);

            if (patchedClasses != null) {
                for (String className : new HashMap<>(patchedClasses).keySet()) {
                    byte[] originalBytes = patchedClasses.get(className);
                    byte[] transformedBytes = applyTransformers(className, originalBytes);
                    if (transformedBytes != null) {
                        additionalTransformedClasses.put(className, transformedBytes);
                        patchedClasses.put(className, transformedBytes);
                    }
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new UnrecoverableMassASMRuntimeError("MassASMTransformer: Failed to access patchedClasses", e);
        }
        massTransformersApplied = true;
    }

    private byte[] applyTransformers(String className, byte[] classBytes) {
        if (classBytes == null) return null;

        byte[] result = classBytes;
        boolean modified = false;

        // Check exact matches first
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

        // Check prefixes
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

        for(NamedTransformerEntry transformer : CONTAINS_TRANSFORMERS) {
            if (transformer.matches(className)) {
                byte[] transformed = transformer.transform(className, result);
                if (transformed != null) {
                    result = transformed;
                    modified = true;
                }
            }
        }

        for(NamedTransformerEntry transformer : OTHER_TRANSFORMERS) {
            if (transformer.matches(className)) {
                byte[] transformed = transformer.transform(className, result);
                if (transformed != null) {
                    result = transformed;
                    modified = true;
                }
            }
        }

        return modified ? result : null;
    }

    /**
     * Register a raw bytecode transformer with a class filter and name
     */
    public static void register(String name, TransformerFilter filter, ClassTransformer transformer) {
        LOGGER.info("Registering transformer '{}' for {}", name, filter);
        final NamedTransformerEntry entry = new NamedTransformerEntry(name, filter, transformer);

        if (filter instanceof NamePatternFilter npFilter) {
            String pattern = npFilter.getPattern();

            switch (npFilter.getStrategy()) {
                case EXACT -> EXACT_TRANSFORMERS.computeIfAbsent(pattern, k -> new ArrayList<>()).add(entry);
                case STARTS_WITH -> PREFIX_TRANSFORMERS.computeIfAbsent(pattern, k -> new ArrayList<>()).add(entry);
                case ENDS_WITH -> SUFFIX_TRANSFORMERS.computeIfAbsent(pattern, k -> new ArrayList<>()).add(entry);
                case CONTAINS -> CONTAINS_TRANSFORMERS.add(entry);
            }
        } else {
            LOGGER.warn("Transformer '{}' has no filter, it will be applied to all classes", name);

            OTHER_TRANSFORMERS.add(entry);
        }
    }

    /**
     * Register a ClassNode based transformer with a class filter and name
     */
    public static void registerNodeTransformer(String name,
                                               TransformerFilter filter,
                                               ClassNodeTransformer transformer) {
        register(name, filter, (className, classBytes) -> {
            ClassReader reader = new ClassReader(classBytes);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.EXPAND_FRAMES);

            boolean modified = transformer.transform(className, node);

            if (modified) {
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                node.accept(writer);
                return writer.toByteArray();
            }
            return null;
        });
    }

    /**
     * Register a visitor-based transformer with a class filter and name
     */
    public static void registerVisitor(String name,
                                       TransformerFilter filter,
                                       VisitorProvider visitorProvider) {
        register(name, filter, (className, classBytes) -> {
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = visitorProvider.createVisitor(className, writer);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        });
    }

    // Internal named entry
    private record NamedTransformerEntry(String name, TransformerFilter filter, ClassTransformer transformer) {
        boolean matches(String className) {
            return filter.matches(className);
        }

        byte[] transform(String className, byte[] bytes) {
            return transformer.transform(className, bytes);
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
