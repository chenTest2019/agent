package org.chen.transform.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chen.utils.Utils;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.List;

import static java.lang.constant.ConstantDescs.CD_String;
import static java.lang.constant.ConstantDescs.CD_void;

public class VMOptionsTransformer implements CommonClassFileTransformer{
    private static final Logger log = LogManager.getLogger(VMOptionsTransformer.class);
    private final List<String> hookClasses = List.of("com/intellij/diagnostic/VMOptions");
    @Override
    public boolean isHook() {
        return false;
    }

    @Override
    public List<String> getHookClasses() {
        return hookClasses;
    }


    /**
     *
     * @param loader                the defining loader of the class to be transformed,
     *                              may be {@code null} if the bootstrap loader
     * @param classInterName             the name of the class in the internal form of fully
     *                              qualified class and interface names as defined in
     *                              <i>The Java Virtual Machine Specification</i>.
     *                              For example, <code>"java/util/List"</code>.
     * @param classBeingRedefined   if this is triggered by a redefine or retransform,
     *                              the class being redefined or retransformed;
     *                              if this is a class load, {@code null}
     * @param protectionDomain      the protection domain of the class being defined or redefined
     * @param classfileBuffer       the input byte buffer in class file format - must not be modified
     * @return a well-formed class file buffer (the result of the transform), or {@code null} if no transform is performed.
     * @throws IllegalClassFormatException if the input does not represent a well-formed class
     */
    @Override
    public byte[] transform(ClassLoader loader, String classInterName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        /**
         *
         * case "com/intellij/diagnostic/VMOptions" -> {
         *                 classNode.methods.forEach(methodNode -> {
         *                     if (methodNode.name.equals("getUserOptionsFile")) {
         *                         Log.info("transform method start {} {}", className, methodNode.name);
         *                         for (AbstractInsnNode node : methodNode.instructions) {
         *                             if (node.getOpcode() == Opcodes.ARETURN) {
         *                                 var nullLabel = new LabelNode();
         *                                 InsnList insnList = new InsnList();
         *                                 insnList.add(new InsnNode(Opcodes.DUP));
         *                                 insnList.add(new JumpInsnNode(Opcodes.IFNULL, nullLabel));
         *                                 var descriptorString = Path.class.descriptorString();
         *                                 descriptorString = "(" + descriptorString + ")" + descriptorString;
         *                                 insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, "getUserOptionsFile", descriptorString));
         *                                 insnList.add(nullLabel);
         *                                 methodNode.instructions.insert(node.getPrevious(), insnList);
         *                             }
         *                         }
         *                         Log.info("transform method end {} {}", className, methodNode.name);
         *                     }
         *                 });
         *             }
         *
         */

        if(hookClasses.contains(classInterName)){
            log.info("transform start {}", classInterName);
            CodeTransform codeTransform= (builder, element) -> {
                // Insert interception before any ARETURN instruction: duplicate return value,
                // if it's null skip, otherwise call ConfigHelperSpy.getUserOptionsFile(Path) to
                // allow the spy to modify/replace the returned Path.
                if (element instanceof Instruction instr && Opcode.ARETURN==instr.opcode()) {
                    Label label = builder.newLabel();
                    // duplicate the top-of-stack reference (the value about to be returned)
                    builder.dup()
                            // if duplicated value is null jump to label (skip spy call)
                            .ifnull(label)
                            // call spy: Path getUserOptionsFile(Path)
                            .invokestatic(ClassDesc.of(owner), "getUserOptionsFile",
                                    MethodTypeDesc.of(ClassDesc.of(Path.class.getName()), ClassDesc.of(Path.class.getName())));
                    // bind label so original ARETURN still returns original value when null
                    builder.labelBinding(label);
                }
                builder.accept(element);
            };

            MethodTransform methodTransform=MethodTransform.transformingCode(codeTransform);
            ClassTransform classTransform=ClassTransform.transformingMethods(
                    methodModel ->{
                        //                            System.out.println(methodModel.methodTypeSymbol());
                        //                            System.out.println(MethodTypeDesc.of(ClassDesc.of(Path.class.getName())));
                        //                            System.out.println();
                        return methodModel.methodName().equalsString("getUserOptionsFile")
                                && methodModel.methodTypeSymbol().equals(MethodTypeDesc.of(ClassDesc.of(Path.class.getName())));
                    }
                    ,methodTransform);

            byte[] bytes;
            try {
                bytes = ClassFile.of().transformClass(ClassFile.of().parse(classfileBuffer), classTransform);
            } catch (Exception e) {
                log.info("transform exception {}", classInterName,e);
                return null;
            }
            List<VerifyError> verify = ClassFile.of().verify(bytes);
            if (!verify.isEmpty()) {
                verify.forEach(Throwable::printStackTrace);
                return null;
            }
            Utils.saveToFile(classInterName, bytes);
            log.info("transform end {}", classInterName);
            return bytes;
        }
        return null;
    }
}
