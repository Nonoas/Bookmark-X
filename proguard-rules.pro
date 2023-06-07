# 保留应用的入口点
-keepclassmembers public class com.example.MyApplication {
    public static void main(java.lang.String[]);
}

# 保留特定的类或方法
-keep public class com.example.MyClass
-keep public class com.example.MyClass {
    public void myMethod();
}

# 保留用于反射的类和方法
-keepclassmembers class * {
    *** *();
    *** *(***);
}

# 保留特定的类或接口，用于反射或序列化
-keepnames class com.example.MyClass
-keepnames interface com.example.MyInterface

# 保留特定的注解
-keepattributes *Annotation*
-keepattributes Signature

# 移除无用的类和方法
-assumenosideeffects class * {
    public void unusedMethod();
}

# 不混淆特定的类或方法
-keepnames class com.example.MyClass

# 重命名类和成员
-repackageclasses ''
-allowaccessmodification

# 优化选项
-optimizationpasses 5
-dontoptimize
-dontpreverify

# 其他混淆规则...