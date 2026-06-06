package org.chen.hook;

import java.lang.instrument.Instrumentation;

public interface Hook {
    default boolean isHook() {
        return true;
    }
    // 钩子方法
    void hook(Instrumentation inst);
}
