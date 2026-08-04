package jrgss.audio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public interface ShortSampleReader {
    public static final int DEFAULT_BUFFER_FRAME_LENGTH = 4096;

    public int readNFrames(short[][] samples, int frameOffset, int maxFrames) throws IOException;

    public static Collection<AudioFormat> supportedFormats() {
        List<AudioFormat> formats = new ArrayList<>();
        for (int sampleDepth : new int[] { 8, 16, 24, 32 }) {
            formats.add(new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, sampleDepth, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false));
            formats.add(new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, sampleDepth, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, true));
        }
        for (int sampleDepth : new int[] { 8, 16 }) {
            formats.add(new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, AudioSystem.NOT_SPECIFIED, sampleDepth, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false));
            formats.add(new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, AudioSystem.NOT_SPECIFIED, sampleDepth, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, true));
        }
        for (int sampleDepth : new int[] { 32 }) {
            formats.add(new AudioFormat(AudioFormat.Encoding.PCM_FLOAT, AudioSystem.NOT_SPECIFIED, sampleDepth, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false));
            formats.add(new AudioFormat(AudioFormat.Encoding.PCM_FLOAT, AudioSystem.NOT_SPECIFIED, sampleDepth, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, true));
        }
        return formats;
    }

    public static ShortSampleReader getReader(InputStream stream, AudioFormat format) throws UnsupportedAudioFileException {
        return getReader(stream, format, DEFAULT_BUFFER_FRAME_LENGTH);
    }

    public static ShortSampleReader getReader(InputStream stream, AudioFormat format, int bufferFrameLength) throws UnsupportedAudioFileException {
        if (format.getChannels() == AudioSystem.NOT_SPECIFIED)
            throw new UnsupportedAudioFileException();

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
        }
        if (encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
            if (format.getSampleSizeInBits() == 8)
                return new PcmUnsigned8Reader(stream, format.getChannels(), format.isBigEndian(), bufferFrameLength);
            if (format.getSampleSizeInBits() == 16)
                return new PcmUnsigned16Reader(stream, format.getChannels(), format.isBigEndian(), bufferFrameLength);
        }
        if (encoding.equals(AudioFormat.Encoding.PCM_FLOAT)) {
            if (format.getSampleSizeInBits() == 32)
                return new PcmFloat32Reader(stream, format.getChannels(), format.isBigEndian(), bufferFrameLength);
        }
        throw new UnsupportedAudioFileException();
    }

    public static abstract class AbstractPcmReader implements ShortSampleReader {
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
            if (sample < -1f) return Short.MIN_VALUE;
            if (sample >= 1f) return Short.MAX_VALUE;
            return (short) Math.floor(sample * 32768f);
        }
    }
}