package org.chen.config;


import lombok.Getter;

@Getter
public enum ConfigKeyEnum {
    BIG_INTEGER_CONFIG(BigIntegerConfig.class),
    URL_CONFIG(URLConfig.class),
    DNS_CONFIG(DNSConfig.class),
    SYSTEM_CONFIG(SystemConfig.class),
    ;

    private final Class<?> key;

    ConfigKeyEnum(Class<?> key) {
        this.key = key;
    }

}
