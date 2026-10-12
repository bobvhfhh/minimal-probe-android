#!/bin/bash
# 极简探针 Android App - 手动构建脚本
# 
# 背景：本环境禁止 Java 的本地 TCP（Gradle daemon 无法通信），
# 因此用 kotlinc + aapt2 + d8 + apksigner 手工完成构建。
# 在正常环境中直接用：./gradlew assembleDebug
#
set -e

export JAVA_HOME=~/workspace/jdk-17
export PATH=$JAVA_HOME/bin:$PATH
W=~/workspace
BT=$W/android-sdk/build-tools/36.0.0
APP=$W/minimal-probe-android/app/src/main
OUT=/tmp/apk-build

echo "=== 1. 资源编译 (aapt2) ==="
rm -rf $OUT && mkdir -p $OUT/gen $OUT/dex
$BT/aapt2 compile --dir $APP/res -o $OUT/res.zip

echo "=== 2. 生成 R.java ==="
sed 's|<manifest xmlns:android="http://schemas.android.com/apk/res/android"|<manifest xmlns:android="http://schemas.android.com/apk/res/android"\n    package="com.minprobe.app"|' \
  $APP/AndroidManifest.xml > $OUT/manifest-tmp.xml
$BT/aapt2 link -o $OUT/unsigned.apk \
  -I $W/android-sdk/platforms/android-36/android.jar \
  --manifest $OUT/manifest-tmp.xml \
  --java $OUT/gen \
  --min-sdk-version 28 --target-sdk-version 36 \
  --version-code 1 --version-name "1.0.0" \
  $OUT/res.zip

echo "=== 3. Kotlin 编译 ==="
CP=$(ls $W/deps/cp-dedup/*.jar | tr '\n' ':')$W/android-sdk/platforms/android-36/android.jar
SRCS=$(find $APP/java -name "*.kt" | tr '\n' ' ')
mkdir -p $OUT/kt-out
$W/kotlinc/bin/kotlinc \
  -cp "$CP" \
  -Xplugin=$W/kotlinc/lib/kotlin-serialization-compiler-plugin.jar \
  -Xplugin=$W/kotlinc/lib/compose-compiler-plugin.jar \
  -jvm-target 17 \
  -d $OUT/kt-out \
  $SRCS $OUT/gen/com/minprobe/app/R.java /tmp/stubs/com/minprobe/app/BuildConfig.java

echo "=== 4. D8 dex ==="
cd $OUT/kt-out && $JAVA_HOME/bin/jar cf $OUT/app.jar .
$BT/d8 --lib $W/android-sdk/platforms/android-36/android.jar \
  --min-api 28 --output $OUT/dex/ \
  $OUT/app.jar $W/dexwork/deduped/*.jar

echo "=== 5. 打包 + 对齐 + 签名 ==="
cd $OUT/dex && zip -q $OUT/unsigned.apk classes*.dex
$BT/zipalign -f 4 $OUT/unsigned.apk $OUT/aligned.apk
if [ ! -f /tmp/debug.keystore ]; then
  $JAVA_HOME/bin/keytool -genkeypair -keystore /tmp/debug.keystore \
    -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10950 \
    -storepass android -keypass android \
    -dname "CN=Android Debug,O=Android,C=US"
fi
$BT/apksigner sign --ks /tmp/debug.keystore \
  --ks-pass pass:android --key-pass pass:android \
  --out $OUT/minimal-probe-debug.apk $OUT/aligned.apk
$BT/apksigner verify $OUT/minimal-probe-debug.apk

echo ""
echo "=== 构建成功 ==="
ls -lh $OUT/minimal-probe-debug.apk
