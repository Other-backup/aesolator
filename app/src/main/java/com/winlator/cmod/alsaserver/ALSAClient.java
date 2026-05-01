package com.winlator.cmod.alsaserver;

import com.winlator.cmod.core.WinlatorNative;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.sysvshm.SysVSharedMemory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ALSAClient {
    public enum DataType {
        U8(1), S16LE(2), S16BE(2), FLOATLE(4), FLOATBE(4);
        public final byte byteCount;

        DataType(int byteCount) {
            this.byteCount = (byte)byteCount;
        }
    }
    private DataType dataType = DataType.U8;
    private byte channelCount = 2;
    private int sampleRate = 0;
    private int position;
    private int bufferSize;
    private int frameBytes;
    private ByteBuffer sharedBuffer;
    private boolean playing = false;
    private long streamPtr = 0;
    private float[] bassLowpassState = new float[2];
    private float bassLowpassAlpha = 0.0f;
    private final Options options;

    public static class Options {
        private static final int DEFAULT_LATENCY_MILLIS = 16;
        private static final float DEFAULT_VOLUME = 1.0f;
        private static final float MAX_VOLUME = 16.0f;
        private static final float DEFAULT_BASS_BOOST = 0.0f;
        private static final float MAX_BASS_BOOST = 2.0f;
        private static final int PERFORMANCE_MODE_NONE = 0;
        private static final int PERFORMANCE_MODE_LOW_LATENCY = 1;
        private static final int PERFORMANCE_MODE_POWER_SAVING = 2;

        public int latencyMillis = DEFAULT_LATENCY_MILLIS;
        public int performanceMode = PERFORMANCE_MODE_LOW_LATENCY;
        public float volume = DEFAULT_VOLUME;
        public float bassBoost = DEFAULT_BASS_BOOST;

        public static Options fromEnvVars(EnvVars envVars) {
            Options options = new Options();
            if (envVars == null) return options;

            options.latencyMillis = parseInt(
                    firstNonEmpty(envVars.get("ANDROID_ALSA_LATENCY_MS"), envVars.get("WINNATIVE_ALSA_LATENCY_MS")),
                    DEFAULT_LATENCY_MILLIS
            );
            options.latencyMillis = Math.max(0, options.latencyMillis);

            options.volume = parseFloat(
                    firstNonEmpty(envVars.get("ANDROID_ALSA_VOLUME"), envVars.get("WINNATIVE_ALSA_VOLUME")),
                    DEFAULT_VOLUME
            );
            options.volume = Math.max(0.0f, Math.min(options.volume, MAX_VOLUME));

            options.bassBoost = parseFloat(
                    firstNonEmpty(envVars.get("ANDROID_ALSA_BASS_BOOST"), envVars.get("WINNATIVE_ALSA_BASS_BOOST")),
                    DEFAULT_BASS_BOOST
            );
            options.bassBoost = Math.max(0.0f, Math.min(options.bassBoost, MAX_BASS_BOOST));

            String mode = firstNonEmpty(
                    envVars.get("ANDROID_ALSA_PERFORMANCE_MODE"),
                    envVars.get("WINNATIVE_ALSA_PERFORMANCE_MODE")
            );
            if (mode.equalsIgnoreCase("none") || mode.equals("0")) {
                options.performanceMode = PERFORMANCE_MODE_NONE;
            } else if (mode.equalsIgnoreCase("power_saving") || mode.equals("2")) {
                options.performanceMode = PERFORMANCE_MODE_POWER_SAVING;
            } else {
                options.performanceMode = PERFORMANCE_MODE_LOW_LATENCY;
            }
            return options;
        }

        private static String firstNonEmpty(String first, String second) {
            return first != null && !first.isEmpty() ? first : (second != null ? second : "");
        }

        private static int parseInt(String value, int fallback) {
            try {
                if (value != null && !value.isEmpty()) return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
            }
            return fallback;
        }

        private static float parseFloat(String value, float fallback) {
            try {
                if (value != null && !value.isEmpty()) return Float.parseFloat(value);
            } catch (NumberFormatException ignored) {
            }
            return fallback;
        }
    }

    static {
        WinlatorNative.ensureLoaded("ALSAClient");
    }

    public ALSAClient() {
        this(new Options());
    }

    public ALSAClient(Options options) {
        this.options = options != null ? options : new Options();
    }

    public void release() {
        if (sharedBuffer != null) {
            SysVSharedMemory.unmapSHMSegment(sharedBuffer, sharedBuffer.capacity());
            sharedBuffer = null;
        }

        stop(streamPtr);
        close(streamPtr);
        playing = false;
        streamPtr = 0;
    }

    public void prepare() {
        position = 0;
        frameBytes = channelCount * dataType.byteCount;
        bassLowpassState = new float[Math.max(1, channelCount)];
        bassLowpassAlpha = computeBassLowpassAlpha(sampleRate);
        release();

        if (!isValidBufferSize()) return;

        streamPtr = create(dataType.ordinal(), channelCount, sampleRate, resolveStreamBufferSize(), options.performanceMode);
        if (streamPtr > 0) start();
    }

    public void start() {
        if (streamPtr > 0 && !playing) {
            start(streamPtr);
            playing = true;
        }
    }

    public void stop() {
        if (streamPtr > 0 && playing) {
            stop(streamPtr);
            playing = false;
        }
    }

    public void pause() {
        if (streamPtr > 0) {
            pause(streamPtr);
            playing = false;
        }
    }

    public void drain() {
        if (streamPtr > 0) flush(streamPtr);
    }

    public void writeDataToStream(ByteBuffer data) {
        if (dataType == DataType.S16LE || dataType == DataType.FLOATLE) {
            data.order(ByteOrder.LITTLE_ENDIAN);
        }
        else if (dataType == DataType.S16BE || dataType == DataType.FLOATBE) {
            data.order(ByteOrder.BIG_ENDIAN);
        }

        if (playing) {
            applyAudioProcessing(data);
            int numFrames = data.limit() / frameBytes;
            int framesWritten = write(streamPtr, data, numFrames);
            if (framesWritten > 0) position += framesWritten;
            data.rewind();
        }
    }

    private int resolveStreamBufferSize() {
        if (options.latencyMillis <= 0 || sampleRate <= 0) return bufferSize;
        int latencyFrames = Math.max(1, Math.round((sampleRate * options.latencyMillis) / 1000.0f));
        return Math.max(1, Math.min(bufferSize, latencyFrames));
    }

    private void applyAudioProcessing(ByteBuffer data) {
        if (options.volume == Options.DEFAULT_VOLUME && options.bassBoost == Options.DEFAULT_BASS_BOOST) {
            return;
        }
        ByteBuffer buffer = data.duplicate();
        buffer.order(data.order());
        int start = data.position();
        int end = data.limit();

        switch (dataType) {
            case U8:
                for (int i = start, sampleIndex = 0; i < end; i++, sampleIndex++) {
                    float sample = ((buffer.get(i) & 0xff) - 128) / 128.0f;
                    int scaledSample = Math.round(processSample(sample, sampleIndex) * 128.0f) + 128;
                    buffer.put(i, (byte)clamp(scaledSample, 0, 255));
                }
                break;
            case S16LE:
            case S16BE:
                for (int i = start, sampleIndex = 0; i + 1 < end; i += 2, sampleIndex++) {
                    float sample = buffer.getShort(i) / 32768.0f;
                    int scaledSample = Math.round(processSample(sample, sampleIndex) * 32768.0f);
                    buffer.putShort(i, (short)clamp(scaledSample, Short.MIN_VALUE, Short.MAX_VALUE));
                }
                break;
            case FLOATLE:
            case FLOATBE:
                for (int i = start, sampleIndex = 0; i + 3 < end; i += 4, sampleIndex++) {
                    buffer.putFloat(i, clamp(processSample(buffer.getFloat(i), sampleIndex), -1.0f, 1.0f));
                }
                break;
        }
    }

    private float processSample(float sample, int sampleIndex) {
        int channel = sampleIndex % Math.max(1, channelCount);
        if (options.bassBoost > Options.DEFAULT_BASS_BOOST && channel < bassLowpassState.length) {
            bassLowpassState[channel] += bassLowpassAlpha * (sample - bassLowpassState[channel]);
            sample += bassLowpassState[channel] * options.bassBoost;
        }
        return clamp(sample * options.volume, -1.0f, 1.0f);
    }

    private static float computeBassLowpassAlpha(int sampleRate) {
        if (sampleRate <= 0) return 0.0f;
        float cutoffHz = 180.0f;
        float dt = 1.0f / sampleRate;
        float rc = 1.0f / (2.0f * (float)Math.PI * cutoffHz);
        return dt / (rc + dt);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    public int pointer() {
        return position;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = (byte)channelCount;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public ByteBuffer getSharedBuffer() {
        return sharedBuffer;
    }

    public void setSharedBuffer(ByteBuffer sharedBuffer) {
        this.sharedBuffer = sharedBuffer;
    }

    public DataType getDataType() {
        return dataType;
    }

    public byte getChannelCount() {
        return channelCount;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getBufferSizeInBytes() {
        return bufferSize * frameBytes;
    }

    private boolean isValidBufferSize() {
        return (getBufferSizeInBytes() % frameBytes == 0) && bufferSize > 0;
    }

    public int computeLatencyMillis() {
        return (int)(((float)bufferSize / sampleRate) * 1000);
    }

    private native long create(int format, byte channelCount, int sampleRate, int bufferSize, int performanceMode);

    private native int write(long streamPtr, ByteBuffer buffer, int numFrames);

    private native void start(long streamPtr);

    private native void stop(long streamPtr);

    private native void pause(long streamPtr);

    private native void flush(long streamPtr);

    private native void close(long streamPtr);
}
