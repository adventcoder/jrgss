package jrgss.audio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;

public interface SampleReader {
    public static final int DEFAULT_BUFFER_FRAME_LENGTH = 4096;

    public int readNFrames(short[][] samples, int frameOffset, int maxFrames) throws IOException;

    public static boolean supports(AudioFormat format) {
        AudioFormat.Encoding encoding = format.getEncoding();
        if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            return format.getSampleSizeInBits() == 8 || format.getSampleSizeInBits() == 16 || format.getSampleSizeInBits() == 24 || format.getSampleSizeInBits() == 32;
        } else if (encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
            return format.getSampleSizeInBits() == 8 || format.getSampleSizeInBits() == 16;
        } else if (encoding.equals(AudioFormat.Encoding.PCM_FLOAT)) {
            return format.getSampleSizeInBits() == 32;
        } else {
            return false;
        }
    }

    public static SampleReader getReader(InputStream stream, AudioFormat format) {
        return getReader(stream, format, DEFAULT_BUFFER_FRAME_LENGTH);
    }

    public static SampleReader getReader(InputStream stream, AudioFormat format, int bufferFrameLength) {
        AudioFormat.Encoding encoding = format.getEncoding();
        if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            if (format.getSampleSizeInBits() == 8)
                return new PcmSigned8Reader(stream, format.getChannels(), format.isBigEndian(), bufferFrameLength);
            if (format.getSampleSizeInBits() == 16)
                return new PcmSigned16Reader(stream, format.getChannels(), format.isBigEndian(), bufferFrameLength);
            if (format.getSampleSizeInBits() == 24)
                return new PcmSigned24Reader(stream, format.getChannels(), format.isBigEndian(), bufferFrameLength);
            if (format.getSampleSizeInBits() == 32)
                return new PcmSigned32Reader(stream, format.getChannels(), format.isBigEndian(), bufferFrameLength);
        } else if (encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
            if (format.getSampleSizeInBits() == 8)
                return new PcmUnsigned8Reader(stream, format.getChannels(), format.isBigEndian(), bufferFrameLength);
            if (format.getSampleSizeInBits() == 16)
                return new PcmUnsigned16Reader(stream, format.getChannels(), format.isBigEndian(), bufferFrameLength);
        } else if (encoding.equals(AudioFormat.Encoding.PCM_FLOAT)) {
            if (format.getSampleSizeInBits() == 32)
                return new PcmFloat32Reader(stream, format.getChannels(), format.isBigEndian(), bufferFrameLength);
        }
        throw new UnsupportedOperationException();
    }

    public static abstract class AbstractPcmReader implements SampleReader {
        private final InputStream stream;
        private final int channels;
        private final int frameSize;
        protected final ByteBuffer buffer;

        public AbstractPcmReader(InputStream stream, int sampleSize, int channels, boolean bigEndian, int bufferFrameLength) {
            this.stream = stream;
            this.channels = channels;
            this.frameSize = sampleSize * channels;
            buffer = ByteBuffer.allocate(bufferFrameLength * frameSize);
            buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        }

        public int readNFrames(short[][] samples, int frameOffset, int maxFrames) throws IOException {
            int frames = 0;
            while (frames < maxFrames) {
                int bytesRead = stream.read(buffer.array(), buffer.position(), Math.min((maxFrames - frames) * frameSize, buffer.remaining()));
                if (bytesRead < 0) break;
                if (bytesRead == 0) continue;
                buffer.position(buffer.position() + bytesRead);

                buffer.flip();
                while (buffer.remaining() >= frameSize) {
                    for (int ch = 0; ch < channels; ch++)
                        samples[ch][frameOffset] = getSample();
                    frameOffset++;
                    frames++;
                }

                buffer.compact();
            }

            return frames;
        }

        protected abstract short getSample();
    }

    public static class PcmSigned8Reader extends AbstractPcmReader {
        public PcmSigned8Reader(InputStream stream, int channels, boolean bigEndian, int bufferFrameLength) {
            super(stream, 1, channels, bigEndian, bufferFrameLength);
        }

        @Override
        protected short getSample() {
            return (short) (buffer.get() << 8);
        }
    }

    public static class PcmSigned16Reader extends AbstractPcmReader {
        public PcmSigned16Reader(InputStream stream, int channels, boolean bigEndian, int bufferFrameLength) {
            super(stream, 2, channels, bigEndian, bufferFrameLength);
        }

        @Override
        protected short getSample() {
            return buffer.getShort();
        }
    }

    public static class PcmSigned24Reader extends AbstractPcmReader {
        public PcmSigned24Reader(InputStream stream, int channels, boolean bigEndian, int bufferFrameLength) {
            super(stream, 3, channels, bigEndian, bufferFrameLength);
        }

        @Override
        protected short getSample() {
            int b0 = buffer.get() & 0xFF;
            int b1 = buffer.get() & 0xFF;
            int b2 = buffer.get() & 0xFF;

            int sample;
            if (buffer.order() == ByteOrder.BIG_ENDIAN) {
                sample = (b0 << 16) | (b1 << 8) | b2;
            } else {
                sample = (b2 << 16) | (b1 << 8) | b0;
            }

            // 24 bit sign extend
            if ((sample & 0x800000) != 0)
                sample |= 0xFF000000;

            return (short) (sample >> 8);
        }
    }

    public static class PcmSigned32Reader extends AbstractPcmReader {
        public PcmSigned32Reader(InputStream stream, int channels, boolean bigEndian, int bufferFrameLength) {
            super(stream, 4, channels, bigEndian, bufferFrameLength);
        }

        @Override
        protected short getSample() {
            return (short) (buffer.getInt() >> 16);
        }
    }

    public static class PcmUnsigned8Reader extends AbstractPcmReader {
        public PcmUnsigned8Reader(InputStream stream, int channels, boolean bigEndian, int bufferFrameLength) {
            super(stream, 1, channels, bigEndian, bufferFrameLength);
        }

        @Override
        protected short getSample() {
            int sample = (buffer.get() & 0xFF) - 0x80;
            return (short) (sample << 8);
        }
    }

    public static class PcmUnsigned16Reader extends AbstractPcmReader {
        public PcmUnsigned16Reader(InputStream stream, int channels, boolean bigEndian, int bufferFrameLength) {
            super(stream, 2, channels, bigEndian, bufferFrameLength);
        }

        @Override
        protected short getSample() {
            return (short) ((buffer.getShort() & 0xFFFF) - 0x8000);
        }
    }

    public static class PcmFloat32Reader extends AbstractPcmReader {
        public PcmFloat32Reader(InputStream stream, int channels, boolean bigEndian, int bufferFrameLength) {
            super(stream, 4, channels, bigEndian, bufferFrameLength);
        }

        @Override
        protected short getSample() {
            float sample = buffer.getFloat();
            if (sample >= 1f) return Short.MAX_VALUE;
            if (sample <= -1f) return Short.MIN_VALUE;
            return (short) (sample * 32767f);
        }
    }
}