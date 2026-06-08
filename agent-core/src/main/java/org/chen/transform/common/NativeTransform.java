//package org.chen.transform.common;//package org.chen.transform.common;
//
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.chen.utils.ConfigHelper;
//import java.security.ProtectionDomain;
//import java.util.ArrayList;
//import java.util.List;
//
//
//
///**
// * 本地方法拦截 ClassVisitor 实现方式
// */
//public class NativeTransform implements CommonClassFileTransformer {
//
//    private static final Logger log = LogManager.getLogger(NativeTransform.class);
//
//    private final List<String> hookClasses = new ArrayList<>();
//
//    public NativeTransform() {
//        hookClasses.add("sun.management.VMManagementImpl");
//    }
//
//    @Override
//    public List<String> getHookClasses() {
//        return hookClasses;
//    }
//
//    @Override
//    public boolean isHook() {
//        return false;
//    }
//
//    @Override
//    public byte[] transform(ClassLoader loader, String classInterName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) {
//        var className = classInterName.replace("/", ".");
//        if (!this.hookClasses.contains(className)) {
//            return classFileBuffer;
//        }
//        log.info("transform start {}", className);
//        ClassReader cr = new ClassReader(classFileBuffer);
//        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
//        var myClassVisitor = new MyClassVisitor(ASM8, cw);
//        var checkClassAdapter = new CheckClassAdapter(myClassVisitor);
//        cr.accept(checkClassAdapter, ClassReader.EXPAND_FRAMES);
//        log.info("transform end {}", className);
//        return cw.toByteArray();
//    }
//
//    static class MyClassVisitor extends ClassVisitor {
//        public MyClassVisitor(int api) {
//            super(api);
//        }
//
//        public MyClassVisitor(int api, ClassVisitor classVisitor) {
//            super(api, classVisitor);
//        }
//
//        @Override
//        public void visitEnd() {
//            var visitMethod = this.visitMethod(Opcodes.ACC_PUBLIC, "newMethod", "()[Ljava/lang/String;", null, null);
//            visitMethod.visitCode();
//            visitMethod.visitVarInsn(ALOAD, 0);
//            visitMethod.visitMethodInsn(INVOKEVIRTUAL, "sun/management/VMManagementImpl", "getVmArguments0", "()[Ljava/lang/String;", false);
//            visitMethod.visitVarInsn(ASTORE, 1);
//            visitMethod.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//            visitMethod.visitLdcInsn("newMethod");
//            visitMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
//            visitMethod.visitVarInsn(ALOAD, 1);
//            visitMethod.visitMethodInsn(INVOKESTATIC, owner, "getVmArguments", "([Ljava/lang/String;)[Ljava/lang/String;", false);
//            //这里可以插入新的自定义方法 实现本地方法的拦截 （实现方案是修改调用本地方法的地方 修改为调用其他方法 然后在
//            // 这个其他方法内部处理
//            visitMethod.visitInsn(ARETURN);
//            visitMethod.visitEnd();
//            super.visitEnd();
//        }
//
//        @Override
//        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
//            var visitMethod = super.visitMethod(access, name, descriptor, signature, exceptions);
//            if ("getVmArguments".equals(name)) {
//                var mv = new MyMethodVisitor(ASM8, visitMethod);
//
//                //log.info("getVmArguments0:"+access+"\t"+descriptor+"\t"+signature+"\t"+ Arrays.toString(exceptions));
//                return mv;
//            }
//            return visitMethod;
//        }
//
//        static class MyMethodVisitor extends MethodVisitor {
//            public MyMethodVisitor(int i, MethodVisitor mv) {
//                super(i, mv);
//            }
//
//            @Override
//            public void visitCode() {
//                // Insert code to print "a" before method execution
////            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
////            mv.visitLdcInsn("native method start");
////            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
//                super.visitCode();
//
//            }
//
//            @Override
//            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
//                //将调用本地方法改为调用其他方法
//                if (name.equals("getVmArguments0")) {
//                    name = "newMethod";
//                }
//                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
//            }
//
//            @Override
//            public void visitInsn(int opcode) {
//
////            if (opcode == Opcodes.ARETURN) {
////                // Insert code to print "b" after method execution
////                mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
////                mv.visitLdcInsn("native method end");
////                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
////            }
//                super.visitInsn(opcode);
//            }
//
//            @Override
//            public void visitEnd() {
//                super.visitEnd();
//            }
//        }
//    }
//
//}
