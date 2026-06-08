package org.chen.transform.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chen.utils.Utils;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.List;

public class VMOptionsTransformer implements CommonClassFileTransformer{
    private static final Logger log = LogManager.getLogger(VMOptionsTransformer.class);
    private final List<String> hookClasses = List.of("com/intellij/diagnostic/VMOptions");
    @Override
    public boolean isHook() {
        return CommonClassFileTransformer.super.isHook();
    }

    @Override
    public List<String> getHookClasses() {
        return hookClasses;
    }

    @Override
    public byte[] transform(ClassLoader loader, String classInterName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if(hookClasses.contains(classInterName)){
            log.info("transform start {}", classInterName);
            CodeTransform codeTransform=new CodeTransform() {
                @Override
                public void accept(CodeBuilder builder, CodeElement element) {
                    builder.with(element);
                }

                @Override
                public void atEnd(CodeBuilder builder) {
//                    Label label = builder.newLabel();
//                    builder.dup().ifnull(label).invokestatic(ClassDesc.of(owner),"getUserOptionsFile",
//                                    MethodTypeDesc.of(ClassDesc.of(Path.class.getName()), ClassDesc.of(Path.class.getName())))
//                            .labelBinding(label);
                        builder.getstatic(ClassDesc.of(System.class.getName()),"out",ClassDesc.ofInternalName("java/io/PrintStream"))
                                .ldc("getUserOptionsFile called")
                                .invokevirtual(ClassDesc.ofInternalName("java/io/PrintStream"),"println",MethodTypeDesc.of(ConstantDescs.CD_void,ConstantDescs.CD_String));

                }
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
