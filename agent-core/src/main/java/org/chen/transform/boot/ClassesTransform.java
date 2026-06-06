package org.chen.transform.boot;


import org.chen.spy.ConfigHelperSpy;
import org.chen.utils.SimpleLog;
import org.chen.utils.Utils;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;


public class ClassesTransform implements BootClassFileTransformer {
    private static final String owner = ConfigHelperSpy.class.getName();
    private final List<String> hookClasses = new ArrayList<>();

    public ClassesTransform() {
        hookClasses.add("java.lang.Class");
    }

    @Override
    public boolean isHook() {
        return false;
    }

    @Override
    public List<String> getHookClasses() {
        return hookClasses;
    }

    @Override
    public byte[] transform(ClassLoader loader, String classInterName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) {
        var className = classInterName.replace("/", ".");
        if (!this.hookClasses.contains(className)) {
            return null;
        }
        SimpleLog.info("transform start {}", className);
        ClassModel classModel = ClassFile.of().parse(classFileBuffer);
        ClassTransform classTransform = getClassTransform();
        byte[] bytes = ClassFile.of().transformClass(classModel, classTransform);
        Utils.saveToFile(classInterName,bytes);
        SimpleLog.info("transform end {}", className);
        return bytes;
    }

    private static ClassTransform getClassTransform() {
        CodeTransform codeTransform=new CodeTransform() {
            @Override
            public void accept(CodeBuilder builder, CodeElement element) {
                builder.with(element);
            }
            @Override
            public void atStart(CodeBuilder builder) {
                builder.aload(0).invokestatic(ClassDesc.of(owner),"forName", MethodTypeDesc.of(ConstantDescs.CD_void,ConstantDescs.CD_String));
            }
        };
        MethodTransform methodTransform=MethodTransform.transformingCode(codeTransform);
        return ClassTransform.transformingMethods((methodModel)->
                methodModel.methodName().stringValue().equals("forName")&&methodModel.methodType().stringValue().equals("(Ljava/lang/String;)Ljava/lang/Class;"),methodTransform);
    }


}
