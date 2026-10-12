# MiniProbe ProGuard 规则
#
# 注意：release 的 isMinifyEnabled 目前为 false，此文件仅作将来启用时准备。
# 若启用混淆，务必补充 Room / kotlinx.serialization / Ktor 的 keep 规则并回归测试。

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
