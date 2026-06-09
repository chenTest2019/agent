package org.chen.transform.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chen.utils.Utils;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.AccessFlag;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RainBowTransform implements CommonClassFileTransformer {
    private static final Logger log = LogManager.getLogger(RainBowTransform.class);
    private final List<String> hookClasses = new ArrayList<>();

    public RainBowTransform() {
        hookClasses.add("com.intellij.diagnostic.VMOptions");
        hookClasses.add("com.intellij.ui.LicensingFacade");
        hookClasses.add("sun.management.VMManagementImpl");
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
    public byte[] transform(ClassLoader loader, String classInterName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) throws IllegalClassFormatException {
        var className = classInterName.replace("/", ".");
        if (!this.hookClasses.contains(className)) {
            return classFileBuffer;
        }

        log.info("transform start {} {}", className, loader);
        ClassTransform classTransform = getClassTransform(classInterName);
        byte[] bytes = null;
        try {
            bytes = ClassFile.of(ClassFile.StackMapsOption.GENERATE_STACK_MAPS, ClassFile.StackMapsOption.STACK_MAPS_WHEN_REQUIRED)
                    .transformClass(ClassFile.of().parse(classFileBuffer), classTransform);
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

    private ClassTransform getClassTransform(String classInterName) {
        ClassTransform classTransform = null;
        switch (classInterName) {
            case "com/intellij/diagnostic/VMOptions":
                // VMOptions.getUserOptionsFile: insert dup/ifnull/invokestatic before each ARETURN


                MethodTransform vmOptionsMethodTransform = MethodTransform.transformingCode((builder, element) -> {
                    if (element instanceof Instruction instr && Opcode.ARETURN == instr.opcode()) {
                        Label label = builder.newLabel();
                        builder.dup()
                                .ifnull(label)
                                .invokestatic(ClassDesc.of(owner), "getUserOptionsFile",
                                        MethodTypeDesc.of(ClassDesc.of(Path.class.getName()), ClassDesc.of(Path.class.getName())));
                        builder.labelBinding(label);
                    }
                    builder.accept(element);

                });
//                classTransform=ClassTransform.transformingMethods( methodModel -> methodModel.methodName().equalsString("getUserOptionsFile")
//                        && methodModel.methodTypeSymbol().equals(MethodTypeDesc.of(ClassDesc.of(Path.class.getName()))), vmOptionsMethodTransform);
                classTransform = (builder, element) -> {
                    if (element instanceof MethodModel m && m.methodName().equalsString("getUserOptionsFile")
                    &&m.methodTypeSymbol().equals(MethodTypeDesc.of(ClassDesc.of(Path.class.getName())))) {
                           builder.transformMethod(m, vmOptionsMethodTransform);
                    } else {
                        builder.with(element);
                    }
                };
                break;
            case "com/intellij/ui/LicensingFacade":
                // LicensingFacade.getLicenseExpirationDate -> replace body with invokestatic(owner, getLicenseExpirationDate) and areturn
                        MethodTransform licenseGetLicenseExpirationDate = MethodTransform.transformingCode(new CodeTransform() {
                            @Override
                            public void accept(CodeBuilder builder, CodeElement element) {
                                // do not emit original instructions: we replace method body entirely
                            }

                            @Override
                            public void atStart(CodeBuilder builder) {
                                builder.invokestatic(ClassDesc.of(owner), "getLicenseExpirationDate",
                                                MethodTypeDesc.of(ClassDesc.of("java.util.Date")))
                                        .areturn();
                            }
                        });

                // LicensingFacade.getExpirationDate(String) -> call owner.getExpirationDate(Map,String)
                        MethodTransform licenseGetExpirationDate = MethodTransform.transformingCode(new CodeTransform() {
                            @Override
                            public void accept(CodeBuilder builder, CodeElement element) {
                                // replace entire method body; do not emit original instructions
                            }

                            @Override
                            public void atStart(CodeBuilder builder) {
                                builder.aload(0)
                                        .getfield(ClassDesc.ofInternalName(classInterName), "expirationDates", ClassDesc.of("java.util.Map"))
                                        .aload(1)
                                        .invokestatic(ClassDesc.of(owner), "getExpirationDate",
                                                MethodTypeDesc.of(ClassDesc.of("java.util.Date"), ClassDesc.of("java.util.Map"), ClassDesc.of(String.class.getName())))
                                        .areturn();
                            }
                        });

                // Update expirationDates at start of every non-static, non-constructor method
                MethodTransform updateExpirationDates = MethodTransform.transformingCode(new CodeTransform() {
                    @Override
                    public void accept(CodeBuilder builder, CodeElement element) {
                        builder.with(element);
                    }

                    @Override
                    public void atStart(CodeBuilder builder) {
                        builder.aload(0)
                                .dup()
                                .getfield(ClassDesc.ofInternalName(classInterName), "expirationDates", ClassDesc.of("java.util.Map"))
                                .invokestatic(ClassDesc.of(owner), "expirationDates",
                                        MethodTypeDesc.of(ClassDesc.of("java.util.Map"), ClassDesc.of("java.util.Map")))
                                .putfield(ClassDesc.ofInternalName(classInterName), "expirationDates", ClassDesc.of("java.util.Map"));
                    }
                });
                classTransform = (builder, element) -> {
                    switch (element) {
                        case MethodModel m when m.methodName().equalsString("getLicenseExpirationDate") && m.methodTypeSymbol().equals(MethodTypeDesc.of(ClassDesc.of("java.util.Date"))) ->
                                builder.transformMethod(m, licenseGetLicenseExpirationDate);
                        case MethodModel m when m.methodName().equalsString("getExpirationDate") && m.methodTypeSymbol().equals(MethodTypeDesc.of(ClassDesc.of("java.util.Date"), ClassDesc.of(String.class.getName()))) ->
                                builder.transformMethod(m, licenseGetExpirationDate);
                        case MethodModel m when !m.methodName().equalsString("<init>") && !m.flags().has(AccessFlag.STATIC) -> {
                            if(m.methodName().equalsString("isApplicableForProduct")||m.methodName().equalsString("isPerpetualForProduct")){
                                //System.out.println("transforming " + m.methodName());
                            }
                            builder.transformMethod(m, updateExpirationDates);
                        }
                        default -> builder.with(element);
                    }
                };break;
                case "sun/management/VMManagementImpl":

                    // VMManagementImpl.getVmArguments: insert vmArgs update before up to 2 ARETURNs
                    MethodTransform vmManagementGetVmArguments = MethodTransform.transformingCode(new CodeTransform() {
                        int inserted = 0;

                        @Override
                        public void accept(CodeBuilder builder, CodeElement element) {
                            if (element instanceof Instruction instr && Opcode.ARETURN == instr.opcode()) {
                                if (inserted < 2) {
                                    builder.aload(0)
                                            .aload(0)
                                            .getfield(ClassDesc.ofInternalName("sun/management/VMManagementImpl"), "vmArgs", ClassDesc.of("java.util.List"))
                                            .invokestatic(ClassDesc.of(owner), "getVmArguments",
                                                    MethodTypeDesc.of(ClassDesc.of("java.util.List"), ClassDesc.of("java.util.List")))
                                            .putfield(ClassDesc.ofInternalName("sun/management/VMManagementImpl"), "vmArgs", ClassDesc.of("java.util.List"));
                                    inserted++;
                                }
                            }
                            builder.accept(element);
                        }
                    });
                    classTransform = (builder, element) -> {
                        if (Objects.requireNonNull(element) instanceof MethodModel m && m.methodName().equalsString("getVmArguments") && m.methodTypeSymbol().equals(MethodTypeDesc.of(ClassDesc.of("java.util.List")))) {
                            builder.transformMethod(m, vmManagementGetVmArguments);
                        } else {
                            builder.with(element);
                        }
                    }; break;
            default:

        }
        return classTransform;
    }
}
