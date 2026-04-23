package com.winlator.cmod.graphics;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public final class GraphicsElfCompatibility {
    private static final byte[] ELF_MAGIC = new byte[]{0x7f, 'E', 'L', 'F'};
    private static final String[] FORBIDDEN_BIONIC_TOKENS = {
            "libc.so.6",
            "ld-linux-"
    };
    private static final int BUFFER_SIZE = 8192;

    private GraphicsElfCompatibility() {
    }

    public static boolean isBionicCompatibleLibrary(File libraryFile) {
        return libraryFile != null
                && libraryFile.isFile()
                && isElf(libraryFile)
                && !containsAny(libraryFile, FORBIDDEN_BIONIC_TOKENS);
    }

    public static boolean hasForbiddenBionicToken(File libraryFile) {
        return libraryFile != null
                && libraryFile.isFile()
                && isElf(libraryFile)
                && containsAny(libraryFile, FORBIDDEN_BIONIC_TOKENS);
    }

    private static boolean isElf(File file) {
        byte[] header = new byte[ELF_MAGIC.length];
        try (FileInputStream inputStream = new FileInputStream(file)) {
            if (inputStream.read(header) != ELF_MAGIC.length) return false;
        } catch (IOException e) {
            return false;
        }

        for (int i = 0; i < ELF_MAGIC.length; i++) {
            if (header[i] != ELF_MAGIC[i]) return false;
        }
        return true;
    }

    private static boolean containsAny(File file, String[] tokens) {
        int maxTokenLength = maxTokenLength(tokens);
        if (maxTokenLength == 0) return false;

        byte[] buffer = new byte[BUFFER_SIZE + maxTokenLength];
        int carry = 0;
        try (FileInputStream inputStream = new FileInputStream(file)) {
            int read;
            while ((read = inputStream.read(buffer, carry, BUFFER_SIZE)) > 0) {
                int length = carry + read;
                for (String token : tokens) {
                    if (!token.isEmpty() && containsAscii(buffer, length, token)) return true;
                }
                carry = Math.min(maxTokenLength - 1, length);
                if (carry > 0) {
                    System.arraycopy(buffer, length - carry, buffer, 0, carry);
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    private static boolean containsAscii(byte[] buffer, int length, String token) {
        int tokenLength = token.length();
        if (tokenLength == 0 || length < tokenLength) return false;
        for (int i = 0; i <= length - tokenLength; i++) {
            int j = 0;
            while (j < tokenLength && buffer[i + j] == (byte) token.charAt(j)) {
                j++;
            }
            if (j == tokenLength) return true;
        }
        return false;
    }

    private static int maxTokenLength(String[] tokens) {
        int max = 0;
        if (tokens == null) return 0;
        for (String token : tokens) {
            if (token != null && token.length() > max) max = token.length();
        }
        return max;
    }
}
