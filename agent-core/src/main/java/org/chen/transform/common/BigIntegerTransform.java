package org.chen.transform.common;

import org.chen.utils.SimpleLog;
import org.chen.utils.Utils;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.math.BigInteger;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class BigIntegerTransform implements CommonClassFileTransformer {

    private final List<String> hookClasses = new ArrayList<>();

    public BigIntegerTransform() {
        SimpleLog.info("this:" + this);
        //owner = ConfigHelperSpy.class.getName();
        //必须再transform前已经确定好
        hookClasses.add(BigInteger.class.getName());
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

//        if (classInterName.contains("BigInteger")) {
//            SimpleLog.info("transform BigInteger:{}", classInterName);
//        }

        var className = classInterName.replace("/", ".");
        if (!this.hookClasses.contains(className)) {
            return null;
        }
        try {
            SimpleLog.info("transform start {}", className);
            ClassModel parse = ClassFile.of().parse(classFileBuffer);
            ClassTransform classTransform = getClassTransform();
            SimpleLog.info("transform end {}", className);
            byte[] bytes = ClassFile.of().transformClass(parse, classTransform);
            List<VerifyError> verify = ClassFile.of().verify(bytes);
            if (!verify.isEmpty()) {
                verify.forEach(Throwable::printStackTrace);
                return null;
            }
            Utils.saveToFile(classInterName,bytes);
            return bytes;
        } catch (Exception e) {
            SimpleLog.error("transform e:" + e);
        }
        return null;
    }

    private ClassTransform getClassTransform() {
        MethodTransform methodTransform=MethodTransform.transformingCode(new MyCodeTransform());
        return ClassTransform.transformingMethods(
                methodModel -> methodModel.methodName().equalsString("oddModPow")
                        &&methodModel.methodTypeSymbol().equals(MethodTypeDesc.of(ClassDesc.of(BigInteger.class.getName()),
                        ClassDesc.of(BigInteger.class.getName()),
                        ClassDesc.of(BigInteger.class.getName())))
                ,methodTransform);
    }

    private  class MyCodeTransform implements CodeTransform {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        @Override
        public void accept(CodeBuilder builder, CodeElement element) {
            if (atomicBoolean.compareAndSet(false, true)) {
                Label label = builder.newLabel();
                builder.aload(0).aload(1).aload(2).invokestatic(ClassDesc.of(owner),
                                "oddModPow", MethodTypeDesc.of(ClassDesc.of(BigInteger.class.getName()),
                                        ClassDesc.of(BigInteger.class.getName()),
                                        ClassDesc.of(BigInteger.class.getName()),
                                        ClassDesc.of(BigInteger.class.getName())),false)
                        .dup().ifnull(label)
                        .areturn()
                        .labelBinding(label)
                ;
            }
            builder.accept(element);
        }

        @Override
        public void atEnd(CodeBuilder builder) {
            CodeTransform.super.atEnd(builder);
        }

        @Override
        public void atStart(CodeBuilder builder) {
            CodeTransform.super.atStart(builder);
        }

        @Override
        public CodeTransform andThen(CodeTransform t) {
            return CodeTransform.super.andThen(t);
        }
    }

}
