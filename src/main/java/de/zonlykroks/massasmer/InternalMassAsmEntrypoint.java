package de.zonlykroks.massasmer;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class InternalMassAsmEntrypoint implements Runnable {
    @Override
    public void run() {
        registerInternalTransformers();
    }

    private void registerInternalTransformers() {
        MassASMTransformer.registerVisitor(
                "massasm-internal-inject-init-stdout",
                className -> className.equals("net.minecraft.client.Minecraft"),
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
                        // Inject at top of <init>:
                        // System.out.println("[MassASM] net.minecraft.client.Minecraft ctor entered");
                        visitFieldInsn(Opcodes.GETSTATIC,
                                "java/lang/System",
                                "out",
                                "Ljava/io/PrintStream;");
                        visitLdcInsn("[MassASM] " + className + " <init> called, this means our transformer is working, shenanigans now probably ensue. If something fails, look at the weird mod using this API!");
                        visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                "java/io/PrintStream",
                                "println",
                                "(Ljava/lang/String;)V",
                                false);
                    }
                };
            }

            return mv;
        }
    }
}
