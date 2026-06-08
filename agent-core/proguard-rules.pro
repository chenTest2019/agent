# 基础配置
#-ignorewarnings # 忽略所有警告，避免打包失败
-allowaccessmodification # 允许修改访问修饰符，提高混淆效果
-useuniqueclassmembernames # 生成唯一的混淆名
-verbose

-printconfiguration target/proguard-full-config.txt
-printusage target/proguard-deleted-classes.txt


# ==============================================
# 保留 org.apache 包下所有内容（核心配置）
# ==============================================
# 保留所有类
-keep class org.apache.** { *; }
# 保留所有接口
-keep interface org.apache.** { *; }
# 保留所有枚举
-keep enum org.apache.** { *; }
# 保留所有注解
-keep @interface org.apache.** { *; }


# ==============================================
# 保留 com.alibaba 包下所有内容（核心配置）
# ==============================================
# 保留所有类
-keep class com.alibaba.** { *; }
# 保留所有接口
-keep interface com.alibaba.** { *; }
# 保留所有枚举
-keep enum com.alibaba.** { *; }
# 保留所有注解
-keep @interface com.alibaba.** { *; }

# ==============================================
# 保留 org.projectlombok 包下所有内容（核心配置）
# ==============================================
# 保留所有类
-keep class org.projectlombok.** { *; }
# 保留所有接口
-keep interface org.projectlombok.** { *; }
# 保留所有枚举
-keep enum org.projectlombok.** { *; }
# 保留所有注解
-keep @interface org.projectlombok.** { *; }

# ==============================================
# 保留 sun.util 包下所有内容（核心配置）
# ==============================================
# 保留所有类
#-keep class sun.util.** { *; }
## 保留所有接口
#-keep interface sun.util.** { *; }
## 保留所有枚举
#-keep enum sun.util.** { *; }
## 保留所有注解
#-keep @interface sun.util.** { *; }


# ==============================================
# 保留必要的元信息（Apache 库必须）
# ==============================================
# 保留所有注解信息
-keepattributes *Annotation*
# 保留泛型签名信息（Jackson、Log4j2 等必须）
-keepattributes Signature
# 保留异常信息
-keepattributes Exceptions
# 保留内部类信息
-keepattributes InnerClasses

# ==============================================
# 忽略 Apache 库产生的所有警告
# ==============================================
-dontwarn org.apache.**
-dontwarn com.alibaba.**
#-dontwarn sun.util.**

# 保留序列化类
#-keep class * implements java.io.Serializable { *; }
# 保留枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}



# ==============================================
# 开启完整变量名混淆（核心配置）
# ==============================================

# 1. 强制混淆所有成员变量（字段）
# 注意：如果你的-keep指令包含了{ *; }，会保留所有字段，需要调整
-keepclassmembers class * {
    #!private <fields>; # 只保留非私有字段（可选，根据需要调整）
    # 完全不保留任何字段，全部混淆：
     <fields>;
}

# 2. 开启局部变量名混淆（最关键，默认关闭）
# 移除局部变量表和局部变量类型表属性，这两个属性保存了原始变量名
-keepattributes !LocalVariableTable,!LocalVariableTypeTable

# 3. 隐藏源文件名和行号（可选，进一步增加逆向难度）
-renamesourcefileattribute "SourceFile"
-keepattributes !LineNumberTable

# 开启Unicode混淆
-obfuscationdictionary unicode-dict.txt
-classobfuscationdictionary unicode-dict.txt
-packageobfuscationdictionary unicode-dict.txt



# 保留单个类
-keep class org.chen.App { *; }


