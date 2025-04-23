package de.zonlykroks.massasmer;

import de.zonlykroks.massasmer.config.MassAsmConfigManager;
import de.zonlykroks.massasmer.util.UnrecoverableMassASMRuntimeError;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

import java.lang.reflect.Field;

public class MassasmerPreLaunch implements PreLaunchEntrypoint {

    public static final Logger LOGGER = LogManager.getLogger("Massasmer-Prelaunch");
    private final MassAsmConfigManager configManager = new MassAsmConfigManager();

    private static boolean hasFailedToAttach = false;

    @Override
    public void onPreLaunch() {
        LOGGER.info("Starting MassASM pre-launch process...");

        MinecraftGameProvider provider = (MinecraftGameProvider)
                ((FabricLoaderImpl) FabricLoader.getInstance()).getGameProvider();

        try {
            Field transformerField = MinecraftGameProvider.class.getDeclaredField("transforme");

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

        callApiRegistrationPoints();
    }

    private void callApiRegistrationPoints() {
        FabricLoader.getInstance()
                .getEntrypointContainers(
                        "mass-asm",
                        Runnable.class
                )
                .stream()
                .map(EntrypointContainer::getEntrypoint)
                .forEach(Runnable::run);
    }

    public static boolean hasFailedToAttach() {
        return hasFailedToAttach;
    }
}
