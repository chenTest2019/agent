package org.chen.utils;
import org.chen.config.SystemConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * 设置日志级别时，为了确保能够正确打印对应类别的日志信息，需要遵循以下规则：
 * INFO:
 * 设置日志级别为 INFO 或其以下级别（如 WARN, ERROR, FATAL 或 ALL）时，INFO 日志将被打印。
 * 如果设置级别为 DEBUG、TRACE 或更高优先级级别（如 WARN, ERROR, FATAL），INFO 日志将不会被记录。
 * DEBUG:
 * 要打印 DEBUG 日志，需将日志级别设定为 DEBUG 或更低优先级级别（如 TRACE）。
 * 若设置级别为 INFO, WARN, ERROR, FATAL, 或 OFF，则 DEBUG 日志将被忽略。
 * WARN:
 * WARN 日志在日志级别设置为 WARN, ERROR, FATAL, 或 ALL 时会被输出。
 * 若日志级别设为 INFO, DEBUG, TRACE, 或 OFF，则不会记录 WARN 级别的消息。
 * ERROR:
 * 仅当日志级别设定为 ERROR, FATAL, 或 ALL 时，ERROR 日志才会被显示。
 * 如果日志级别设定为 WARN, INFO, DEBUG, TRACE, 或 OFF，ERROR 日志将被抑制。
 * 总结来说，日志级别设置的原则是：所设置的日志级别决定了系统将记录高于该级别的所有日志消息，而低于该级别的消息则会被过滤掉。因此，根据需要查看或保留的日志详细程度，应选择合适的日志级别，使得期望查看的类别（如 INFO, DEBUG, WARN, ERROR）能够成功打印出来。
 */
public class Log {
    static private final String regex = "\\{}";
    static private final String replacement = "%s";
    static private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");
    static private final DateTimeFormatter formatterLog = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm");
    private static final BlockingQueue<LogEntry> logQueue = new LinkedBlockingDeque<>();
    private static final ExecutorService logProcessorExecutor = Executors.newSingleThreadExecutor();

    private static final Pattern EXCLUDED_CLASSES_PATTERN = Pattern.compile(
            "java.lang.invoke." + "|"
                    + SimpleLog.class.getName() + "|"
                    + "getCallerInfo"
    );
    private static final LocalDateTime now = LocalDateTime.now();

    static {
        // 使用优雅的关闭方式
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            logProcessorExecutor.shutdown();
//            try {
//                if (!logProcessorExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
//                    logProcessorExecutor.shutdownNow(); // 取消当前执行的任务
//                }
//            } catch (InterruptedException e) {
//                logProcessorExecutor.shutdownNow();
//                Thread.currentThread().interrupt(); // 重新设置中断状态
//            }
//        }));

        logProcessorExecutor.submit(() -> {
            while (true) {
                try {
                    LogEntry entry = logQueue.take(); // 使用阻塞方式获取日志条目
                    processLogEntry(entry);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    //OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL
    private static void processLogEntry(LogEntry entry) {
        var config = ConfigHelper.getConfig(SystemConfig.class);
        Level info = Level.INFO;
        var appName = "app";
        if (config != null) {
            info = Level.parse(config.getLevel());
            appName = config.getAppName();
        }
        if (entry.level.intValue() < info.intValue()) {
            return;
        }
        // 实现具体的日志记录逻辑，如打印到控制台、写入文件、发送至远程服务等
        String log = entry.timestamp + " " + entry.level + " [" + entry.callerInfo + "] " + entry.message;
        System.out.println(log);
        String tempDirPath = System.getProperty("java.io.tmpdir");
        String path = tempDirPath + File.separator + appName + File.separator + now.format(formatterLog) + ".log";
        Path outputPath = Paths.get(path);
        if (!Files.exists(outputPath.getParent())) {
            try {
                Files.createDirectories(outputPath.getParent());
            } catch (Exception e) {
                System.out.println("Failed to createDirectories  : " + e.getMessage());
            }
        }
        try {
            Files.writeString(outputPath, log + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.APPEND
                    , StandardOpenOption.DSYNC
            );
        } catch (IOException e) {
            System.out.println("Failed to writeString  : " + e.getMessage());
        }
    }

    //OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL
    public static void info(String msg, Object... args) {
        log(Level.INFO, msg, args);
    }

    //OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL
    public static void debug(String msg, Object... args) {
        log(Level.FINE, msg, args);
    }

    //OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL
    public static void error(String msg, Object... args) {
        log(Level.SEVERE, msg, args);
    }

    //OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL
    public static void warn(String msg, Object... args) {
        log(Level.WARNING, msg, args);
    }

    private static void log(Level level, String msg, Object... args) {
        String formattedMsg = msg.replaceAll(regex, replacement);
        if (args.length > 0) {
            formattedMsg = String.format(formattedMsg, args);
        } else {
            formattedMsg = msg;
        }
        String timestamp = LocalDateTime.now().format(formatter);
        String callerInfo = getCallerInfo();
        logQueue.offer(new LogEntry(level, timestamp, callerInfo, formattedMsg));
    }

    /**
     * 获取调用者的相关信息。
     * 该方法利用StackWalker API遍历调用栈，找到调用当前Log类方法的上一级调用者的信息（类名和行号）。
     * 如果找到调用者信息，则返回格式化的字符串；如果没有找到，则返回"<unknown>:<unknown>"。
     *
     * @return 格式化的调用者信息字符串，格式为"类名:行号"；如果无法确定调用者，则返回"<unknown>:<unknown>"。
     */
    public static String getCallerInfo() {
        // 使用StackWalker API获取调用栈，并设置保留类引用的选项
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        // 遍历调用栈，找到调用当前方法的上一级非排除类的方法，并获取其行号
        Optional<StackWalker.StackFrame> callerFrame = walker.walk(s ->
                s.dropWhile(frame -> frame.getDeclaringClass().getName().equals(SimpleLog.class.getName())) // 跳过本类方法
                        .filter(frame -> !isExcludedClass(frame.getClassName()) && !frame.getMethodName().equals("getCallerInfo"))
                        .findFirst());
        // 如果找到调用者帧，格式化并返回其类名和行号；否则，返回未知信息
        return callerFrame.map(frame -> String.format("%s:%d", frame.getClassName(), frame.getLineNumber()))
                .orElse("<unknown>:<unknown>");
    }

    private static boolean isExcludedClass(String className) {
        // 根据需要添加需要排除的类名
        return EXCLUDED_CLASSES_PATTERN.matcher(className).find();
    }

    private record LogEntry(Level level, String timestamp, String callerInfo, String message) {
    }
}
