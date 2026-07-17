package jrgss.audio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
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
import lombok.ToString;

@ToString
public class SampleBuffer implements Buffer {
    public static final int UNKNOWN_FRAME_CAPACITY = 4096;

    @ToString.Exclude
    private final byte[] data;
    private final AudioFormat format;
    public final int frameLength;
    public int loopStartFrame;
    public int loopEndFrame; // exclusive

    public SampleBuffer(byte[] data, AudioFormat format, int frameLength) {
        this.data = data;
        this.format = format;
        this.frameLength = frameLength;
        this.loopStartFrame = 0;
        this.loopEndFrame = frameLength;
    }

    public AudioFormat getFormat(float pitch) {
        if (pitch == 1f) return format;
        return new AudioFormat(format.getEncoding(), format.getSampleRate()*pitch, format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), format.getFrameRate()*pitch, format.isBigEndian());
    }

    public int secondsToFrames(float time) {
        return (int) Math.ceil(format.getFrameRate() * time);
    }

    public int secondsToBytes(float time) {
        return framesToBytes(secondsToFrames(time));
    }

    public int framesToBytes(int frames) {
        return frames * format.getFrameSize();
    }

    public int bytesToFrames(int bytes) {
        return bytes / format.getFrameSize();
    }

    public int writeSamples(SourceDataLine dest, int frameOffset, int frameLength) {
        return dest.write(data, framesToBytes(frameOffset), framesToBytes(frameLength));
    }

    @Override
    public SampleSource openSource(float pitch, boolean looping) throws LineUnavailableException {
        return new SampleSource(this, pitch, looping);
    }

    public static SampleBuffer read(File file) throws IOException, UnsupportedAudioFileException {
        try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
            return read(stream);
        }
    }

    public static SampleBuffer read(InputStream rawStream) throws IOException, UnsupportedAudioFileException {
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(rawStream);

        AudioInputStream stream = new AudioInputStream(rawStream, fileFormat.getFormat(), fileFormat.getFrameLength());
        if (stream.getFormat().getEncoding().equals(VorbisEncoding.VORBISENC))
            stream = AudioSystem.getAudioInputStream(decodedVorbisFormat(stream.getFormat()), stream);

        SampleBuffer buffer = readData(stream);
        extractLoopPoints(fileFormat, buffer);
        return buffer;
    }

    private static AudioFormat decodedVorbisFormat(AudioFormat format) {
        return new AudioFormat(format.getSampleRate(), 16, format.getChannels(), true, false);
    }

    private static SampleBuffer readData(AudioInputStream stream) throws IOException {
        AudioFormat format = stream.getFormat();
        int frameSize = format.getFrameSize();

        int frameCapacity = stream.getFrameLength() == AudioSystem.NOT_SPECIFIED ? UNKNOWN_FRAME_CAPACITY : Math.toIntExact(stream.getFrameLength());
        byte[] data = new byte[Math.multiplyExact(frameCapacity, frameSize)];

        int byteLength = 0;
        while (true) {
            if (byteLength == data.length)
                data = Arrays.copyOf(data, Math.multiplyExact(data.length, 2));
            int bytesRead = stream.read(data, byteLength, data.length - byteLength);
            if (bytesRead < 0) break;
            byteLength += bytesRead;
        }

        return new SampleBuffer(data, format, byteLength / frameSize);
    }

    private static void extractLoopPoints(AudioFileFormat fileFormat, SampleBuffer buffer) {
        if (fileFormat.getType().equals(VorbisFileFormatType.OGG)) {
            int loopStart = -1;
            int loopLength = -1;
            int loopEnd = -1;

            for (Map.Entry<String, Object> prop : fileFormat.properties().entrySet()) {
                if (prop.getKey().startsWith("ogg.comment.ext.")) {
                    String comment = prop.getValue().toString();
                    String[] pair = comment.split("=", 2);
                    if (pair[0].equalsIgnoreCase("loopstart")) {
                        loopStart = Integer.parseUnsignedInt(pair[1]);
                    } else if (pair[0].equalsIgnoreCase("looplength")) {
                        loopLength = Integer.parseUnsignedInt(pair[1]);
                    } else if (pair[0].equalsIgnoreCase("loopend")) {
                        loopEnd = Integer.parseUnsignedInt(pair[1]);
                    }
                }
            }

            if (loopStart < 0)
                loopStart = 0;
            if (loopEnd < 0 && loopLength >= 0)
                loopEnd = Math.addExact(loopStart, loopLength);
            if (loopEnd < 0)
                loopEnd = buffer.frameLength;

            if (loopStart >= 0 && loopStart < loopEnd && loopEnd <= buffer.frameLength) {
                buffer.loopStartFrame = loopStart;
                buffer.loopEndFrame = loopEnd;
            }
        }
    }

    public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
        SampleBuffer buffer = SampleBuffer.read(new File("ShipTest.ogg"));
        System.out.println(buffer);
    }
}
