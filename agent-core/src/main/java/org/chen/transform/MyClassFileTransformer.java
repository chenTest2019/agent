package org.chen.transform;

import java.lang.instrument.ClassFileTransformer;
import java.util.List;

public interface MyClassFileTransformer extends ClassFileTransformer {
    default boolean isHook() {
        return true;
    }
    List<String> getHookClasses();
}
