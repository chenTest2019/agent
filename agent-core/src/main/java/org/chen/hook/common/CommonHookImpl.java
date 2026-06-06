package org.chen.hook.common;

import org.chen.launch.Launch;
import org.chen.transform.common.CommonClassFileTransformer;
import org.chen.utils.SimpleLog;
import org.chen.utils.Utils;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashMap;
import java.util.List;
/*如果canRetransform参数被设置为false，然后显式调用Instrumentation接口的retransformClasses(Class<?>... classes)方法，
 不会触发对已加载类的Transformer。
 canRetransform参数的作用是决定注册的ClassFileTransformer是否具备重新转换（retransform）已加载类的能力。
 当其值为false时，表示该Transformer不支持对已加载类进行重新转换。在这种情况下：
即使您尝试调用retransformClasses(Class<?>... classes)方法，并传入已经加载的类列表，
 由于对应的Transformer不具备重新转换的能力（即canRetransform=false），
 JVM在执行retransformClasses()时会忽略这些没有重新转换权限的Transformer。
因此，即使显式调用了retransformClasses()，这些已加载类的字节码也不会经过指定的Transformer进行处理。
 只有在canRetransform设置为true时，retransformClasses()才能成功触发对已加载类的Transformer。
结论：当canRetransform参数被设置为false时，即使显式调用retransformClasses()方法，
也无法触发对已加载类的Transformer。若要触发重新转换，需要确保canRetransform参数为true。
*/

public class CommonHookImpl implements CommonHook {

    @Override
    public void hook(Instrumentation inst) {
        var transformers = Utils.findHookClasses(Launch.jarFile, CommonClassFileTransformer.class,
                "org/chen/transform/common");
        HashMap<CommonClassFileTransformer, List<String>> transformerMap = new HashMap<>();

        for (CommonClassFileTransformer transformer : transformers) {
            try {
                SimpleLog.info("hook transformer:" + transformer.getClass().getName() + "\t" + transformer.isHook());

                if (transformer.isHook()) {
                    inst.addTransformer(transformer, true);
                    transformerMap.put(transformer, transformer.getHookClasses());
                }
            } catch (Exception e) {
                SimpleLog.info("hookClass e: {}", e);
            }
        }
        // 使已加载的类能够生效 retransformClasses
        var collect = transformerMap.values().stream().flatMap(List::stream).toList();
        SimpleLog.info("need hook {}", collect.toString());
        var allLoadedClasses = inst.getAllLoadedClasses();
        for (Class<?> allLoadedClass : allLoadedClasses) {
            if (collect.contains(allLoadedClass.getName())) {
                try {
                    SimpleLog.info("retransformClasses {}", allLoadedClass);
                    inst.retransformClasses(allLoadedClass);
                } catch (UnmodifiableClassException e) {
                    SimpleLog.info("retransformClasses {} e:", allLoadedClass, e);
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
