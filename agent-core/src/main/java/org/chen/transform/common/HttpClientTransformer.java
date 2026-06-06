package org.chen.transform.common;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chen.utils.Utils;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

public class HttpClientTransformer implements CommonClassFileTransformer {

    private static final Logger log = LogManager.getLogger(HttpClientTransformer.class);
    private final List<String> hookClasses = new ArrayList<>();

    public HttpClientTransformer() {
        log.info("HttpClientTransformer");
        hookClasses.add("sun.net.www.http.HttpClient");
        //sun.net.www.http.HttpClient.class.getName();
        //java.net.http.HttpClient.class.getName();
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
            return classFileBuffer;
        }
        log.info("transform start {}", className);
        ClassTransform classTransform = getClassTransform(classInterName);
        byte[] bytes = null;
        try {
            bytes = ClassFile.of().transformClass(ClassFile.of().parse(classFileBuffer), classTransform);
        } catch (Exception e) {
            log.error("transform e:" , e);
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
        log.info("transform end {}", className);
        return bytes;
    }

    private  ClassTransform getClassTransform(String classInterName) {
        CodeTransform codeTransform=new CodeTransform() {
            @Override
            public void accept(CodeBuilder builder, CodeElement element) {
                builder.with(element);
            }

            @Override
            public void atStart(CodeBuilder builder) {
                builder.aload(0)
                        .getfield(ClassDesc.ofInternalName(classInterName),"url", ClassDesc.of(URL.class.getName()))
                        .invokestatic(ClassDesc.of(owner),"openServer", MethodTypeDesc.of(ConstantDescs.CD_void,ClassDesc.of(URL.class.getName())));
                ;
            }
        };
        MethodTransform methodTransform = MethodTransform.transformingCode(codeTransform);
        return ClassTransform.transformingMethods((methodModel)-> methodModel.methodName().equalsString("openServer")&&methodModel.methodTypeSymbol()
                .equals(MethodTypeDesc.of(ConstantDescs.CD_void)),methodTransform);
    }


}
