package org.chen.transform.common;//package org.chen.transform.common;
//
//
//import jdk.internal.org.objectweb.asm.ClassReader;
//import jdk.internal.org.objectweb.asm.ClassWriter;
//import jdk.internal.org.objectweb.asm.Opcodes;
//import jdk.internal.org.objectweb.asm.tree.*;
//
//import java.lang.instrument.IllegalClassFormatException;
//import java.security.ProtectionDomain;
//import java.util.List;
//
//import static org.chen.utils.Utils.saveClass;
//import static jdk.internal.org.objectweb.asm.Opcodes.*;
//
///**
// * idea 加载 插件的class loader是PluginClassLoader ，我们修改这个类使得其无法加载到指定的class
// *
// */
//public class PluginClassLoaderTransformer implements CommonClassFileTransformer {
//
//    @Override
//    public List<String> getHookClasses() {
//        return List.of("com/intellij/ide/plugins/cl/PluginClassLoader");
//    }
//
//    @Override
//    public boolean isHook() {
//        return true;
//    }
//
//    @Override
//    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
//        if (!getHookClasses().contains(className)) {
//            return classfileBuffer;
//        }
//        log.info("transform start {}", className);
////        ClassReader reader = new ClassReader(classfileBuffer);
////        ClassNode node = new ClassNode(ASM8);
////        reader.accept(node, 0);
////        node.methods.forEach(methodNode -> {
////            if ("loadClass".equals(methodNode.name)) {
////                log.info("transform method start {} {}", className, methodNode.name);
////                InsnList insnList = new InsnList();
//////                insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
//////                insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
//////                insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));
////                insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
////                insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, "forName", "(Ljava/lang/String;)V", false));
////                methodNode.instructions.insert(insnList);
////                log.info("transform method end {} {}", className, methodNode.name);
////            }
////        });
////
////        //此处node.accept(writer) 如果失败 addTransformation 捕获不到异常 导致难以排查 所以需要try catch
////        //new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
////        //此处的ClassWriter.COMPUTE_FRAMES 要去掉 否则报错
////        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
////        try {
////            node.accept(writer);
////            saveClass(className.replaceAll("/", "."), writer);
////            log.info("transform end {}", className);
////        } catch (Exception e) {
////            log.error("transform error {} {}", e.getMessage());
////        }
//        return null;
//    }
//
//
//}
