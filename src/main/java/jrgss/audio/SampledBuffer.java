package jrgss.audio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import javazoom.spi.vorbis.sampled.file.VorbisEncoding;
import javazoom.spi.vorbis.sampled.file.VorbisFileFormatType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class SampledBuffer implements AudioBuffer {
    @ToString.Exclude
    private final byte[] data;
    public final AudioFormat format;
    public final int frameLength;
    public final int loopStartFrame;
    public final int loopEndFrame; // (inclusive)
    private final @Getter File file;

    public SampledBuffer(AudioInputStream audioStream, File file) throws IOException {
        int frameSize = audioStream.getFormat().getFrameSize();
        if (frameSize == AudioSystem.NOT_SPECIFIED)
            throw new IOException("unknown frame size");

        if (audioStream.getFrameLength() == AudioSystem.NOT_SPECIFIED) {
            this.data = audioStream.readAllBytes();
            this.frameLength = data.length / frameSize;
        } else {
            this.frameLength = Math.toIntExact(audioStream.getFrameLength());
            this.data = audioStream.readNBytes(Math.multiplyExact(frameLength, frameSize));
        }

        this.format = audioStream.getFormat();
        this.loopStartFrame = 0;
        this.loopEndFrame = this.frameLength - 1;
        this.file = file;
    }

    public SampledBuffer withLoop(int loopStartFrame, int loopEndFrame) {
        if (loopStartFrame < 0 || loopStartFrame > loopEndFrame || loopEndFrame > frameLength)
            throw new IllegalArgumentException("invalid loop points: " + loopStartFrame + ", " + loopEndFrame);
        return new SampledBuffer(data, format, frameLength, loopStartFrame, loopEndFrame, file);
    }

    public AudioFormat getFormat(int pitch) {
        if (pitch == 100) return format;
        float newSampleRate = format.getSampleRate() * pitch / 100f;
        float newFrameRate = format.getFrameRate() * pitch / 100f;
        return new AudioFormat(format.getEncoding(), newSampleRate, format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), newFrameRate, format.isBigEndian());
    }

    public int millisToFrames(int millis) {
        return (int) Math.ceil(format.getFrameRate() * millis / 1000f);
    }

    public int millisToBytes(int millis) {
        return framesToBytes(millisToFrames(millis));
    }

    public int framesToBytes(int frames) {
        return frames * format.getFrameSize();
    }

    public int bytesToFrames(int bytes) {
        return bytes / format.getFrameSize();
    }

    public int writeSamples(SourceDataLine dest, int frameOffset, int frameLength) {
        return bytesToFrames(dest.write(data, framesToBytes(frameOffset), framesToBytes(frameLength)));
    }

    @Override
    public SampledSource openSource(int pitch, int pos, boolean looping) throws LineUnavailableException {
        return new SampledSource(this, pitch, pos, looping);
    }

    @Override
    public int getMemorySize() {
        return data.length;
    }

    public static SampledBuffer read(InputStream stream, File file) throws IOException, UnsupportedAudioFileException {
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(stream);
        SampledBuffer buffer = new SampledBuffer(getAudioInputStream(stream, fileFormat), file);
        return extractLoopPoints(buffer, fileFormat);
    }

    private static AudioInputStream getAudioInputStream(InputStream stream, AudioFileFormat fileFormat) throws IOException {
        AudioInputStream audioStream = new AudioInputStream(stream, fileFormat.getFormat(), fileFormat.getFrameLength());

        AudioFormat format = audioStream.getFormat();
        if (format.getEncoding().equals(VorbisEncoding.VORBISENC)) {
            format = new AudioFormat(format.getSampleRate(), 16, format.getChannels(), true, false);
            audioStream = AudioSystem.getAudioInputStream(format, audioStream);
        }

        return audioStream;
    }

    private static SampledBuffer extractLoopPoints(SampledBuffer buffer, AudioFileFormat fileFormat) {
        if (fileFormat.getType().equals(VorbisFileFormatType.OGG)) {
            int loopStart = -1;
            int loopLength = -1;

            for (Map.Entry<String, Object> prop : fileFormat.properties().entrySet()) {
                if (prop.getKey().startsWith("ogg.comment.ext.")) {
                    String comment = prop.getValue().toString();
                    String[] pair = comment.split("=", 2);
                    if (pair[0].equalsIgnoreCase("loopstart")) {
                        loopStart = Integer.parseUnsignedInt(pair[1]);
                    } else if (pair[0].equalsIgnoreCase("looplength")) {
                        loopLength = Integer.parseUnsignedInt(pair[1]);
                    }
                }
            }

            if (loopStart > 0 && loopLength > 0)
                buffer = buffer.withLoop(loopStart, loopStart + loopLength - 1);
        }

        return buffer;
    }
}
