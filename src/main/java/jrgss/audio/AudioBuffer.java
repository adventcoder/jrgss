package jrgss.audio;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioBuffer {
    private static final int UNKNOWN_INITIAL_FRAME_LENGTH = 4096;

    public final byte[] data;
    public final AudioFormat format;
    public final int frameLength;
    public int loopStart;
    public int loopEnd; // (exclusive)

    private AudioBuffer(byte[] data, AudioFormat format, int frameLength) {
        this.data = data;
        this.format = format;
        this.frameLength = frameLength;
        this.loopStart = 0;
        this.loopEnd = frameLength;
    }

    public void setLoopStart(int frame) {
        if (frame < 0 || frame >= loopEnd)
            throw new IllegalArgumentException("invalid loop start");
        this.loopStart = frame;
    }

    public void setLoopEnd(int frame) {
        if (frame <= loopStart || frame > frameLength)
            throw new IllegalArgumentException("invalid loop start");
        this.loopEnd = frame;
    }

    public void setLoopLength(int frames) {
        if (frames <= 0 || loopStart + frames >= frameLength)
            throw new IllegalArgumentException("invalid loop length");
        this.loopEnd = loopStart + frames;
    }

    public static AudioBuffer read(File file, Mixer mixer, Class<? extends DataLine> sourceLineClass) throws UnsupportedAudioFileException, IOException {
        AudioInputStream stream = AudioSystem.getAudioInputStream(file);
        try {
            stream = AudioConverter.convert(stream, mixer, sourceLineClass);
            return read(stream);
        } finally {
            stream.close();
        }
    }

    public static AudioBuffer read(AudioInputStream stream) throws UnsupportedAudioFileException, IOException {
        AudioFormat format = stream.getFormat();

        if (format.getFrameSize() == AudioSystem.NOT_SPECIFIED)
            throw new UnsupportedAudioFileException("unknown frame size");
        int frameSize = format.getFrameSize();

        byte[] data;
        int pos = 0;

        int frameLength = Math.toIntExact(stream.getFrameLength());
        if (frameLength == AudioSystem.NOT_SPECIFIED) {
            data = new byte[UNKNOWN_INITIAL_FRAME_LENGTH * frameSize];
            while (true) {
                if (pos == data.length)
                    data = Arrays.copyOf(data, data.length * 2);
                int bytesRead = stream.read(data, pos, data.length - pos);
                if (bytesRead < 0) break;
                pos += bytesRead;
            }
        } else {
            data = new byte[Math.multiplyExact(frameLength, frameSize)];
            while (pos < data.length) {
                int bytesRead = stream.read(data, pos, data.length - pos);
                if (bytesRead < 0) break;
                pos += bytesRead;
            }
        }

        return new AudioBuffer(data, format, pos / frameSize);
    }    
}
