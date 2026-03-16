#!/data/data/com.termux/files/usr/bin/sh

export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
export ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk
export ANDROID_HOME=/data/data/com.termux/files/home/android-sdk
export AEO_HOST_LLVM_VERSION=22.1.1
export AEO_HOST_LLVM_ROOT=/data/data/com.termux/files/home/.toolchains/llvm-$AEO_HOST_LLVM_VERSION-termux
export PATH="/data/data/com.termux/files/usr/bin:$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"
export AEO_AAPT2_OVERRIDE=/data/data/com.termux/files/usr/bin/aapt2
export GRADLE_OPTS="${GRADLE_OPTS:+$GRADLE_OPTS }-Dorg.gradle.project.android.aapt2FromMavenOverride=$AEO_AAPT2_OVERRIDE"

if [ -x "$AEO_HOST_LLVM_ROOT/bin/clang" ]; then
  export PATH="$AEO_HOST_LLVM_ROOT/bin:$PATH"
  export CC="$AEO_HOST_LLVM_ROOT/bin/clang"
  export CXX="$AEO_HOST_LLVM_ROOT/bin/clang++"
  export AR="$AEO_HOST_LLVM_ROOT/bin/llvm-ar"
  export RANLIB="$AEO_HOST_LLVM_ROOT/bin/llvm-ranlib"
  export STRIP="$AEO_HOST_LLVM_ROOT/bin/llvm-strip"
  export LD="$AEO_HOST_LLVM_ROOT/bin/ld.lld"
  export NM="$AEO_HOST_LLVM_ROOT/bin/llvm-nm"
  export OBJCOPY="$AEO_HOST_LLVM_ROOT/bin/llvm-objcopy"
  export OBJDUMP="$AEO_HOST_LLVM_ROOT/bin/llvm-objdump"
  export READELF="$AEO_HOST_LLVM_ROOT/bin/llvm-readelf"
  export RC="$AEO_HOST_LLVM_ROOT/bin/llvm-rc"
  export WINDRES="$AEO_HOST_LLVM_ROOT/bin/llvm-windres"
  export DLLTOOL="$AEO_HOST_LLVM_ROOT/bin/llvm-dlltool"
fi

echo "JAVA_HOME=$JAVA_HOME"
echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
echo "AEO_HOST_LLVM_ROOT=$AEO_HOST_LLVM_ROOT"
echo "AEO_AAPT2_OVERRIDE=$AEO_AAPT2_OVERRIDE"
echo "GRADLE_OPTS=$GRADLE_OPTS"
