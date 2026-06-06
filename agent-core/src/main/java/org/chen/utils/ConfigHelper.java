package org.chen.utils;


import com.alibaba.fastjson2.JSONObject;
import org.chen.config.*;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ConfigHelper {
    private static final HashMap<String, Object> config = new HashMap<>();
    private static final List<String> list = List.of("com.janetfilter.core.utils", "jdk.internal.org.objectweb");
    private static final HashMap<String, Date> myExpirationDates = new HashMap<>();

    public static void setConfig(String json) {
        var jsonObject =  JSONObject.parse(json);
        //SimpleLog.info("readFileToString :"+jsonObject);
        ConfigKeyEnum[] values = ConfigKeyEnum.values();
        for (ConfigKeyEnum value : values) {
            if (!(jsonObject.containsKey(value.getKey().getSimpleName()))) {
                continue;
            }
            config.put(value.getKey().getSimpleName(), jsonObject.getObject(value.getKey().getSimpleName(),value.getKey()));
        }
    }

    public static void setConfig(String key, Object value) {
        config.put(key, value);
    }

    public static <T> T getConfig(Class<T> key) {
        Object configObj = config.get(key.getSimpleName());
        return key.cast(configObj);
    }
    private static Date licenseExpirationDate = getLicenseExpirationDate();

    public static boolean canCracked(MyBigIntegerConfig config, BigInteger x, BigInteger y, BigInteger z) {
        if (config == null) {
            return false;
        }
        return config.getX().equals(x.toString())
                && config.getY().equals(y.toString())
                && config.getZ().equals(z.toString());
    }

    public static BigInteger oddModPow(BigInteger x, BigInteger y, BigInteger z) {
        var config = getConfig(BigIntegerConfig.class);
        if (config == null) {
            IO.println("not cracked config is null");
            return null;
        }
        //System.out.printf("\n %s\tx y z\n %s\n%s\n%s\n", LocalDateTime.now(), x, y, z);
        var records = config.getRecords();
        var optional = records.stream()
                .filter(record -> canCracked(record, x, y, z))
                .findFirst();

        if (optional.isPresent()) {
            var configOptional = optional.get();
            //SimpleLog.info("configHelper have found cracked");
            return new BigInteger(configOptional.getResult());
        }
        //SimpleLog.info("not cracked not match");

        return null;
    }

    public static void getAllByName(String host) throws IOException {
        if (host!=null&&!host.isBlank()) {
            var config = getConfig(DNSConfig.class);
            List<String> domain = null;
            IO.println("getAllByName host : " + host);
            if (config != null) {
                domain = config.getValues();
                for (String s : domain) {
                    if (host.contains(s)) {
                        IO.println("Reject dns query: " + host + ", config: " + s);
                        throw new java.net.UnknownHostException();
                    }
                }
            }
        }
    }

    public static Boolean isReachable(InetAddress n) {
        var config = getConfig(DNSConfig.class);
        List<String> list = null;
        SimpleLog.info("isReachable InetAddress : " + n.getHostName());
        if (config != null) {
            list = config.getValues();
            for (String s : list) {
                if (n.getHostName().contains(s)) {
                    SimpleLog.info("Reject dns reachable : " + n.getHostName() + ", config: " + s);
                    return false;
                }
            }
        }
        return null;
    }

    public static void openServer(URL url) throws IOException {
        var string = url.toString();
        var config = getConfig(URLConfig.class);
        List<String> list;
        SimpleLog.debug("openServer url : " + url);
        if (config != null) {
            list = config.getValues();
            for (String s : list) {
                if (string.contains(s)) {
                    SimpleLog.info("Reject url : " + url + ", config: " + s);
                    //这里必须抛出异常 不然拦截不住
                    throw new SocketTimeoutException("connect timed out");
                }
            }
        }
    }

    public static List<String> getVmArguments(List<String> vmArgs) {
        SimpleLog.info("getVmArguments:" + vmArgs.size());
        List<String> list = new ArrayList<>(vmArgs);
        var iterator = list.iterator();
        var blacklist = List.of("jdk.internal.org.objectweb", "javaagent");
        try {
            while (iterator.hasNext()) {
                String next = iterator.next();
                if (blacklist.stream().anyMatch(next::contains)) {
                    iterator.remove();
                }
                if (next.startsWith("-Djanf.debug=")) {
                    SimpleLog.info("next:" + next);
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            SimpleLog.info("getVmArguments e:" + e);
        }
        SimpleLog.info("getVmArguments:" + list.size());
        return Collections.unmodifiableList(list);
    }

    public static String[] getVmArguments(String[] vmArgs) {
        return getVmArguments(Arrays.asList(vmArgs)).toArray(new String[0]);
    }

    public static Path getUserOptionsFile(Path path) {
        try {

            var contextClassLoader = Thread.currentThread().getContextClassLoader();
            Class<?> vmArgsClass = contextClassLoader.loadClass("com.intellij.diagnostic.VMOptions");
            Class<?> pathManager = contextClassLoader.loadClass("com.intellij.openapi.application.PathManager");

            String fileName = (String) vmArgsClass.getDeclaredMethod("getFileName").invoke(null);
            String location = (String) pathManager.getDeclaredMethod("getBinPath").invoke(null);

            SimpleLog.info("getUserOptionsFile location:{},fileName:{}", location, fileName);
            return Paths.get(location, fileName);
        } catch (Exception e) {
            SimpleLog.info("getUserOptionsFile:{}", e);
        }
        return path;
    }

    /**
     * 设置证书过期时间为 永远为30 天，绕过大于 60 天的检测
     */
    public static Date getLicenseExpirationDate() {
        if (licenseExpirationDate == null) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 30);
            SimpleLog.info("getLicenseExpirationDate:{}", calendar.getTime());
            licenseExpirationDate = calendar.getTime();
        }
        return licenseExpirationDate;
    }

    public static void forName(String className)
            throws ClassNotFoundException {
        if (list.stream().anyMatch(className::contains)) {
            SimpleLog.info("forName find:{}", className);
            throw new ClassNotFoundException(className);
        }
    }

    public static Date getExpirationDate(Map<String, Date> dateMap, String code) {
        SimpleLog.info("getExpirationDate:{}", code);
        if (dateMap.containsKey(code)) {
            SimpleLog.info("getExpirationDate find:{}", code);
            return dateMap.get(code);
        } else {
            SimpleLog.info("getExpirationDate not find:{}", code);
        }
        return getLicenseExpirationDate();
    }

    public static Map<String, Date> expirationDates(Map<String, Date> expirationDates) {
        if (expirationDates == null) {
            expirationDates = myExpirationDates;
        } else {
            if (expirationDates.containsKey("org.chen.init")) {
                return expirationDates;
            }
        }
        SimpleLog.info("expirationDates:{}", expirationDates);
        for (String next : expirationDates.keySet()) {
            myExpirationDates.put(next, licenseExpirationDate);
            SimpleLog.info("expirationDates:{}", next);
        }
        myExpirationDates.put("org.chen.init", new Date());
        return Collections.unmodifiableMap(myExpirationDates);
    }


}
