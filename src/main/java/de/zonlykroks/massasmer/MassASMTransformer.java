package de.zonlykroks.massasmer;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MassASMTransformer extends GameTransformer {
    private static final LoggerWrapper LOGGER = new LoggerWrapper(LogManager.getLogger("MassASMTransformer"), MassasmerPreLaunch.configManager.isLogEnabled());

    // List of named transformer entries
    private static final List<NamedTransformerEntry> TRANSFORMERS = new ArrayList<>();

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

        for (NamedTransformerEntry entry : TRANSFORMERS) {
            if (entry.matches(className)) {
                byte[] transformed = entry.transform(className, result);
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
    public static void register(String name,
                                Predicate<String> filter,
                                ClassTransformer transformer) {
        if(MassasmerPreLaunch.hasFailedToAttach()) {
            LOGGER.error("MassASMTransformer: Failed to attach, cowardly refusing to register transformer!");
            return;
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Transformer name must be non-empty");
        }
        LOGGER.info("MassASMTransformer: Registering transformer '{}' for {}", name, filter);
        TRANSFORMERS.add(new NamedTransformerEntry(name, filter, transformer));
    }

    /**
     * Register a ClassNode based transformer with a class filter and name
     */
    public static void registerNodeTransformer(String name,
                                               Predicate<String> filter,
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
                                       Predicate<String> filter,
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
    private record NamedTransformerEntry(String name, Predicate<String> filter, ClassTransformer transformer) {

        boolean matches(String className) {
            return filter.test(className);
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
