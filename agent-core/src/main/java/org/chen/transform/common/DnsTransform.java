package org.chen.transform.common;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chen.utils.Utils;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;


public class DnsTransform implements CommonClassFileTransformer {
    private static final Logger log = LogManager.getLogger(DnsTransform.class);
    private final List<String> hookClasses = new ArrayList<>();

    public DnsTransform() {
        log.info("DnsTransform");
        hookClasses.add(InetAddress.class.getName());
    }

    @Override
    public List<String> getHookClasses() {
        return hookClasses;
    }

    @Override
    public boolean isHook() {
        return true;
    }

    @Override
    public byte[] transform(ClassLoader loader, String classInterName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) {
        var className = classInterName.replace("/", ".");
        if (!this.hookClasses.contains(className)) {
            return null;
        }
        log.info("transform start {}", className);
        ClassModel parse = ClassFile.of().parse(classFileBuffer);
        ClassTransform classTransform=getClassTransform();
        try {
            byte[] bytes = ClassFile.of().transformClass(parse, classTransform);
            log.info("transform end {}", className);
            List<VerifyError> verify = ClassFile.of().verify(bytes);
            if (!verify.isEmpty()) {
                verify.forEach(Throwable::printStackTrace);
                return null;
            }
            Utils.saveToFile(classInterName, bytes);
        } catch (Exception e) {
            log.error("transform e:" , e);
        }
        return null;
    }
    public  ClassTransform getClassTransform() {
        MethodTransform getAllbyNamemethodTransform = MethodTransform.transformingCode(new CodeTransform() {
            @Override
            public void accept(CodeBuilder builder, CodeElement element) {
                builder.accept(element);
            }
            @Override
            public void atStart(CodeBuilder builder) {
                builder.aload(0).invokestatic(ClassDesc.of(owner),"getAllByName",
                        MethodTypeDesc.of(ConstantDescs.CD_void,ConstantDescs.CD_String));
            }
        });

        MethodTransform isReachable=MethodTransform.transformingCode(new CodeTransform() {
            @Override
            public void accept(CodeBuilder builder, CodeElement element) {
                builder.accept(element);
            }
            @Override
            public void atStart(CodeBuilder builder) {
                builder.aload(0)
                        .invokestatic(ClassDesc.of(owner),"isReachable",
                                MethodTypeDesc.of(ConstantDescs.CD_Boolean, ClassDesc.of(InetAddress.class.getName())));
                Label label = builder.newLabel();
                builder.ifnull(label);
                builder.iconst_0();
                builder.ireturn();
                builder.labelBinding(label);
            }
        });

        ClassTransform classTransform = (builder,element) -> {
            switch (element ){
                case MethodModel methodModel when methodModel.methodName().equalsString("getAllByName")
                        && methodModel.methodTypeSymbol().equals(
                        MethodTypeDesc.of(ClassDesc.of(InetAddress.class.getName()).arrayType(), ConstantDescs.CD_String)) ->
                        builder.transformMethod(methodModel,getAllbyNamemethodTransform);
                case MethodModel methodModel when methodModel.methodName().equalsString("isReachable")
                        && methodModel.methodTypeSymbol().equals(MethodTypeDesc.of(ConstantDescs.CD_boolean, ClassDesc.of(NetworkInterface.class.getName()),
                        ConstantDescs.CD_int, ConstantDescs.CD_int)) -> builder.transformMethod(methodModel,isReachable);
                default -> builder.with(element);
            }
        };
        return classTransform;
    }
}
