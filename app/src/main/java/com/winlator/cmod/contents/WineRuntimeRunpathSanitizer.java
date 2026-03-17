package com.winlator.cmod.contents;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class WineRuntimeRunpathSanitizer {
    private static final String TAG = "WineRunpathSanitizer";
    private static final byte[] ELF_MAGIC = new byte[]{0x7f, 'E', 'L', 'F'};
    private static final int EI_CLASS = 4;
    private static final int EI_DATA = 5;
    private static final int ELFCLASS32 = 1;
    private static final int ELFCLASS64 = 2;
    private static final int ELFDATA2LSB = 1;
    private static final int ELFDATA2MSB = 2;
    private static final long PT_LOAD = 1L;
    private static final long PT_DYNAMIC = 2L;
    private static final long DT_NULL = 0L;
    private static final long DT_STRTAB = 5L;
    private static final long DT_STRSZ = 10L;
    private static final long DT_RPATH = 15L;
    private static final long DT_RUNPATH = 29L;
    static final String ORIGIN_TOKEN = "$ORIGIN";

    private WineRuntimeRunpathSanitizer() {
    }

    public static Result sanitizeTree(@Nullable File runtimeRoot, @Nullable File imageFsLibDir) {
        Result result = new Result();
        if (runtimeRoot == null || imageFsLibDir == null || !runtimeRoot.exists() || !imageFsLibDir.isDirectory()) {
            return result;
        }

        ArrayList<File> files = new ArrayList<>();
        collectFiles(runtimeRoot, files);
        for (File file : files) {
            PatchOutcome outcome = sanitizeFile(file, imageFsLibDir);
            switch (outcome) {
                case NOT_ELF -> result.nonElfFiles++;
                case UNCHANGED -> result.elfFilesScanned++;
                case PATCHED -> {
                    result.elfFilesScanned++;
                    result.patchedFiles++;
                }
                case FAILED -> {
                    result.elfFilesScanned++;
                    result.failedFiles++;
                }
            }
        }
        return result;
    }

    public static boolean hasAbsolutePathSegment(@Nullable String runpath) {
        if (runpath == null || runpath.trim().isEmpty()) return false;
        String[] segments = runpath.split(":");
        for (String segment : segments) {
            String trimmed = segment == null ? "" : segment.trim();
            if (trimmed.startsWith("/")) return true;
        }
        return false;
    }

    @Nullable
    public static String buildSanitizedRunpath(@Nullable File elfFile, @Nullable File imageFsLibDir, int maxLength) {
        if (elfFile == null || imageFsLibDir == null || maxLength < ORIGIN_TOKEN.length()) return null;

        File parent = elfFile.getParentFile();
        if (parent == null) return ORIGIN_TOKEN;

        String relativeLibPath;
        try {
            relativeLibPath = parent.toPath()
                    .toAbsolutePath()
                    .normalize()
                    .relativize(imageFsLibDir.toPath().toAbsolutePath().normalize())
                    .toString()
                    .replace(File.separatorChar, '/');
        } catch (Exception ignored) {
            relativeLibPath = "";
        }

        if (!relativeLibPath.isEmpty() && !".".equals(relativeLibPath)) {
            String candidate = ORIGIN_TOKEN + ":" + ORIGIN_TOKEN + "/" + relativeLibPath;
            if (candidate.length() <= maxLength) {
                return candidate;
            }
        }
        return ORIGIN_TOKEN;
    }

    private static void collectFiles(File root, List<File> files) {
        if (root == null || !root.exists()) return;
        if (Files.isSymbolicLink(root.toPath())) return;
        if (root.isFile()) {
            files.add(root);
            return;
        }

        File[] children = root.listFiles();
        if (children == null) return;
        for (File child : children) {
            collectFiles(child, files);
        }
    }

    private static PatchOutcome sanitizeFile(File file, File imageFsLibDir) {
        if (file == null || !file.isFile()) return PatchOutcome.NOT_ELF;
        if (file.length() < 16L) return PatchOutcome.NOT_ELF;
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            ElfMetadata elf = parseElf(raf);
            if (elf == null) return PatchOutcome.NOT_ELF;

            boolean patched = false;
            Set<Long> visitedOffsets = new HashSet<>();
            for (Long stringIndex : elf.runpathStringIndexes) {
                if (stringIndex == null || stringIndex < 0 || stringIndex >= elf.stringTableSize) continue;
                long stringOffset = elf.stringTableFileOffset + stringIndex;
                if (!visitedOffsets.add(stringOffset)) continue;

                String current = readCString(raf, stringOffset, elf.stringTableFileOffset + elf.stringTableSize);
                if (!hasAbsolutePathSegment(current)) continue;

                String replacement = buildSanitizedRunpath(file, imageFsLibDir, current.length());
                if (replacement == null || replacement.equals(current)) continue;

                writeCString(raf, stringOffset, current.length(), replacement);
                patched = true;
                Log.i(TAG, String.format(Locale.US,
                        "Sanitized runtime RUNPATH: %s -> %s",
                        file.getAbsolutePath(),
                        replacement));
            }
            return patched ? PatchOutcome.PATCHED : PatchOutcome.UNCHANGED;
        } catch (Exception e) {
            Log.w(TAG, "Failed to sanitize runtime RUNPATH for " + file.getAbsolutePath(), e);
            return PatchOutcome.FAILED;
        }
    }

    @Nullable
    private static ElfMetadata parseElf(RandomAccessFile raf) throws IOException {
        try {
            byte[] ident = new byte[16];
            raf.seek(0L);
            raf.readFully(ident);
            if (ident[0] != ELF_MAGIC[0] || ident[1] != ELF_MAGIC[1] || ident[2] != ELF_MAGIC[2] || ident[3] != ELF_MAGIC[3]) {
                return null;
            }

            int elfClass = ident[EI_CLASS] & 0xff;
            if (elfClass != ELFCLASS32 && elfClass != ELFCLASS64) return null;

            int dataClass = ident[EI_DATA] & 0xff;
            ByteOrder byteOrder = switch (dataClass) {
                case ELFDATA2LSB -> ByteOrder.LITTLE_ENDIAN;
                case ELFDATA2MSB -> ByteOrder.BIG_ENDIAN;
                default -> null;
            };
            if (byteOrder == null) return null;

            long programHeaderOffset = elfClass == ELFCLASS64
                    ? readUnsignedLong(raf, 32L, byteOrder)
                    : readUnsignedInt(raf, 28L, byteOrder);
            int programHeaderEntrySize = readUnsignedShort(raf, elfClass == ELFCLASS64 ? 54L : 42L, byteOrder);
            int programHeaderCount = readUnsignedShort(raf, elfClass == ELFCLASS64 ? 56L : 44L, byteOrder);
            if (programHeaderOffset <= 0L || programHeaderEntrySize <= 0 || programHeaderCount <= 0) {
                return null;
            }

            ArrayList<ProgramHeader> loadSegments = new ArrayList<>();
            ProgramHeader dynamicSegment = null;
            for (int index = 0; index < programHeaderCount; index++) {
                long entryOffset = programHeaderOffset + (long) index * programHeaderEntrySize;
                ProgramHeader header = readProgramHeader(raf, entryOffset, elfClass, byteOrder);
                if (header == null) continue;
                if (header.type == PT_LOAD) {
                    loadSegments.add(header);
                } else if (header.type == PT_DYNAMIC) {
                    dynamicSegment = header;
                }
            }
            if (dynamicSegment == null || dynamicSegment.fileSize <= 0L) return null;

            long entrySize = elfClass == ELFCLASS64 ? 16L : 8L;
            long entries = dynamicSegment.fileSize / entrySize;
            long stringTableVirtualAddress = -1L;
            long stringTableSize = -1L;
            ArrayList<Long> runpathStringIndexes = new ArrayList<>();

            for (long entryIndex = 0; entryIndex < entries; entryIndex++) {
                long entryOffset = dynamicSegment.fileOffset + (entryIndex * entrySize);
                long tag = elfClass == ELFCLASS64
                        ? readSignedLong(raf, entryOffset, byteOrder)
                        : readSignedInt(raf, entryOffset, byteOrder);
                long value = elfClass == ELFCLASS64
                        ? readUnsignedLong(raf, entryOffset + 8L, byteOrder)
                        : readUnsignedInt(raf, entryOffset + 4L, byteOrder);

                if (tag == DT_NULL) break;
                if (tag == DT_STRTAB) {
                    stringTableVirtualAddress = value;
                } else if (tag == DT_STRSZ) {
                    stringTableSize = value;
                } else if (tag == DT_RUNPATH || tag == DT_RPATH) {
                    runpathStringIndexes.add(value);
                }
            }

            if (stringTableVirtualAddress < 0L || stringTableSize <= 0L || runpathStringIndexes.isEmpty()) {
                return null;
            }

            long stringTableFileOffset = virtualAddressToFileOffset(stringTableVirtualAddress, loadSegments);
            if (stringTableFileOffset < 0L) return null;

            return new ElfMetadata(stringTableFileOffset, stringTableSize, runpathStringIndexes);
        } catch (EOFException ignored) {
            return null;
        }
    }

    @Nullable
    private static ProgramHeader readProgramHeader(RandomAccessFile raf, long offset, int elfClass, ByteOrder byteOrder) throws IOException {
        if (elfClass == ELFCLASS64) {
            long type = readUnsignedInt(raf, offset, byteOrder);
            long fileOffset = readUnsignedLong(raf, offset + 8L, byteOrder);
            long virtualAddress = readUnsignedLong(raf, offset + 16L, byteOrder);
            long fileSize = readUnsignedLong(raf, offset + 32L, byteOrder);
            long memorySize = readUnsignedLong(raf, offset + 40L, byteOrder);
            return new ProgramHeader(type, fileOffset, virtualAddress, fileSize, memorySize);
        }

        long type = readUnsignedInt(raf, offset, byteOrder);
        long fileOffset = readUnsignedInt(raf, offset + 4L, byteOrder);
        long virtualAddress = readUnsignedInt(raf, offset + 8L, byteOrder);
        long fileSize = readUnsignedInt(raf, offset + 16L, byteOrder);
        long memorySize = readUnsignedInt(raf, offset + 20L, byteOrder);
        return new ProgramHeader(type, fileOffset, virtualAddress, fileSize, memorySize);
    }

    private static long virtualAddressToFileOffset(long virtualAddress, List<ProgramHeader> loadSegments) {
        for (ProgramHeader loadSegment : loadSegments) {
            if (virtualAddress < loadSegment.virtualAddress) continue;
            long upperBound = loadSegment.virtualAddress + loadSegment.memorySize;
            if (virtualAddress >= upperBound) continue;
            return loadSegment.fileOffset + (virtualAddress - loadSegment.virtualAddress);
        }
        return -1L;
    }

    private static String readCString(RandomAccessFile raf, long offset, long maxOffsetExclusive) throws IOException {
        if (offset < 0L || maxOffsetExclusive <= offset) return "";
        int maxLength = (int) Math.min(Integer.MAX_VALUE, maxOffsetExclusive - offset);
        byte[] data = new byte[maxLength];
        raf.seek(offset);
        raf.readFully(data);

        int length = 0;
        while (length < data.length && data[length] != 0) {
            length++;
        }
        return new String(data, 0, length, StandardCharsets.UTF_8);
    }

    private static void writeCString(RandomAccessFile raf, long offset, int previousLength, String replacement) throws IOException {
        byte[] replacementBytes = replacement.getBytes(StandardCharsets.UTF_8);
        if (replacementBytes.length > previousLength) {
            throw new IOException("Replacement RUNPATH exceeds available string-table slot");
        }

        byte[] buffer = new byte[previousLength + 1];
        System.arraycopy(replacementBytes, 0, buffer, 0, replacementBytes.length);
        raf.seek(offset);
        raf.write(buffer);
    }

    private static int readUnsignedShort(RandomAccessFile raf, long offset, ByteOrder byteOrder) throws IOException {
        return readBuffer(raf, offset, 2, byteOrder).getShort() & 0xffff;
    }

    private static long readUnsignedInt(RandomAccessFile raf, long offset, ByteOrder byteOrder) throws IOException {
        return readBuffer(raf, offset, 4, byteOrder).getInt() & 0xffffffffL;
    }

    private static long readSignedInt(RandomAccessFile raf, long offset, ByteOrder byteOrder) throws IOException {
        return readBuffer(raf, offset, 4, byteOrder).getInt();
    }

    private static long readUnsignedLong(RandomAccessFile raf, long offset, ByteOrder byteOrder) throws IOException {
        return readBuffer(raf, offset, 8, byteOrder).getLong();
    }

    private static long readSignedLong(RandomAccessFile raf, long offset, ByteOrder byteOrder) throws IOException {
        return readBuffer(raf, offset, 8, byteOrder).getLong();
    }

    private static ByteBuffer readBuffer(RandomAccessFile raf, long offset, int size, ByteOrder byteOrder) throws IOException {
        byte[] data = new byte[size];
        raf.seek(offset);
        raf.readFully(data);
        return ByteBuffer.wrap(data).order(byteOrder);
    }

    public static final class Result {
        public int nonElfFiles;
        public int elfFilesScanned;
        public int patchedFiles;
        public int failedFiles;

        public boolean hasSignal() {
            return patchedFiles > 0 || failedFiles > 0;
        }

        public String toSummary() {
            return String.format(Locale.US,
                    "elf_scanned=%d patched=%d failed=%d skipped_non_elf=%d",
                    elfFilesScanned,
                    patchedFiles,
                    failedFiles,
                    nonElfFiles);
        }
    }

    private enum PatchOutcome {
        NOT_ELF,
        UNCHANGED,
        PATCHED,
        FAILED
    }

    private static final class ProgramHeader {
        final long type;
        final long fileOffset;
        final long virtualAddress;
        final long fileSize;
        final long memorySize;

        ProgramHeader(long type, long fileOffset, long virtualAddress, long fileSize, long memorySize) {
            this.type = type;
            this.fileOffset = fileOffset;
            this.virtualAddress = virtualAddress;
            this.fileSize = fileSize;
            this.memorySize = memorySize;
        }
    }

    private static final class ElfMetadata {
        final long stringTableFileOffset;
        final long stringTableSize;
        final List<Long> runpathStringIndexes;

        ElfMetadata(long stringTableFileOffset, long stringTableSize, List<Long> runpathStringIndexes) {
            this.stringTableFileOffset = stringTableFileOffset;
            this.stringTableSize = stringTableSize;
            this.runpathStringIndexes = runpathStringIndexes;
        }
    }
}
