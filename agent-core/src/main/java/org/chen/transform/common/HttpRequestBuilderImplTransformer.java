package org.chen.transform.common;

import org.chen.utils.SimpleLog;
import org.chen.utils.Utils;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URI;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

//import jdk.internal.net.http.*;
public class HttpRequestBuilderImplTransformer implements CommonClassFileTransformer{
    private final List<String> hookClasses = new ArrayList<>();

    public HttpRequestBuilderImplTransformer() {
        hookClasses.add("jdk.internal.net.http.HttpRequestBuilderImpl");
    }

    @Override
    public boolean isHook() {
        return true;
    }

    @Override
    public List<String> getHookClasses() {
        return hookClasses;
    }

    @Override
    public byte[] transform(ClassLoader loader, String classInterName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        var className = classInterName.replace("/", ".");
        if (!this.hookClasses.contains(className)) {
            return classfileBuffer;
        }
        SimpleLog.info("transform start {}", className);
        ClassTransform classTransform = getClassTransform();
        byte[] bytes = null;
        try {
            bytes = ClassFile.of().transformClass(ClassFile.of().parse(classfileBuffer), classTransform);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
        if (bytes == null) {
            return null;
        }
        List<VerifyError> verify = ClassFile.of().verify(bytes);
        if (!verify.isEmpty()) {
            verify.forEach(Throwable::printStackTrace);
            return null;
        }
        Utils.saveToFile(classInterName, bytes);
        SimpleLog.info("transform end {}", className);
        return bytes;
    }

    private ClassTransform getClassTransform() {

        CodeTransform codeTransform=new CodeTransform() {
            @Override
            public void accept(CodeBuilder builder, CodeElement element) {
                builder.with(element);
            }

            @Override
            public void atStart(CodeBuilder builder) {
                builder.aload(1)
                        .invokestatic(ClassDesc.of(owner),"checkURI", MethodTypeDesc.of(ConstantDescs.CD_void,ClassDesc.of(URI.class.getName())));
                ;
            }
        };
        MethodTransform methodTransform = MethodTransform.transformingCode(codeTransform);
        return ClassTransform.transformingMethods((methodModel)-> {
            boolean init=methodModel.methodName().equalsString("<init>")&&methodModel.methodTypeSymbol()
                    .equals(MethodTypeDesc.of(ConstantDescs.CD_void,ClassDesc.of(URI.class.getName())));
            boolean uri=methodModel.methodName().equalsString("uri")&&methodModel.methodTypeSymbol()
                    .equals(MethodTypeDesc.of(ClassDesc.of("jdk.internal.net.http.HttpRequestBuilderImpl"),ClassDesc.of(URI.class.getName())));
            return init||uri;
        },methodTransform);
    }
}
