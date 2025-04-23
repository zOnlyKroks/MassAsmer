package de.zonlykroks.massasmer;

import de.zonlykroks.massasmer.config.MassAsmConfigManager;
import de.zonlykroks.massasmer.util.LoggerWrapper;
import de.zonlykroks.massasmer.util.UnrecoverableMassASMRuntimeError;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;

public class MassasmerPreLaunch implements PreLaunchEntrypoint {

    //Keep this dang thing in order
    public static final MassAsmConfigManager configManager = new MassAsmConfigManager();
    public static final LoggerWrapper LOGGER = new LoggerWrapper(LogManager.getLogger("Massasmer-Prelaunch"), MassasmerPreLaunch.configManager.isLogEnabled());

    @Getter
    private static boolean hasFailedToAttach = false;

    //If you touch this, I will cry :c
    @Getter
    private static boolean registryFrozen = true;

    @Override
    public void onPreLaunch() {
        LOGGER.info("Starting MassASM pre-launch process...");

        MinecraftGameProvider provider = (MinecraftGameProvider)
                ((FabricLoaderImpl) FabricLoader.getInstance()).getGameProvider();

        try {
            Field transformerField = MinecraftGameProvider.class.getDeclaredField("transformer");
            transformerField.setAccessible(true);
            transformerField.set(provider, new MassASMTransformer());
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

    private void callApiRegistrationPoints() {
        registryFrozen = false;
        FabricLoader.getInstance()
                .getEntrypointContainers("mass-asm", Runnable.class)
                .stream()
                .map(EntrypointContainer::getEntrypoint)
                .forEach(runnable -> {
                    LOGGER.info("Registering Entrypoint {}", runnable.getClass().getName());
                    runnable.run();
                });
        registryFrozen = true;
    }
}
