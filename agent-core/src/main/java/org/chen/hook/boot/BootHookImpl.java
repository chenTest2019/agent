package org.chen.hook.boot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chen.launch.Launch;
import org.chen.transform.boot.BootClassFileTransformer;

import org.chen.utils.Utils;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashMap;
import java.util.List;

public class BootHookImpl implements BootHook {
    private static final Logger log = LogManager.getLogger(BootHookImpl.class);

    @Override
    public void hook(Instrumentation inst) {

        var transformers = Utils.findHookClasses(Launch.jarFile, BootClassFileTransformer.class,
                "org/chen/transform/boot");
        HashMap<BootClassFileTransformer, List<String>> transformerMap = new HashMap<>();
        for (BootClassFileTransformer transformer : transformers) {
            try {
                log.info("hook transformer:{} {}" , transformer.getClass().getName() ,  transformer.isHook());

                if (transformer.isHook()) {
                    inst.addTransformer(transformer, true);
                    transformerMap.put(transformer, transformer.getHookClasses());
                }
            } catch (Exception e) {
                IO.println("hookClass e:" + e);
            }
        }
        // 使已加载的类能够生效 retransformClasses
        var collect = transformerMap.values().stream().flatMap(List::stream).toList();
        log.info("{} need hook {}", this, collect.toString());
        var allLoadedClasses = inst.getAllLoadedClasses();
        for (Class<?> allLoadedClass : allLoadedClasses) {
            if (collect.contains(allLoadedClass.getName())) {
                try {
                    log.info("retransformClasses {}", allLoadedClass);
                    inst.retransformClasses(allLoadedClass);
                } catch (UnmodifiableClassException e) {
                    log.info("retransformClasses {} e:", allLoadedClass, e);
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
