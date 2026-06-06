package org.chen.config;


import com.alibaba.fastjson2.JSONObject;
import org.chen.App;
import org.chen.utils.ConfigHelper;
import org.chen.utils.SimpleLog;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

import static org.chen.utils.Utils.*;


public class ConfigImpl implements Config {

    public Path getConfigAbsolutePath(String agentArgs) {
        var pathStr = getConfigPath(agentArgs);
        SimpleLog.info("{}:{}", "getConfigAbsolutePath", pathStr);
        Path path = Path.of(pathStr);
        if (path.isAbsolute()) {
            return path;
        }
        File file = path.toFile();
        if (file.isDirectory()) {
            IO.println("path is directory:" + path);
            return null;
        }
        URI jarPath =getJarPath(App.class);
        IO.println("jarPath:" + jarPath);
        return Path.of(jarPath).getParent().resolve(path);
    }

    private String getConfigPath(String agentArgs) {
        var commandLineArgs = getCommandLineArgs(agentArgs);
        String config = commandLineArgs.getConfig();
        if (config == null || config.isBlank()) {
            return "config.json";
        }
        SimpleLog.info("getConfigPath :{}", config);
        return config;
    }

    private CommandLineArgs getCommandLineArgs(String agentArgs) {
        var commandLineArgs = new CommandLineArgs();
        JSONObject jsonObject = new JSONObject();
        if (agentArgs!=null&&!agentArgs.isBlank()) {
            for (String s : agentArgs.split(",")) {
                var split = s.split("=");
                jsonObject.put(split[0], split[1]);
            }
        }
        try {
            commandLineArgs = jsonObject.to(CommandLineArgs.class);
            String appName=commandLineArgs.getAppName();
            if (appName==null||appName.isBlank()) {
                commandLineArgs.setAppName("app");
            }
        } catch (Exception e) {
            e.printStackTrace();
            SimpleLog.error("readFileToString fail", e);
        }
        commandLineArgs.setAgentArgs(agentArgs);
        SystemConfig systemConfigJSON = JSONObject.parseObject(JSONObject.toJSONString(commandLineArgs), SystemConfig.class);
        ConfigHelper.setConfig(SystemConfig.class.getSimpleName(), systemConfigJSON);
        var systemConfig = ConfigHelper.getConfig(SystemConfig.class);
        if (systemConfig != null) {
            SimpleLog.debug("systemConfig {}", systemConfig.getAppName());
            SimpleLog.debug("systemConfig {}", systemConfig.getConfig());
            SimpleLog.debug("systemConfig {}", systemConfig.getLevel());
        }
        return commandLineArgs;
    }

    @Override
    public void readJsonStringFromConfigFile(String agentArgs, Instrumentation inst) {
        //inst.appendToSystemClassLoaderSearch(getJarFile(JSONObject.class));
        Path configFilePath = getConfigAbsolutePath(agentArgs);
        if (configFilePath == null) {
            return;
        }
        try {
            String s = Files.readString(configFilePath, StandardCharsets.UTF_8);
            //SimpleLog.info("readFileToString success:"+s);
            ConfigHelper.setConfig(s);
            //SimpleLog.info("org.chen.utils.ConfigHelper success:"+org.chen.utils.ConfigHelper.config);

        } catch (Exception e) {
            e.printStackTrace();
            IO.println("readFileToString fail：" + configFilePath + e.getMessage());
        }

    }
}
