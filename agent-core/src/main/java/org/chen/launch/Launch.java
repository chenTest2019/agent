package org.chen.launch;

import org.chen.config.ConfigImpl;
import org.chen.hook.Hook;
import org.chen.hook.boot.BootHookImpl;
import org.chen.utils.SimpleLog;
import org.chen.utils.Utils;

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class Launch {
    public static final JarFile jarFile = Utils.getJarFile(Launch.class);
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


    public void start() {
        SimpleLog.info("Launch start");
        try {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            ClassLoader platformClassLoader = ClassLoader.getPlatformClassLoader();
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            System.out.println("contextClassLoader: "+contextClassLoader);
            System.out.println("platformClassLoader: "+platformClassLoader);
            System.out.println("systemClassLoader: "+systemClassLoader);
            inst.appendToBootstrapClassLoaderSearch(Utils.getBootstrapClassLoaderSearch("myAgentNext-1.0-SNAPSHOT-spy.jar"));
        } catch (Exception e) {
            SimpleLog.info("result:" + e);
        }
        new ConfigImpl().readJsonStringFromConfigFile(agentArgs, inst);
        try {
            boolean result = handler(jarFile, "com/chen/hook");
            SimpleLog.info("result:" + result);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }

    private boolean handler(JarFile jarFile, String path) {
        List<Hook> hookClasses = null;
        try {
            hookClasses = Utils.findHookClasses(jarFile, Hook.class, path);
        } catch (Exception e) {
            e.printStackTrace();
            SimpleLog.info("findHookClasses e {}", e);
        }
        if (hookClasses == null || hookClasses.isEmpty()) {
            SimpleLog.info("processJar findHookClasses null");
            return false;
        }
        SimpleLog.info("processJar findHookClasses {} {}", path, hookClasses.size());
        hookClasses.forEach(hook -> {
            SimpleLog.debug("hookClass {} {}", hook, hook.getClass().getName());
        });
        // todo 如果有多个BootHook 这里需要再优化
        //先处理bootHook
        var collect = hookClasses.stream().collect(Collectors.groupingBy(Object::getClass, Collectors.toList()));
        collect.get(BootHookImpl.class).forEach(bootHook -> {
            SimpleLog.debug("bootHook isHook {} {}", bootHook, bootHook.isHook());
            try {
                if (bootHook.isHook()) {
                    SimpleLog.info("bootHook end {}", bootHook);
                    bootHook.hook(inst);
                }
            } catch (Exception e) {
                SimpleLog.error("bootHook hookClass fail {} {}", bootHook, e);
            }

        });
        collect.remove(BootHookImpl.class);
        SimpleLog.info("collect size {}", collect.size());
        collect.forEach((key, value) -> {
            SimpleLog.info("commonHook {} {}", key, value);
            value.forEach(hook -> {
                SimpleLog.debug("commonHook isHook {} {}", hook, hook.isHook());
                try {
                    if (hook.isHook()) {
                        SimpleLog.info("commonHook end {}", hook);
                        hook.hook(inst);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    SimpleLog.error("commonHook hookClass fail {} {}", hook, e);
                }
            });
        });
        SimpleLog.info("processJar end {}", path);
        return true;
    }
}
