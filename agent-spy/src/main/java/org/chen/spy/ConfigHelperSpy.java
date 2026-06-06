package org.chen.spy;



import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;

/**
 * 空壳Spy类：无任何第三方依赖，仅做转发
 * 这个类会被Bootstrap类加载器加载
 */
public class ConfigHelperSpy  {
    // 缓存真正的ConfigHelper的方法，避免每次反射
    private static final HashMap<String,MethodHandle> methodHandleHashMap = new HashMap<>();
    static final String realHelp="com.chen.utils.ConfigHelper";
    static {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> configClass = Class.forName(realHelp, true, classLoader);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandles.Lookup privateLookupIn = MethodHandles.privateLookupIn(configClass, lookup);
            MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
            Method[] declaredMethods = configClass.getDeclaredMethods();
            for (Method method : declaredMethods) {
                int modifiers = method.getModifiers();
                MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
                MethodHandle methodHandle=null;
                if (Modifier.isPublic(modifiers)) {
                    methodHandle = publicLookup.findStatic(configClass, method.getName(),methodType );
                }else if (Modifier.isPrivate(modifiers)) {
                    method.setAccessible(true);
                    methodHandle = privateLookupIn.findStatic(configClass, method.getName(),methodType );
                }else {
                    continue;
                }
                methodHandleHashMap.put(configClass.getName()+"."+method.getName()+"."+ methodType,methodHandle);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public static BigInteger oddModPow(BigInteger x, BigInteger y, BigInteger z)  {
        try {
            MethodType methodType = MethodType.methodType(BigInteger.class, BigInteger.class,BigInteger.class,BigInteger.class);
            MethodHandle methodHandle = methodHandleHashMap.get(realHelp+".oddModPow."+methodType.toString());
            Object result = methodHandle.invoke(x,y,z);
            if (result == null) {
                return null;
            }
            return (BigInteger) result;
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println(e);
        }
        return null;
    }
    public static void forName(String className)throws ClassNotFoundException{
        MethodType methodType = MethodType.methodType(Void.class, String.class);
        MethodHandle methodHandle = methodHandleHashMap.get(realHelp+".forName."+methodType.toString());
        try {
            methodHandle.invoke(className);
        } catch (Throwable e) {
            System.out.println(e);
            if (e instanceof ClassNotFoundException classNotFoundException) {
                throw classNotFoundException;
            }else{
                e.printStackTrace();
            }
        }
    }
    public static void getAllByName(String host) throws IOException {
        MethodType methodType = MethodType.methodType(Void.class, String.class);
        MethodHandle methodHandle = methodHandleHashMap.get(realHelp+".getAllByName."+methodType.toString());
        try {
            methodHandle.invoke(host);
        } catch (Throwable e) {
            System.out.println(e);
            if (e instanceof IOException ioException) {
                throw ioException;
            }else{
                e.printStackTrace();
            }
        }
    }

    public static Boolean isReachable(InetAddress n) {
        MethodType methodType = MethodType.methodType(Boolean.class, InetAddress.class);
        MethodHandle methodHandle = methodHandleHashMap.get(realHelp+".isReachable."+methodType.toString());
        try {
            Object result = methodHandle.invoke(n);
            if (result == null) {
                return null;
            }
            return (Boolean) result;
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println(e);
        }
        return null;
    }

    public static void openServer(URL url) throws IOException {
        MethodType methodType = MethodType.methodType(void.class, URL.class);
        MethodHandle methodHandle = methodHandleHashMap.get(realHelp+".openServer."+methodType.toString());
        try {
            methodHandle.invoke(url);
        } catch (Throwable e) {
            System.out.println(e);
            if (e instanceof IOException ioException) {
                throw ioException;
            }else{
                e.printStackTrace();
            }
        }
    }
    public static void checkURI(URI uri) {
        System.out.println("checkURI:"+uri.toString());
        try {
            openServer(uri.toURL());
        } catch (IOException e) {
            e.printStackTrace();
             throw new IllegalArgumentException(e);
        }
    }
}
