package de.zonlykroks.massasmer;

import de.zonlykroks.massasmer.config.MassAsmConfigManager;
import de.zonlykroks.massasmer.util.LoggerWrapper;
import de.zonlykroks.massasmer.util.UnrecoverableMassASMRuntimeError;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Field;

public class MassasmerPreLaunch implements PreLaunchEntrypoint {

    //Keep this dang thing in order
    public static final MassAsmConfigManager configManager = new MassAsmConfigManager();
    public static final LoggerWrapper LOGGER = new LoggerWrapper(LogManager.getLogger("Massasmer-Prelaunch"), MassasmerPreLaunch.configManager.isLogEnabled());

    private static boolean hasFailedToAttach = false;

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
            if(!configManager.isAllowAttachNonFailHard()) {
                throw new UnrecoverableMassASMRuntimeError("Cannot set custom game provider transformer, failing!", e);
            } else {
                LOGGER.error("Cannot set custom game provider transformer, failing non-hard as config allows this! (If this is intentional, get help please)", e);
            }
            hasFailedToAttach = true;
        }

        LOGGER.info("MassASM pre-launch process completed, calling entrypoints...");
        callApiRegistrationPoints();
        LOGGER.info("MassASM pre-launch process completed, entrypoints called.");
        LOGGER.info("Be aware that every transform that now happens, can (and likely will if done incorrectly) implode your whole jvm!");
        LOGGER.info("I took the courtesy to log any classes that register transformers in a supported way. If they dont use my entrypoint, well shit. More log combing for you");
    }

    private void callApiRegistrationPoints() {
        FabricLoader.getInstance()
                .getEntrypointContainers("mass-asm", Runnable.class)
                .stream()
                .map(EntrypointContainer::getEntrypoint)
                .forEach(runnable -> {
                    LOGGER.info("Registering Entrypoint {}", runnable.getClass().getName());
                    runnable.run();
                });

    }

    public static boolean hasFailedToAttach() {
        return hasFailedToAttach;
    }
}
