package de.zonlykroks.massasmer;

import de.zonlykroks.massasmer.config.MassAsmConfigManager;
import de.zonlykroks.massasmer.util.LoggerWrapper;
import de.zonlykroks.massasmer.util.UnrecoverableMassASMRuntimeError;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class MassasmerPreLaunch implements PreLaunchEntrypoint {

    //Keep this dang thing in order
    public static final MassAsmConfigManager configManager = new MassAsmConfigManager();
    public static final LoggerWrapper LOGGER = new LoggerWrapper(LogManager.getLogger("Massasmer-Prelaunch"), MassasmerPreLaunch.configManager.isLogEnabled());

    @Getter
    private static boolean hasFailedToAttach = false;

    //Dont you thuch my registry state!
    private static volatile RegistryState registryState = RegistryState.FROZEN;
    private static final long INIT_TOKEN = System.nanoTime() ^ System.identityHashCode(MassasmerPreLaunch.class);

    public static boolean isRegistryFrozen() {
        return registryState == RegistryState.FROZEN;
    }

    static void requireUnfrozen() {
        if (isRegistryFrozen()) {
            throw new IllegalStateException(
                "Cannot register transformers - registry is frozen. " +
                "Transformers must be registered during the 'mass-asm' entrypoint phase."
            );
        }
    }

    private static void setRegistryState(RegistryState state) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        boolean validCaller = false;
        for (StackTraceElement elem : stack) {
            if (elem.getClassName().equals(MassasmerPreLaunch.class.getName())
                && elem.getMethodName().equals("callApiRegistrationPoints")) {
                validCaller = true;
                break;
            }
        }

        if (!validCaller && INIT_TOKEN != 0) {
            throw new SecurityException("Unauthorized attempt to modify registry state");
        }

        registryState = state;
    }

    private enum RegistryState {
        FROZEN, OPEN
    }

    @Override
    public void onPreLaunch() {
        LOGGER.info("Starting MassASM pre-launch process...");

        LOGGER.info("Default transformer exclusions: {}", configManager.getExclusionsString());

        MinecraftGameProvider provider = (MinecraftGameProvider)
                ((FabricLoaderImpl) FabricLoader.getInstance()).getGameProvider();

        try {
            Field transformerField = MinecraftGameProvider.class
                    .getDeclaredField("transformer");
            transformerField.setAccessible(true);

            transformerField.set(provider, getMassASMTransformer(transformerField, provider));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            if (!configManager.isAllowAttachNonFailHard()) {
                throw new UnrecoverableMassASMRuntimeError("Cannot set custom game provider transformer, failing!", e);
            } else {
                LOGGER.error("Cannot set custom game provider transformer, non-hard failure allowed by config", e);
            }
            hasFailedToAttach = true;
        }

        LOGGER.info("MassASM pre-launch process completed, calling entrypoints...");
        callApiRegistrationPoints();
        LOGGER.info("MassASM pre-launch process finished. Watch your JVM—it can still implode if transforms go wrong!");
    }

    private static @NotNull MassASMTransformer getMassASMTransformer(Field transformerField, MinecraftGameProvider provider) throws IllegalAccessException, NoSuchFieldException {
        GameTransformer oldTransformer = (GameTransformer) transformerField
                .get(provider);

        Field patchesField = GameTransformer.class
                .getDeclaredField("patches");
        patchesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<GamePatch> patches = (List<GamePatch>) patchesField
                .get(oldTransformer);

        Field classesField = GameTransformer.class
                .getDeclaredField("patchedClasses");
        classesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, byte[]> patchedClasses = (Map<String, byte[]>) classesField
                .get(oldTransformer);

        Field locatedField = GameTransformer.class
                .getDeclaredField("entrypointsLocated");
        locatedField.setAccessible(true);
        boolean entrypointsLocated = locatedField.getBoolean(oldTransformer);

        return new MassASMTransformer(
                patches, patchedClasses, entrypointsLocated);
    }

    private void callApiRegistrationPoints() {
        setRegistryState(RegistryState.OPEN);
        try {
            FabricLoader.getInstance()
                    .getEntrypointContainers("mass-asm", Runnable.class)
                    .stream()
                    .map(EntrypointContainer::getEntrypoint)
                    .forEach(runnable -> {
                        LOGGER.info("Registering Entrypoint {}", runnable.getClass().getName());
                        runnable.run();
                    });
        } finally {
            setRegistryState(RegistryState.FROZEN);
        }
    }
}
