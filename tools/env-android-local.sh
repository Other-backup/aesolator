#!/data/data/com.termux/files/usr/bin/sh

export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
export ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk
export ANDROID_HOME=/data/data/com.termux/files/home/android-sdk
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:/data/data/com.termux/files/usr/bin:$PATH"
export AEO_AAPT2_OVERRIDE=/data/data/com.termux/files/usr/bin/aapt2
export GRADLE_OPTS="${GRADLE_OPTS:+$GRADLE_OPTS }-Dorg.gradle.project.android.aapt2FromMavenOverride=$AEO_AAPT2_OVERRIDE"

echo "JAVA_HOME=$JAVA_HOME"
echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
echo "AEO_AAPT2_OVERRIDE=$AEO_AAPT2_OVERRIDE"
echo "GRADLE_OPTS=$GRADLE_OPTS"
