package org.chen.config;

import java.lang.instrument.Instrumentation;

public interface Config {
    void readJsonStringFromConfigFile(String agentArgs, Instrumentation inst);
}
