package org.chen.transform.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chen.utils.Utils;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.AccessFlag;
import java.security.ProtectionDomain;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

import static java.lang.constant.ConstantDescs.*;

public class MybatisCodeHelperProTransformer implements CommonClassFileTransformer {
    private static final Logger log = LogManager.getLogger(MybatisCodeHelperProTransformer.class);
    private final List<String> hookClasses = new ArrayList<>();

    @Override
    public boolean isHook() {
        return CommonClassFileTransformer.super.isHook();
    }

    @Override
    public List<String> getHookClasses() {
        hookClasses.add("com/ccnode/codegenerator/ag/c/c");
        hookClasses.add("com/ccnode/codegenerator/ag/f/d");
        return hookClasses;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!this.hookClasses.contains(className)) {
            return null;
        }
        ClassTransform classTransform = null;
        switch (className) {
            case "com/ccnode/codegenerator/ag/c/c"->{
                MethodTransform methodTransform=MethodTransform.transformingCode(new CodeTransform() {
                    @Override
                    public void accept(CodeBuilder builder, CodeElement element) {
                        builder.with(element);
                    }

                    @Override
                    public void atStart(CodeBuilder builder) {
                        builder.aload(0)
                                .aload(1)
                                .invokestatic(ClassDesc.of(owner), "getOfflineActivationCode",
                                        MethodTypeDesc.of(CD_String, CD_String))
                                .astore(1);
                    }
                });
                classTransform= ClassTransform.transformingMethods((methodModel)-> methodModel.methodName().equalsString("a")&&methodModel.methodTypeSymbol().equals(
                        MethodTypeDesc.of(CD_void,CD_String)
                ),methodTransform);
            }
            case "com/ccnode/codegenerator/ag/f/d"->{
                MethodTransform methodTransform = MethodTransform.transformingCode(new CodeTransform() {
                    @Override
                    public void accept(CodeBuilder builder, CodeElement element) {

                    }

                    @Override
                    public void atStart(CodeBuilder builder) {
                        builder.aload(1).areturn();
                    }
                });

                classTransform = ClassTransform.transformingMethods((methodModel) -> {
                            if (methodModel.flags().has(AccessFlag.STATIC) && methodModel.methodName().equalsString("b")) {
                                if(methodModel.methodTypeSymbol()
                                        .equals(MethodTypeDesc.of(CD_byte.arrayType(), ClassDesc.of(RSAPublicKey.class.getName()), CD_byte.arrayType()))){
                                    log.info("find");
                                    return true;
                                }
                            }
                            return false;
                        }
                        , methodTransform);
            }
        }


        byte[] bytes;
        try {
            bytes = ClassFile.of().transformClass(ClassFile.of(ClassFile.StackMapsOption.GENERATE_STACK_MAPS, ClassFile.StackMapsOption.STACK_MAPS_WHEN_REQUIRED).parse(classfileBuffer), classTransform);
        } catch (Exception e) {
            log.info("transform exception {}", className, e);
            return null;
        }
        List<VerifyError> verify = ClassFile.of().verify(bytes);
        if (!verify.isEmpty()) {
            verify.forEach(Throwable::printStackTrace);
            return null;
        }
        Utils.saveToFile(className, bytes);
        log.info("transform end {}", className);
        return bytes;
    }
}
