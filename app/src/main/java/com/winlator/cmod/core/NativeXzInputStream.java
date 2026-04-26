package com.winlator.cmod.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public final class NativeXzInputStream extends InputStream {
    private static final Object LOAD_LOCK = new Object();
    private static volatile Boolean nativeAvailable;

    private long handle;
    private final byte[] singleByte = new byte[1];

    public NativeXzInputStream(File source) throws IOException {
        Objects.requireNonNull(source, "source");
        if (!isAvailable()) {
            throw new IOException("Native XZ decoder is unavailable");
        }
        handle = nativeOpen(source.getAbsolutePath());
        if (handle == 0) {
            throw new IOException("Native XZ decoder failed to open source");
        }
    }

    public static boolean isAvailable() {
        Boolean cached = nativeAvailable;
        if (cached != null) return cached;
        synchronized (LOAD_LOCK) {
            cached = nativeAvailable;
            if (cached != null) return cached;
            try {
                NativeLibraryLoader.ensureLoaded("aero_native_xz", "native_xz_stream");
                nativeAvailable = true;
                return true;
            }
            catch (Throwable ignored) {
                nativeAvailable = false;
                return false;
            }
        }
    }

    @Override
    public int read() throws IOException {
        int count = read(singleByte, 0, 1);
        return count < 0 ? -1 : singleByte[0] & 0xff;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        Objects.requireNonNull(buffer, "buffer");
        if (offset < 0 || length < 0 || length > buffer.length - offset) {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0) return 0;
        if (handle == 0) throw new IOException("Native XZ stream is closed");
        return nativeRead(handle, buffer, offset, length);
    }

    @Override
    public void close() {
        long current = handle;
        handle = 0;
        if (current != 0) {
            nativeClose(current);
        }
    }

    private static native long nativeOpen(String path) throws IOException;

    private static native int nativeRead(long handle, byte[] output, int offset, int length) throws IOException;

    private static native void nativeClose(long handle);
}
