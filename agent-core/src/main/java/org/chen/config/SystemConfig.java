package org.chen.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;


@EqualsAndHashCode(callSuper = true)
@Data
public class SystemConfig extends CommandLineArgs {


    private static final Lock lock = new ReentrantLock();
    private static volatile SystemConfig instance;
    private String appName;
    private String version;
    private boolean debug;
    private String config;
    private String level = Level.INFO.getName();

    public static SystemConfig getInstance(String version, boolean debug, String appName) {
        if (instance == null) {
            lock.lock();
            try {
                if (instance == null) {
                    instance = new SystemConfig();
                    instance.setVersion(version);
                    instance.setDebug(debug);
                    instance.setAppName(appName);
                }
            } finally {
                lock.unlock();
            }
        }
        return instance;
    }
}
