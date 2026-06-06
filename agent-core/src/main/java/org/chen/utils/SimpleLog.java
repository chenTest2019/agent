package org.chen.utils;

/**
 * 极简日志工具，基于 System.out 实现
 * 支持 info/debug 级别，支持 {} 占位符，用法与 SLF4J 完全一致
 */
public class SimpleLog {
    // 全局开关：是否开启 debug 日志
    public static boolean DEBUG_ENABLED = true;

    // ========== 对外 API ==========

    public static void info(String pattern, Object... args) {
        log("INFO", pattern, args);
    }

    public static void debug(String pattern, Object... args) {
        if (DEBUG_ENABLED) {
            log("DEBUG", pattern, args);
        }
    }
    public static void error(String pattern, Object... args) {
        log("ERROR", pattern, args);
    }
    // ========== 内部实现 ==========

    private static void log(String level, String pattern, Object... args) {
        String message = format(pattern, args);
        // 输出格式：[级别] 消息内容
        System.out.printf("[%s] %s%n", level, message);
    }

    /**
     * 实现 {} 占位符替换
     * 处理参数数量与占位符数量不匹配的情况
     */
    private static String format(String pattern, Object... args) {
        if (pattern == null || args == null || args.length == 0) {
            return pattern;
        }

        StringBuilder sb = new StringBuilder(pattern.length() + args.length * 16);
        int argIndex = 0;
        int pos = 0;

        while (pos < pattern.length()) {
            int nextBrace = pattern.indexOf("{}", pos);
            if (nextBrace == -1 || argIndex >= args.length) {
                // 没有更多占位符，或者没有更多参数，追加剩余部分
                sb.append(pattern, pos, pattern.length());
                break;
            }

            // 追加占位符之前的内容
            sb.append(pattern, pos, nextBrace);
            // 追加参数值（处理 null）
            sb.append(args[argIndex] == null ? "null" : args[argIndex]);
            // 跳过占位符
            pos = nextBrace + 2;
            argIndex++;
        }

        return sb.toString();
    }
}