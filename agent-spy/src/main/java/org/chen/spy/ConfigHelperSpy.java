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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/**
 * 空壳Spy类：无任何第三方依赖，仅做转发
 * 这个类会被Bootstrap类加载器加载
 */
public class ConfigHelperSpy  {
    // 缓存真正的ConfigHelper的方法，避免每次反射
    private static final HashMap<String,MethodHandle> methodHandleHashMap = new HashMap<>();
    static final String realHelp="org.chen.utils.ConfigHelper";
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
                MethodHandle methodHandle;
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
            System.err.println("ConfigHelperSpy e:"+e);
        }
        return null;
    }
    public static void forName(String className)throws ClassNotFoundException{
        MethodType methodType = MethodType.methodType(Void.class, String.class);
        MethodHandle methodHandle = methodHandleHashMap.get(realHelp+".forName."+methodType.toString());
        try {
            methodHandle.invoke(className);
        } catch (Throwable e) {
            if (e instanceof ClassNotFoundException classNotFoundException) {
                throw classNotFoundException;
            }else{
                System.err.println("ConfigHelperSpy e:"+e);
            }
        }
    }
    public static void getAllByName(String host) throws IOException {
        MethodType methodType = MethodType.methodType(Void.class, String.class);
        MethodHandle methodHandle = methodHandleHashMap.get(realHelp+".getAllByName."+methodType.toString());
        try {
            methodHandle.invoke(host);
        } catch (Throwable e) {
            if (e instanceof IOException ioException) {
                throw ioException;
            }else{
                System.err.println("ConfigHelperSpy e:"+e);
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
            System.err.println("ConfigHelperSpy e:"+e);
        }
        return null;
    }

    public static void openServer(URL url) throws IOException {
        MethodType methodType = MethodType.methodType(void.class, URL.class);
        MethodHandle methodHandle = methodHandleHashMap.get(realHelp+".openServer."+methodType.toString());
        try {
            methodHandle.invoke(url);
        } catch (Throwable e) {
            if (e instanceof IOException ioException) {
                throw ioException;
            }else{
                System.err.println("ConfigHelperSpy e:"+e);
            }
        }
    }
    public static void checkURI(URI uri) {
        try {
            openServer(uri.toURL());
        } catch (IOException e) {
            System.err.println("ConfigHelperSpy e:"+e);
            throw new IllegalArgumentException(e);
        }
    }

    public static Path getUserOptionsFile(Path path){
        if (path == null) {
            return null;
        }
        MethodType methodType = MethodType.methodType(Path.class, Path.class);
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StackTraceElement stackTraceElement = stackTrace[1];
        String className = stackTraceElement.getClassName();
        String methodName = stackTraceElement.getMethodName();
        String fileName = stackTraceElement.getFileName();
        int lineNumber = stackTraceElement.getLineNumber();
        MethodHandle methodHandle = methodHandleHashMap.get(realHelp+"."+methodName+"."+methodType.toString());
        try {
            Object invoke = methodHandle.invoke(path);
            return (Path) invoke;
        } catch (Throwable e) {
            System.err.println("ConfigHelperSpy e:"+e);
        }
        return path;
    }

    public static Date getLicenseExpirationDate() {
        MethodType methodType = MethodType.methodType(Date.class);
        MethodHandle methodHandle = methodHandleHashMap.get(realHelp+".getLicenseExpirationDate."+methodType.toString());
        try {
            Object result = methodHandle.invoke();
            if (result == null) {
                return null;
            }
            return (Date) result;
        } catch (Throwable e) {
            System.err.println("ConfigHelperSpy e:"+e);
        }
        return null;
    }

    public static Date getExpirationDate(Map<String, Date> dateMap, String code) {
        MethodType methodType = MethodType.methodType(Date.class, Map.class, String.class);
        MethodHandle methodHandle = methodHandleHashMap.get(realHelp+".getExpirationDate."+methodType.toString());
        try {
            Object result = methodHandle.invoke(dateMap, code);
            if (result == null) {
                return null;
            }
            return (Date) result;
        } catch (Throwable e) {
            System.err.println("ConfigHelperSpy e:"+e);
        }
        return null;
    }
    public static Map<String, Date> expirationDates(Map<String, Date> expirationDates) {
        MethodType methodType = MethodType.methodType(Map.class, Map.class);
        MethodHandle methodHandle = methodHandleHashMap.get(realHelp+".expirationDates."+methodType.toString());
        try {
            Object result = methodHandle.invoke(expirationDates);
            if (result == null) {
                return null;
            }
            return (Map<String, Date>) result;
        } catch (Throwable e) {
            System.err.println("ConfigHelperSpy e:"+e);
        }
        return null;
    }

    public static List<String> getVmArguments(List<String> vmArgs) {
        MethodType methodType = MethodType.methodType(List.class, List.class);
        MethodHandle methodHandle = methodHandleHashMap.get(realHelp+".getVmArguments."+methodType.toString());
        try {
            Object result = methodHandle.invoke(vmArgs);
            if (result == null) {
                return null;
            }
            return (List<String>) result;
        } catch (Throwable e) {
            System.err.println("ConfigHelperSpy e:"+e);
        }
        return null;

    }

    public static String getOfflineActivationCode(String str) {
//        String str="{\n" +
//                "    \"paidKey\":\"XXX\",\n" +
//                "    \"valid\":true,\n" +
//                "    \"userMac\":\"BC-24-11-A4-C0-90\",\n" +
//                "    \"validTo\":4859711999000\n" +
//                "}";
        String string = new String(Base64.getEncoder().encode(str.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        System.out.println(string);
        return string;
    }
}
