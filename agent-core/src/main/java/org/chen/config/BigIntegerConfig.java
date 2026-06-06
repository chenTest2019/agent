package org.chen.config;

import lombok.Data;

import java.util.List;

@Data
public class BigIntegerConfig {
    private boolean hook;
    private List<MyBigIntegerConfig> records;
}
