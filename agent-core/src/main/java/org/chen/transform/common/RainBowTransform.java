//package org.chen.transform.common;
//
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.chen.utils.Utils;
//
//import java.lang.classfile.*;
//import java.lang.classfile.attribute.CodeAttribute;
//import java.lang.classfile.constantpool.Utf8Entry;
//import java.security.ProtectionDomain;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//
//public class RainBowTransform implements CommonClassFileTransformer {
//    private static final Logger log = LogManager.getLogger(RainBowTransform.class);
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
//        return CommonClassFileTransformer.super.isHook();
//    }
//
//    @Override
//    public byte[] transform(ClassLoader loader, String classInterName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) {
//        var className = classInterName.replace("/", ".");
//        if (!this.hookClasses.contains(className)) {
//            return classFileBuffer;
//        }
//
//        log.info("transform start {} {}", className, loader);
//        try {
//            // 初始化ClassFile工具，自动处理栈大小、栈帧计算（等价原ClassWriter.COMPUTE_FRAMES|COMPUTE_MAXS）
//            ClassFile classFile = ClassFile.of();
//            // 解析原始class字节码
//            ClassModel classModel = classFile.parse(classFileBuffer);
//
//            // 保存我们修改后的方法
//            List<MethodModel> modifiedMethods = new ArrayList<>();
//            boolean hasModified = false;
//
//            // ==============================================
//            // 分类型处理不同的目标类，完全复刻原switch逻辑
//            // ==============================================
//            switch (classInterName) {
//                case "com/intellij/diagnostic/VMOptions" -> {
//                    // 处理VMOptions类，修改getUserOptionsFile方法
//                    for (MethodModel oldMethod : classModel.methods()) {
//                        if (oldMethod.methodName().equalsString("getUserOptionsFile")) {
//                            log.info("transform method start {} {}", className, oldMethod.methodName());
//                            Optional<CodeModel> oldCode = oldMethod.code();
//                            if (oldCode.isEmpty()) {
//                                break;
//                            }
//                            CodeTransform codeTransform=new CodeTransform() {
//                                @Override
//                                public void accept(CodeBuilder builder, CodeElement element) {
//                                        builder.with(element);
//                                }
//                            };
//
//                            CodeModel codeModel = oldCode.get();
//                            ClassTransform classTransform = ClassTransform.transformingMethods(methodModel -> false,(builder, element) -> {
//
//                            });
//
//                            // 遍历所有指令，等价原InsnList遍历
//                            for (Instruction instr : oldCode) {
//                                if (instr.opcode() == OpCode.ARETURN) {
//                                    // 找到返回指令，插入拦截代码（完全复刻原ASM逻辑）
//                                    Label nullLabel = new Label();
//                                    newCode.dup(); // DUP
//                                    newCode.ifnull(nullLabel); // IFNULL 跳转
//                                    // 调用我们的hook方法
//                                    newCode.invokestatic(
//                                            classModel.constantPool().methodRef(
//                                                    classModel.constantPool().classSymbol(owner),
//                                                    classModel.constantPool().nameAndType(
//                                                            "getUserOptionsFile",
//                                                            "(Ljava/nio/file/Path;)Ljava/nio/file/Path;"
//                                                    )
//                                            )
//                                    );
//                                    newCode.labelBinding(nullLabel);
//                                }
//                                // 原返回指令原样保留
//                                newCode.with(instr);
//                            }
//
//                            // 构建修改后的方法
//                            MethodInfo newMethod = MethodInfo.of(
//                                    oldMethod.flags(),
//                                    oldMethod.methodName().orElseThrow(),
//                                    oldMethod.methodType().descriptorString(),
//                                    oldMethod.attributes().stream().filter(a -> !(a instanceof CodeAttribute)).toList(),
//                                    newCode.build()
//                            );
//                            modifiedMethods.add(newMethod);
//                            hasModified = true;
//                            log.info("transform method end {} {}", className, oldMethod.methodName().orElse(null));
//                        }
//                    }
//                }
//
////                case "com/intellij/ui/LicensingFacade" -> {
////                    // 处理LicensingFacade类
////                    for (MethodInfo oldMethod : classModel.methods()) {
////                        String methodName = oldMethod.methodName().orElse(null);
////                        String methodDesc = oldMethod.methodType().descriptorString();
////
////                        // 1. 修改getLicenseExpirationDate方法，清空原指令替换为hook
////                        if ("getLicenseExpirationDate".equals(methodName)) {
////                            log.info("transform method start {} {}", className, methodName);
////                            CodeBuilder newCode = CodeBuilder.of(classModel.constantPool(), 1, 0);
////                            // 调用我们的hook方法
////                            newCode.invokestatic(
////                                    classModel.constantPool().methodRef(
////                                            classModel.constantPool().classSymbol(owner),
////                                            classModel.constantPool().nameAndType("getLicenseExpirationDate", "()Ljava/util/Date;")
////                                    )
////                            );
////                            newCode.areturn();
////
////                            MethodInfo newMethod = MethodInfo.of(
////                                    oldMethod.flags(),
////                                    methodName,
////                                    methodDesc,
////                                    oldMethod.attributes().stream().filter(a -> !(a instanceof CodeAttribute)).toList(),
////                                    newCode.build()
////                            );
////                            modifiedMethods.add(newMethod);
////                            hasModified = true;
////                            log.info("transform method end {} {}", className, methodName);
////                        }
////
////                        // 2. 修改getExpirationDate(String)方法
////                        else if ("getExpirationDate".equals(methodName) && "(Ljava/lang/String;)Ljava/util/Date;".equals(methodDesc)) {
////                            log.info("transform method start {} {}", className, methodName);
////                            CodeBuilder newCode = CodeBuilder.of(classModel.constantPool(), 2, 2);
////                            // 完全复刻原ASM的指令顺序
////                            newCode.aload(0); // ALOAD 0: this
////                            newCode.getfield( // GETFIELD expirationDates
////                                    classModel.constantPool().fieldRef(
////                                            classModel.constantPool().classSymbol(classInterName),
////                                            classModel.constantPool().nameAndType("expirationDates", "Ljava/util/Map;")
////                                    )
////                            );
////                            newCode.aload(1); // ALOAD 1: 方法参数
////                            // 调用我们的hook方法
////                            newCode.invokestatic(
////                                    classModel.constantPool().methodRef(
////                                            classModel.constantPool().classSymbol(owner),
////                                            classModel.constantPool().nameAndType("getExpirationDate", "(Ljava/util/Map;Ljava/lang/String;)Ljava/util/Date;")
////                                    )
////                            );
////                            newCode.areturn();
////
////                            MethodInfo newMethod = MethodInfo.of(
////                                    oldMethod.flags(),
////                                    methodName,
////                                    methodDesc,
////                                    oldMethod.attributes().stream().filter(a -> !(a instanceof CodeAttribute)).toList(),
////                                    newCode.build()
////                            );
////                            modifiedMethods.add(newMethod);
////                            hasModified = true;
////                            log.info("transform method end {} {}", className, methodName);
////                        }
////
////                        // 3. 所有非静态、非构造方法，开头插入expirationDates更新逻辑
////                        else {
////                            boolean isNonStatic = (oldMethod.flags().toModifierFlags() & Opcodes.ACC_STATIC) == 0;
////                            boolean isConstructor = "<init>".equals(methodName);
////                            if (isNonStatic && !isConstructor) {
////                                log.info("transform method start {} {}", className, methodName);
////                                CodeAttribute oldCode = oldMethod.code().orElseThrow();
////                                CodeBuilder newCode = CodeBuilder.of(
////                                        classModel.constantPool(),
////                                        oldCode.maxStack(),
////                                        oldCode.maxLocals()
////                                );
////
////                                // 方法最开头插入更新代码（等价原instructions.insert(insnList)）
////                                newCode.aload(0); // ALOAD 0: this
////                                newCode.dup(); // DUP
////                                newCode.getfield( // GETFIELD expirationDates
////                                        classModel.constantPool().fieldRef(
////                                                classModel.constantPool().classSymbol(classInterName),
////                                                classModel.constantPool().nameAndType("expirationDates", "Ljava/util/Map;")
////                                        )
////                                );
////                                // 调用我们的hook方法
////                                newCode.invokestatic(
////                                        classModel.constantPool().methodRef(
////                                                classModel.constantPool().classSymbol(owner),
////                                                classModel.constantPool().nameAndType("expirationDates", "(Ljava/util/Map;)Ljava/util/Map;")
////                                        )
////                                );
////                                newCode.putfield( // PUTFIELD 写回字段
////                                        classModel.constantPool().fieldRef(
////                                                classModel.constantPool().classSymbol(classInterName),
////                                                classModel.constantPool().nameAndType("expirationDates", "Ljava/util/Map;")
////                                        )
////                                );
////
////                                // 原方法的所有指令原样保留
////                                for (Instruction instr : oldCode) {
////                                    newCode.with(instr);
////                                }
////
////                                MethodInfo newMethod = MethodInfo.of(
////                                        oldMethod.flags(),
////                                        methodName,
////                                        methodDesc,
////                                        oldMethod.attributes().stream().filter(a -> !(a instanceof CodeAttribute)).toList(),
////                                        newCode.build()
////                                );
////                                modifiedMethods.add(newMethod);
////                                hasModified = true;
////                                log.info("transform method end {} {}", className, methodName);
////                            }
////                        }
////                    }
////                }
////
////                case "sun/management/VMManagementImpl" -> {
////                    // 处理VMManagementImpl类
////                    for (MethodModel oldMethod : classModel.methods()) {
////                        Utf8Entry methodName = oldMethod.methodName();
////                        Utf8Entry methodDesc = oldMethod.methodType();
////                        if (methodName.equalsString("getVmArguments") && methodDesc.equalsString("()Ljava/util/List;")) {
////                            log.info("transform method start {} {}", className, methodName);
////                            CodeAttribute oldCode = oldMethod.;
////                            CodeBuilder newCode = CodeBuilder.of(
////                                    classModel.constantPool(),
////                                    oldCode.maxStack(),
////                                    oldCode.maxLocals()
////                            );
////
////                            int breakFlag = 0; // 完全复刻原breakFlag逻辑，找到2个目标就停止处理
////                            for (Instruction instr : oldCode) {
////                                if (breakFlag >= 2) {
////                                    // 超过目标数量，后面的指令原样保留
////                                    newCode.with(instr);
////                                    continue;
////                                }
////
////                                // 1. 处理ARETURN，插入vmArgs更新逻辑
////                                if (instr.opcode() == OpCode.ARETURN) {
////                                    // 完全复刻原ASM的插入代码
////                                    newCode.aload(0); // ALOAD 0: this
////                                    newCode.aload(0); // ALOAD 0: this
////                                    newCode.getfield( // GETFIELD vmArgs
////                                            classModel.constantPool().fieldRef(
////                                                    classModel.constantPool().classSymbol("sun/management/VMManagementImpl"),
////                                                    classModel.constantPool().nameAndType("vmArgs", "Ljava/util/List;")
////                                            )
////                                    );
////                                    // 调用ConfigHelper的hook
////                                    newCode.invokestatic(
////                                            classModel.constantPool().methodRef(
////                                                    classModel.constantPool().classSymbol("com/chen/utils/ConfigHelper"),
////                                                    classModel.constantPool().nameAndType("getVmArguments", "(Ljava/util/List;)Ljava/util/List;")
////                                            )
////                                    );
////                                    newCode.putfield( // PUTFIELD 写回vmArgs
////                                            classModel.constantPool().fieldRef(
////                                                    classModel.constantPool().classSymbol("sun/management/VMManagementImpl"),
////                                                    classModel.constantPool().nameAndType("vmArgs", "Ljava/util/List;")
////                                            )
////                                    );
////                                    breakFlag++;
////                                }
////
////                                // 2. 处理方法调用，拦截getVmArguments0
////                                if (instr instanceof InvokeInstruction invokeInstr) {
////                                    // 打印原方法调用日志，完全复刻
////                                    log.info("methodInsnNode:{} {} {}", invokeInstr.owner(), invokeInstr.name(), invokeInstr.type().descriptorString());
////
////                                    // 找到目标调用getVmArguments0
////                                    if ("getVmArguments0".equals(invokeInstr.name())
////                                            && "sun/management/VMManagementImpl".equals(invokeInstr.owner())
////                                            && "()[Ljava/lang/String;".equals(invokeInstr.type().descriptorString())
////                                            && invokeInstr.opcode() == OpCode.INVOKEVIRTUAL) {
////
////                                        log.info("found methodInsnNode:{} {} {}", invokeInstr.owner(), invokeInstr.name(), invokeInstr.type().descriptorString());
////                                        // 插入Entering打印
////                                        newCode.getstatic(
////                                                classModel.constantPool().fieldRef(
////                                                        classModel.constantPool().classSymbol("java/lang/System"),
////                                                        classModel.constantPool().nameAndType("out", "Ljava/io/PrintStream;")
////                                                )
////                                        );
////                                        newCode.ldc("Entering native method: getVmArguments0");
////                                        newCode.invokevirtual(
////                                                classModel.constantPool().methodRef(
////                                                        classModel.constantPool().classSymbol("java/io/PrintStream"),
////                                                        classModel.constantPool().nameAndType("println", "(Ljava/lang/String;)V")
////                                                )
////                                        );
////
////                                        // 原调用指令
////                                        newCode.with(instr);
////
////                                        // 插入Exiting打印
////                                        newCode.getstatic(
////                                                classModel.constantPool().fieldRef(
////                                                        classModel.constantPool().classSymbol("java/lang/System"),
////                                                        classModel.constantPool().nameAndType("out", "Ljava/io/PrintStream;")
////                                                )
////                                        );
////                                        newCode.ldc("Exiting native method: getVmArguments0");
////                                        newCode.invokevirtual(
////                                                classModel.constantPool().methodRef(
////                                                        classModel.constantPool().classSymbol("java/io/PrintStream"),
////                                                        classModel.constantPool().nameAndType("println", "(Ljava/lang/String;)V")
////                                                )
////                                        );
////
////                                        breakFlag++;
////                                    } else {
////                                        newCode.with(instr);
////                                    }
////                                } else {
////                                    newCode.with(instr);
////                                }
////                            }
////
////                            // 构建修改后的方法
////                            MethodInfo newMethod = MethodInfo.of(
////                                    oldMethod.flags(),
////                                    methodName,
////                                    methodDesc,
////                                    oldMethod.attributes().stream().filter(a -> !(a instanceof CodeAttribute)).toList(),
////                                    newCode.build()
////                            );
////                            modifiedMethods.add(newMethod);
////                            hasModified = true;
////                            log.info("transform method end {} {}", className, methodName);
////                        }
////                    }
////                }
//            }
//
//            // 没有任何修改，直接返回原字节码
//            if (!hasModified) {
//                return classFileBuffer;
//            }
//
//            // ==============================================
//            // 生成最终的修改后的class字节码
//            // ==============================================
////            byte[] newClassBytes = classFile.transformClass(classModel, clb -> {
////                // 替换我们修改过的方法，其他所有元素完全保留
////                clb.with(element -> {
////                    if (element instanceof MethodModel m) {
////                        // 找到对应的修改后的方法
////                        Optional<MethodModel> newM = modifiedMethods.stream()
////                                .filter(nm -> nm.methodName().equals(m.methodName()) && nm.methodType().equals(m.methodType()))
////                                .findFirst();
////                        return newM.orElse(m);
////                    }
////                    return element;
////                });
////            });
//
//            byte[] newClassBytes=classFileBuffer;
//            Utils.saveToFile(classInterName, newClassBytes);
//            log.info("transform end {}", className);
//            return newClassBytes;
//        } catch (Exception e) {
//            // 出错时返回null 不修改
//            log.error("transform failed for class {}", className, e);
//            return null;
//        }
//    }
//}
