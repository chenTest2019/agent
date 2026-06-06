package org.chen.config;

import lombok.Data;

import java.util.List;

@Data
public class DNSConfig {
    private boolean hook;
    private List<String> values;
}
