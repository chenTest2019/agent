package org.chen.utils;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chen.App;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class Utils {

    private static final Logger log = LogManager.getLogger(Utils.class);

    public static JarFile getJarFile(Class<?> clazz) {
        if (clazz==null){
            return null;
        }
        try {
            URI uri = getJarPath(clazz);
            return new JarFile(uri.getPath());
        } catch (IOException e) {
            log.error("getJarFile:{}",clazz,e);
            throw new RuntimeException(e);
        }
    }
    public static URI getJarPath(Class<?> clazz){
        URL location = clazz.getProtectionDomain().getCodeSource().getLocation();
        String path = "";
        if(location!=null&&location.getPath()!=null){
            path= location.getPath();
        }else{
            try {
                String className = clazz.getName().replace('.', '/') + ".class";
                URL resource = clazz.getResource("/" + className);
                if (resource != null) {
                    String str=resource.getPath();
                    int start = str.length();
                    int end = className.length();
                    path = str.substring(0, start-end-2);
                }
            } catch (Exception e) {
                // 最终兜底
                throw new RuntimeException(e);
            }
        }
        try {
            String jarPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
            return new URI(jarPath);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> findHookClasses(JarFile jarFile, Class<T> interfaceClass, String path) {
        if (jarFile == null || interfaceClass == null || path == null || path.isBlank()) {
            throw new IllegalArgumentException("参数不能为空");
        }
        // 2. 标准化路径：支持包名格式(com.xxx)和路径格式(com/xxx)，自动补全结尾/避免误匹配
        path = path.replace('.', '/');
        if (!path.endsWith("/")) {
            path += "/";
        }
        List<T> classes = new ArrayList<>();
        String finalPath = path;
        jarFile.stream().filter(jarEntry ->  jarEntry.getName().startsWith(finalPath))
                .filter(jarEntry -> !jarEntry.isDirectory())
                .filter(jarEntry -> jarEntry.getName().endsWith(".class"))
                .filter(jarEntry -> !jarEntry.getName().contains("$"))
                .forEach(jarEntry -> {
                    log.info("jarEntry.getName():{}" , jarEntry.getName());
                    String className = jarEntry.getName().replace("/", ".").replace(".class", "");
                    Class<?> clazz;
                    try {
                        clazz = Class.forName(className,false,Thread.currentThread().getContextClassLoader());
                    } catch (ClassNotFoundException e) {
                        log.info("Class not found: {} {}", className, e);
                        throw new RuntimeException(e);
                    }
                    if (clazz.isInterface()) {
                        return;
                    }
                    if (interfaceClass.isAssignableFrom(clazz)) {
                        try {
                            Object o = clazz.getDeclaredConstructor().newInstance();
                            classes.add(interfaceClass.cast(o));
                        } catch (Exception e) {
                            log.error("getHookClass:{}",className,e);
                            throw new RuntimeException(e);
                        }
                    }
        });
        return classes;
    }
    public static void saveToFile(String fileName, byte[] bytes) {
        try {
            Path filePath = Path.of("E:/ideame", fileName+".class");
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, bytes);
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

    public static JarFile getBootstrapClassLoaderSearch(String fileName) {
        URI jarPath =getJarPath(App.class);
        IO.println("jarPath:" + jarPath);
        try {
            return new JarFile(Path.of(jarPath).getParent().resolve(fileName).toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
