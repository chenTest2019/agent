package org.chen.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 仿 ServiceLoader 的手动指定包扫描器
 * 无需 META-INF/services 配置文件，通过传参指定扫描目录
 * @param <S> 要扫描的接口类型
 */
public class DirectoryServiceLoader<S> implements Iterable<S> {
    private final Class<S> serviceInterface;
    private final ClassLoader classLoader;
    private final String[] basePackages;
    private final List<S> cachedInstances = new CopyOnWriteArrayList<>();
    private volatile boolean isScanned = false;

    /**
     * 私有构造函数，通过静态工厂方法创建实例
     */
    private DirectoryServiceLoader(Class<S> serviceInterface, ClassLoader classLoader, String[] basePackages) {
        this.serviceInterface = serviceInterface;
        this.classLoader = classLoader;
        this.basePackages = basePackages;
    }

    // -------------------------------------------------------------------------
    // 静态工厂方法（和 ServiceLoader 完全一致的 API）
    // -------------------------------------------------------------------------

    /**
     * 使用当前线程上下文类加载器加载指定包下的接口实现
     * @param service 要扫描的接口
     * @param basePackages 要扫描的根包（多个），例如 "org.chen", "com.other"
     * @return DirectoryServiceLoader 实例
     */
    public static <S> DirectoryServiceLoader<S> load(Class<S> service, String... basePackages) {
        return load(service, Thread.currentThread().getContextClassLoader(), basePackages);
    }

    /**
     * 使用指定类加载器加载指定包下的接口实现
     * @param service 要扫描的接口
     * @param classLoader 用于加载实现类的类加载器
     * @param basePackages 要扫描的根包（多个）
     * @return DirectoryServiceLoader 实例
     */
    public static <S> DirectoryServiceLoader<S> load(Class<S> service, ClassLoader classLoader, String... basePackages) {
        // 参数校验
        if (service == null) throw new NullPointerException("service interface cannot be null");
        if (classLoader == null) throw new NullPointerException("classLoader cannot be null");
        if (basePackages == null || basePackages.length == 0) {
            throw new IllegalArgumentException("at least one base package must be specified");
        }
        return new DirectoryServiceLoader<>(service, classLoader, basePackages);
    }

    // -------------------------------------------------------------------------
    // 核心方法
    // -------------------------------------------------------------------------

    /**
     * 获取迭代器（懒加载：第一次调用才执行扫描和实例化）
     */
    @Override
    public Iterator<S> iterator() {
        ensureScanned();
        return cachedInstances.iterator();
    }

    /**
     * 获取所有已加载的实现类实例（不可变列表）
     */
    public List<S> getInstances() {
        ensureScanned();
        return Collections.unmodifiableList(cachedInstances);
    }

    /**
     * 清空缓存，重新扫描所有包
     */
    public void reload() {
        synchronized (this) {
            cachedInstances.clear();
            isScanned = false;
        }
    }

    // -------------------------------------------------------------------------
    // 内部实现
    // -------------------------------------------------------------------------

    /**
     * 确保已经完成扫描
     */
    private void ensureScanned() {
        if (!isScanned) {
            synchronized (this) {
                if (!isScanned) {
                    scanAllPackages();
                    isScanned = true;
                }
            }
        }
    }

    /**
     * 扫描所有指定的包
     */
    private void scanAllPackages() {
        for (String basePackage : basePackages) {
            try {
                scanSinglePackage(basePackage);
            } catch (Exception e) {
                System.err.printf("[DirectoryServiceLoader] 扫描包失败: %s, 错误: %s%n", basePackage, e.getMessage());
            }
        }
    }

    /**
     * 扫描单个包
     */
    private void scanSinglePackage(String basePackage) throws IOException {
        String resourcePath = basePackage.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(resourcePath);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            switch (protocol) {
                case "file":
                    // 扫描文件系统目录
                    File directory = new File(resource.getFile());
                    if (directory.exists() && directory.isDirectory()) {
                        scanFileSystemDirectory(directory, basePackage);
                    }
                    break;
                case "jar":
                    // 扫描 JAR 文件
                    JarURLConnection jarConn = (JarURLConnection) resource.openConnection();
                    try (JarFile jarFile = jarConn.getJarFile()) {
                        scanJarFile(jarFile, basePackage);
                    }
                    break;
                default:
                    // 忽略其他协议（如 jrt:/ 等）
                    break;
            }
        }
    }

    /**
     * 扫描文件系统目录
     */
    private void scanFileSystemDirectory(File directory, String currentPackage) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // 递归扫描子包
                scanFileSystemDirectory(file, currentPackage + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                // 处理 class 文件
                String className = currentPackage + "." + file.getName().substring(0, file.getName().length() - 6);
                loadAndInstantiateClass(className);
            }
        }
    }

    /**
     * 扫描 JAR 文件
     */
    private void scanJarFile(JarFile jarFile, String basePackage) {
        String basePath = basePackage.replace('.', '/') + "/";
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            // 只处理指定包下的 class 文件
            if (entryName.startsWith(basePath) && entryName.endsWith(".class") && !entry.isDirectory()) {
                // 转换为全限定类名
                String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                loadAndInstantiateClass(className);
            }
        }
    }

    /**
     * 加载类并实例化（核心校验逻辑）
     */
    private void loadAndInstantiateClass(String className) {
        try {
            // 1. 加载类
            Class<?> clazz = classLoader.loadClass(className);

            // 2. 校验条件
            if (!isValidImplementation(clazz)) {
                return;
            }

            // 3. 获取无参构造函数
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true); // 允许访问私有构造函数

            // 4. 实例化并转换为接口类型
            S instance = serviceInterface.cast(constructor.newInstance());

            // 5. 添加到缓存
            cachedInstances.add(instance);
            System.out.printf("[DirectoryServiceLoader] 成功加载实现类: %s%n", className);

        } catch (ClassNotFoundException e) {
            // 忽略无法加载的类
        } catch (NoSuchMethodException e) {
            System.err.printf("[DirectoryServiceLoader] 类 %s 没有无参构造函数%n", className);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            System.err.printf("[DirectoryServiceLoader] 实例化类失败: %s, 错误: %s%n", className, e.getMessage());
        } catch (Throwable t) {
            // 捕获所有异常，绝对不能影响原方法执行
            System.err.printf("[DirectoryServiceLoader] 加载类时发生意外错误: %s, 错误: %s%n", className, t.getMessage());
        }
    }

    /**
     * 校验类是否是有效的接口实现
     */
    private boolean isValidImplementation(Class<?> clazz) {
        return serviceInterface.isAssignableFrom(clazz)       // 实现了目标接口
                && !clazz.isInterface()                        // 不是接口本身
                && !Modifier.isAbstract(clazz.getModifiers())  // 不是抽象类
                && !clazz.isEnum()                             // 不是枚举
                && !clazz.isAnnotation();                      // 不是注解
    }
}