package de.zonlykroks.massasmer;

import de.zonlykroks.massasmer.filter.NamePatternFilter;
import de.zonlykroks.massasmer.util.LoggerWrapper;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class InternalMassAsmEntrypoint implements Runnable {

    @SuppressWarnings("unused")
    public static final LoggerWrapper INTERNAL_TEST_LOGGER = new LoggerWrapper(LogManager.getLogger("Massasmer-test-logger"), true);

    @Override
    public void run() {
        registerInternalTransformers();
    }

    private void registerInternalTransformers() {
        MassASMTransformer.registerVisitor(
                "massasm-internal-inject-init-stdout",
                new NamePatternFilter(FabricLauncherBase.getLauncher().isDevelopment() ? "net.minecraft.client.Minecraft" : "net.minecraft.client.main.Main$2", true, false,false,false),
                (className, nextVisitor) -> new CreateTitlePrintTransformer(Opcodes.ASM9, nextVisitor, className)
        );
    }

    private static class CreateTitlePrintTransformer extends ClassVisitor {
        private final String className;

        public CreateTitlePrintTransformer(int api, ClassVisitor next, String className) {
            super(api, next);
            this.className = className;
        }

        @Override
        public MethodVisitor visitMethod(int access,
                                         String name,
                                         String descriptor,
                                         String signature,
                                         String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

            if (name.equals("<init>")) {
                return new AdviceAdapter(api, mv, access, name, descriptor) {
                    @Override
                    protected void onMethodEnter() {
                        // Injecting the logger call into the constructor
                        // INTERNAL_TEST_LOGGER.info("[MassASM] <init> called for " + className);

                        // Load the logger
                        visitFieldInsn(Opcodes.GETSTATIC,
                                "de/zonlykroks/massasmer/InternalMassAsmEntrypoint",
                                "INTERNAL_TEST_LOGGER",
                                "Lde/zonlykroks/massasmer/util/LoggerWrapper;");

                        // Load the log message
                        visitLdcInsn("[MassASM] " + className + " <init> called, this means our transformer is working, shenanigans now probably ensue. If something fails, look at the weird mod using this API!");

                        // Call the logger's info() method
                        visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                "de/zonlykroks/massasmer/util/LoggerWrapper",
                                "info",
                                "(Ljava/lang/String;)V",
                                false);
                    }
                };
            }

            return mv;
        }
    }
}
