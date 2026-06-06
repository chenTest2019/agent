package org.chen.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandLineArgs implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String agentArgs;
    private String appName;
    private String version;
    private boolean debug;
    private String config;
}
