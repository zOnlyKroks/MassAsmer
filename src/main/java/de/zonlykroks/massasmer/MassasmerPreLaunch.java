package de.zonlykroks.massasmer;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

import java.lang.reflect.Field;
import java.util.logging.Logger;

public class MassasmerPreLaunch implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        MinecraftGameProvider provider = (MinecraftGameProvider)
                ((FabricLoaderImpl) FabricLoader.getInstance()).getGameProvider();

        try {
            Field transformerField = MinecraftGameProvider.class.getDeclaredField("transformer");

            transformerField.setAccessible(true);
            transformerField.set(provider, new MassASMTransformer());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
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
}
