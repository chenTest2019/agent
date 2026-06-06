package org.chen.launch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chen.config.ConfigImpl;
import org.chen.hook.Hook;
import org.chen.hook.boot.BootHookImpl;

import org.chen.utils.Utils;

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class Launch {
    public static final JarFile jarFile = Utils.getJarFile(Launch.class);
    private static final String bootStrapJar="agent-spy-1.0-SNAPSHOT.jar";
    private static final Logger log = LogManager.getLogger(Launch.class);
    private Instrumentation inst;
    private String agentArgs;

    public Launch() {

    }

    public Launch(String agentArgs, Instrumentation inst) {
        this();
        this.agentArgs = agentArgs;
        this.inst = inst;
    }

    static void main(String[] args) {
    }


    public void start()  {
        log.info("Launch start");
        try {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            ClassLoader platformClassLoader = ClassLoader.getPlatformClassLoader();
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            log.info("contextClassLoader: {}",contextClassLoader);
            log.info("platformClassLoader: {}",platformClassLoader);
            log.info("systemClassLoader: {}",systemClassLoader);
            inst.appendToBootstrapClassLoaderSearch(Utils.getBootstrapClassLoaderSearch(bootStrapJar));
        } catch (Exception e) {
            log.error("启动失败",e);
            throw new RuntimeException(e);
        }
        new ConfigImpl().readJsonStringFromConfigFile(agentArgs, inst);
        try {
            boolean result = handler(jarFile, "org/chen/hook");
            log.info("result:{}" , result);
        } catch (Exception e) {
            log.error("handler 异常 ",e);
            throw new RuntimeException(e);
        }
    }

    private boolean handler(JarFile jarFile, String path) {
        List<Hook> hookClasses = null;
        try {
            hookClasses = Utils.findHookClasses(jarFile, Hook.class, path);
        } catch (Exception e) {
            log.atError().withThrowable(e).log("findHookClasses");
        }
        if (hookClasses == null || hookClasses.isEmpty()) {
            log.info("processJar findHookClasses null");
            return false;
        }
        log.info("processJar findHookClasses {} {}", path, hookClasses.size());
        hookClasses.forEach(hook -> {
            log.info("hookClass {} {}", hook, hook.getClass().getName());
        });
        // todo 如果有多个BootHook 这里需要再优化
        //先处理bootHook
        var collect = hookClasses.stream().collect(Collectors.groupingBy(Object::getClass, Collectors.toList()));
        collect.get(BootHookImpl.class).forEach(bootHook -> {
            log.debug("bootHook isHook {} {}", bootHook, bootHook.isHook());
            try {
                if (bootHook.isHook()) {
                    log.info("bootHook end {}", bootHook);
                    bootHook.hook(inst);
                }
            } catch (Exception e) {
                log.error("bootHook hookClass fail {} {}", bootHook, e);
            }

        });
        collect.remove(BootHookImpl.class);
        log.info("collect size {}", collect.size());
        collect.forEach((key, value) -> {
            log.info("commonHook {} {}", key, value);
            value.forEach(hook -> {
                log.debug("commonHook isHook {} {}", hook, hook.isHook());
                try {
                    if (hook.isHook()) {
                        log.info("commonHook end {}", hook);
                        hook.hook(inst);
                    }
                } catch (Exception e) {
                    log.atError().withThrowable(e).log("commonHook hookClass fail {}", hook);
                }
            });
        });
        log.info("processJar end {}", path);
        return true;
    }
}
