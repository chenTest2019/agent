# Java Agent

A powerful Java Agent framework for runtime bytecode manipulation and class loading interception.

## 项目简介

这是一个功能强大的 Java Agent 框架，支持在 JVM 运行时对类文件进行动态转换和修改。通过 Java Agent 技术，该框架可以在不修改原始源代码的情况下，对应用程序的行为进行拦截、修改和增强。

## 核心特性

- **Java Agent 标准入口**: 支持 `premain` 和 `agentmain` 两种启动方式
- **配置管理系统**: 灵活的配置加载机制，支持 JSON 格式配置
- **多类型转换器**: 内置多种类文件转换器，支持不同场景的需求
- **热部署支持**: 支持在运行时动态加载和卸载 agent

## 模块结构

### agent-core

核心模块，包含以下主要组件：

#### 配置模块 (`org.chen.config`)
- `SystemConfig` - 系统配置管理
- `BigIntegerConfig` - BigInteger 钩子配置
- `DNSConfig` - DNS 钩子配置
- `URLConfig` - URL 钩子配置
- `CommandLineArgs` - 命令行参数解析

#### Hook 模块 (`org.chen.hook`)
- `BootHook` - 启动时钩子
- `CommonHook` - 通用钩子

#### 转换器模块 (`org.chen.transform`)
- `BigIntegerTransform` - BigInteger 操作转换
- `DnsTransform` - DNS 解析转换
- `HttpClientTransformer` - HTTP 客户端转换
- `HttpRequestBuilderImplTransformer` - HTTP 请求构建器转换
- `MybatisCodeHelperProTransformer` - MyBatis 插件转换
- `RainBowTransform` - 彩虹转换器
- `VMOptionsTransformer` - JVM 选项转换

#### 工具模块 (`org.chen.utils`)
- `ConfigHelper` - 配置辅助工具
- `DirectoryServiceLoader` - 目录服务加载器
- `Utils` - 通用工具类

### agent-spy

监控模块，提供运行时监控能力。

## 使用方法

### 构建项目

```bash
mvn clean package
```

### 运行 Agent

#### 方式一：使用 -javaagent 参数

```bash
java -javaagent:agent-core.jar=config.json -jar your-application.jar
```

#### 方式二：使用 premain

```bash
java -javaagent:agent-core.jar=config.json -jar your-application.jar
```

### 配置示例

```json
{
  "appName": "your-app",
  "version": "1.0.0",
  "debug": false,
  "config": "config.json"
}
```

## 技术栈

- Java
- Maven
- ASM (字节码操作)
- Javassist (可选)

## 许可证

本项目仅供学习交流使用，请勿用于非法目的。