# Java Agent

A powerful Java Agent framework for runtime bytecode manipulation and class loading interception.

## Project Overview

This is a powerful Java Agent framework that supports dynamic transformation and modification of class files at JVM runtime. Through Java Agent technology, this framework enables interception, modification, and enhancement of application behavior without modifying the original source code.

## Core Features

- **Java Agent Standard Entry Points**: Supports both `premain` and `agentmain` startup modes
- **Configuration Management System**: Flexible configuration loading mechanism supporting JSON format
- **Multiple Type Transformers**: Built-in various class file transformers to meet diverse scenario requirements
- **Hot Deployment Support**: Supports dynamic loading and unloading of agents at runtime

## Module Structure

### agent-core

The core module containing the following main components:

#### Configuration Module (`org.chen.config`)
- `SystemConfig` - System configuration management
- `BigIntegerConfig` - BigInteger hook configuration
- `DNSConfig` - DNS hook configuration
- `URLConfig` - URL hook configuration
- `CommandLineArgs` - Command-line argument parser

#### Hook Module (`org.chen.hook`)
- `BootHook` - Boot-time hook
- `CommonHook` - General-purpose hook

#### Transformer Module (`org.chen.transform`)
- `BigIntegerTransform` - BigInteger operation transformation
- `DnsTransform` - DNS resolution transformation
- `HttpClientTransformer` - HTTP client transformation
- `HttpRequestBuilderImplTransformer` - HTTP request builder transformation
- `MybatisCodeHelperProTransformer` - MyBatis plugin transformation
- `RainBowTransform` - Rainbow transformer
- `VMOptionsTransformer` - JVM options transformation

#### Utility Module (`org.chen.utils`)
- `ConfigHelper` - Configuration utility
- `DirectoryServiceLoader` - Directory service loader
- `Utils` - General-purpose utility class

### agent-spy

Monitoring module providing runtime monitoring capabilities.

## Usage

### Build the Project

```bash
mvn clean package
```

### Run the Agent

#### Method 1: Using the -javaagent Parameter

```bash
java -javaagent:agent-core.jar=config.json -jar your-application.jar
```

#### Method 2: Using premain

```bash
java -javaagent:agent-core.jar=config.json -jar your-application.jar
```

### Configuration Example

```json
{
  "appName": "your-app",
  "version": "1.0.0",
  "debug": false,
  "config": "config.json"
}
```

## Technology Stack

- Java
- Maven
- ASM (Bytecode Manipulation)
- Javassist (Optional)

## License

This project is intended solely for learning and communication purposes. Do not use it for illegal purposes.