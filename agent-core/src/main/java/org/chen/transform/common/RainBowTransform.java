package org.chen.transform.common;//package org.chen.transform.common;
//
//
//import org.chen.utils.ConfigHelper;
//import org.chen.utils.SimpleLog;
//import jdk.internal.org.objectweb.asm.ClassReader;
//import jdk.internal.org.objectweb.asm.ClassWriter;
//import jdk.internal.org.objectweb.asm.Opcodes;
//import jdk.internal.org.objectweb.asm.tree.*;
//
//import java.nio.file.Path;
//import java.security.ProtectionDomain;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//
//import static org.chen.utils.Utils.saveClass;
//import static jdk.internal.org.objectweb.asm.Opcodes.ASM8;
//
//public class RainBowTransform implements CommonClassFileTransformer {
//    private static final String owner = ConfigHelper.class.getName().replace(".", "/");
//    private final List<String> hookClasses = new ArrayList<>();
//
//    public RainBowTransform() {
//        hookClasses.add("com.intellij.diagnostic.VMOptions");
//        hookClasses.add("com.intellij.ui.LicensingFacade");
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
//        return true;
//    }
//
//    @Override
//    public byte[] transform(ClassLoader loader, String classInterName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) {
//        var className = classInterName.replaceAll("/", ".");
//        if (!this.hookClasses.contains(className)) {
//            return classFileBuffer;
//        }
//
//        SimpleLog.info("transform start {} {}", className, loader);
//        ClassReader classReader = new ClassReader(classFileBuffer);
//        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
//        ClassNode classNode = new ClassNode(ASM8);
//        classReader.accept(classNode, 0);
//        switch (classInterName) {
//            case "com/intellij/diagnostic/VMOptions" -> {
//                classNode.methods.forEach(methodNode -> {
//                    if (methodNode.name.equals("getUserOptionsFile")) {
//                        SimpleLog.info("transform method start {} {}", className, methodNode.name);
//                        for (AbstractInsnNode node : methodNode.instructions) {
//                            if (node.getOpcode() == Opcodes.ARETURN) {
//                                var nullLabel = new LabelNode();
//                                InsnList insnList = new InsnList();
//                                insnList.add(new InsnNode(Opcodes.DUP));
//                                insnList.add(new JumpInsnNode(Opcodes.IFNULL, nullLabel));
//                                var descriptorString = Path.class.descriptorString();
//                                descriptorString = "(" + descriptorString + ")" + descriptorString;
//                                insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, "getUserOptionsFile", descriptorString));
//                                insnList.add(nullLabel);
//                                methodNode.instructions.insert(node.getPrevious(), insnList);
//                            }
//                        }
//                        SimpleLog.info("transform method end {} {}", className, methodNode.name);
//                    }
//                });
//            }
//            case "com/intellij/ui/LicensingFacade" -> {
//                classNode.methods.forEach(methodNode -> {
//                    if (methodNode.name.equals("getLicenseExpirationDate")) {
//                        SimpleLog.info("transform method start {} {}", className, methodNode.name);
//                        methodNode.instructions.clear();
//                        methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, methodNode.name, "()Ljava/util/Date;"));
//                        methodNode.instructions.add(new InsnNode(Opcodes.ARETURN));
//                        SimpleLog.info("transform method end {} {}", className, methodNode.name);
//                    }
//
//                    if (methodNode.name.equals("getExpirationDate") && methodNode.desc.equals("(Ljava/lang/String;)Ljava/util/Date;")) {
//                        SimpleLog.info("transform method start {} {}", className, methodNode.name);
//                        methodNode.instructions.clear();
//                        methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
//                        methodNode.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classInterName, "expirationDates", "Ljava/util/Map;"));
//                        methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
//                        methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, methodNode.name, "(Ljava/util/Map;Ljava/lang/String;)Ljava/util/Date;"));
//                        methodNode.instructions.add(new InsnNode(Opcodes.ARETURN));
//                        SimpleLog.info("transform method end {} {}", className, methodNode.name);
//                    }
//
//                    //由于不知道expirationDates什么时候被修改 所以这里在每个对象方法前更新
//                    //非静态方法
//                    boolean isNonStatic = ((methodNode.access & Opcodes.ACC_STATIC) == 0);
//                    boolean isConstructor = methodNode.name.equals("<init>");
//                    if (isNonStatic && !isConstructor) {
//                        SimpleLog.info("transform method start {} {}", className, methodNode.name);
//                        InsnList insnList = new InsnList();
//                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
//                        insnList.add(new InsnNode(Opcodes.DUP));
//                        insnList.add(new FieldInsnNode(Opcodes.GETFIELD, classInterName, "expirationDates", "Ljava/util/Map;"));
//                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, "expirationDates", "(Ljava/util/Map;)Ljava/util/Map;"));
//                        insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, classInterName, "expirationDates", "Ljava/util/Map;"));
//                        methodNode.instructions.insert(insnList);
//                        SimpleLog.info("transform method end {} {}", className, methodNode.name);
//                    }
//                });
//            }
//            case "sun/management/VMManagementImpl" -> {
//                classNode.methods.forEach(methodNode -> {
//                    var iterator = methodNode.instructions.iterator();
//                    if (methodNode.name.equals("getVmArguments") &&
//                            "()Ljava/util/List;".equals(methodNode.desc)) {
//                        SimpleLog.info("transform method start {} {}", className, methodNode.name);
//                        InsnList insnList = new InsnList();
//                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
//                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
//                        insnList.add(new FieldInsnNode(Opcodes.GETFIELD, "sun/management/VMManagementImpl", "vmArgs", "Ljava/util/List;"));
//                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/chen/utils/ConfigHelper", "getVmArguments", "(Ljava/util/List;)Ljava/util/List;", false));
//                        insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, "sun/management/VMManagementImpl", "vmArgs", "Ljava/util/List;"));
//                        int breakFlag = 0;
//                        while (iterator.hasNext()) {
//
//                            AbstractInsnNode insnNode = iterator.next();
//                            if (insnNode.getType() == AbstractInsnNode.INSN && insnNode.getOpcode() == Opcodes.ARETURN) {
//                                methodNode.instructions.insert(insnNode.getPrevious().getPrevious(), insnList);
//                                breakFlag = breakFlag + 1;
//                                if (breakFlag == 2) {
//                                    break;
//                                }
//                            }
//
//                            if (insnNode instanceof MethodInsnNode methodInsnNode) {
//                                SimpleLog.info("methodInsnNode:" + methodInsnNode.owner + "\t" + methodInsnNode.name + "\t" + methodInsnNode.desc);
//                                boolean canAdd = (Objects.equals(methodInsnNode.owner, "sun/management/VMManagementImpl") &&
//                                        Objects.equals(methodInsnNode.name, "getVmArguments0") &&
//                                        Objects.equals(methodInsnNode.desc, "()[Ljava/lang/String;") &&
//                                        Opcodes.INVOKEVIRTUAL == insnNode.getOpcode());
//                                if (canAdd) {
//                                    SimpleLog.info("found methodInsnNode:" + methodInsnNode.owner + "\t" + methodInsnNode.name + "\t" + methodInsnNode.desc);
//                                    // 在方法开始处插入打印消息的指令
//                                    InsnList startInstructions = new InsnList();
//                                    startInstructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
//                                    startInstructions.add(new LdcInsnNode("Entering native method: " + methodInsnNode.name));
//                                    startInstructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"));
//                                    methodNode.instructions.insert(insnNode.getPrevious(), startInstructions);
//
//                                    // 在方法结束处插入打印消息的指令
//                                    InsnList endInstructions = new InsnList();
//                                    endInstructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
//                                    endInstructions.add(new LdcInsnNode("Exiting native method: " + methodInsnNode.name));
//                                    endInstructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"));
//                                    methodNode.instructions.insert(insnNode, endInstructions);
//                                    breakFlag = breakFlag + 1;
//                                    if (breakFlag == 2) {
//                                        break;
//                                    }
//                                }
//                            }
//                        }
//                        SimpleLog.info("transform method end {} {}", className, methodNode.name);
//                    }
//                });
//            }
//        }
//        SimpleLog.info("transform end {}", className);
//        classNode.accept(classWriter);
//        saveClass(className, classWriter);
//        return classWriter.toByteArray();
//    }
//}
