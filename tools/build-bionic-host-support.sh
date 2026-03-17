#!/data/data/com.termux/files/usr/bin/sh
set -eu

ROOT="/data/data/com.termux/files/home/aesolator"
TERMUX_PREFIX="/data/data/com.termux/files/usr"
VULKAN_PREFIX="/data/data/com.termux/files/home/.tmp_vulkan/prefix-1.4.341.0"
STAGE="$ROOT/.tmp_bionic_host_build/stage/bionic_host_support"
OUT="$ROOT/app/src/main/assets/bionic_host_support.tzst"
LIBDIR="$STAGE/usr/lib/android-host"

mkdir -p "$LIBDIR"
rm -f "$OUT"
find "$LIBDIR" -mindepth 1 -maxdepth 1 -exec rm -rf {} +

copy_glob() {
    base="$1"
    pattern="$2"
    found=0
    for src in "$base"/$pattern; do
        [ -e "$src" ] || continue
        cp -a "$src" "$LIBDIR/"
        found=1
    done
    [ "$found" -eq 1 ] || {
        echo "missing required host-support pattern: $base/$pattern" >&2
        exit 1
    }
}

patch_rpath_tree() {
    find "$LIBDIR" -maxdepth 1 -type f | while read -r file; do
        readelf -h "$file" >/dev/null 2>&1 || continue
        patchelf --set-rpath '$ORIGIN' "$file"
    done
}

copy_glob "$TERMUX_PREFIX/lib" "libandroid-support.so*"
copy_glob "$TERMUX_PREFIX/lib" "libX*.so*"
copy_glob "$TERMUX_PREFIX/lib" "libxcb*.so*"
copy_glob "$TERMUX_PREFIX/lib" "libxshmfence.so*"
copy_glob "$TERMUX_PREFIX/lib" "libexpat.so*"
copy_glob "$TERMUX_PREFIX/lib" "libfontconfig.so*"
copy_glob "$TERMUX_PREFIX/lib" "libfreetype.so*"
copy_glob "$TERMUX_PREFIX/lib" "libz.so*"
copy_glob "$TERMUX_PREFIX/lib" "libbz2.so*"
copy_glob "$TERMUX_PREFIX/lib" "libpng16.so*"
copy_glob "$TERMUX_PREFIX/lib" "libbrotlicommon.so*"
copy_glob "$TERMUX_PREFIX/lib" "libbrotlidec.so*"
copy_glob "$VULKAN_PREFIX/lib" "libvulkan.so*"

patch_rpath_tree

tar -C "$STAGE" -cf - usr | zstd -19 -T0 -o "$OUT"
chmod 0644 "$OUT"
ls -lh "$OUT"
