package jrgss.audio;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class ShortAudioBuffer {
    private static final int UNKNOWN_INITIAL_FRAME_LENGTH = 4096;

    public final short[][] channels;
    public final int frameLength;
    public final float sampleRate;
    public int loopStart;
    public int loopEnd; // (exclusive)

    private ShortAudioBuffer(short[][] channels, int frameLength, float sampleRate) {
        this.channels = channels;
        this.frameLength = frameLength;
        this.sampleRate = sampleRate;
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

    //TODO: move this into AudioSource
    public void getFrame(double pos, float[] frame) {
        int x0 = (int) Math.floor(pos);
        assert x0 < frameLength;

        int x1 = x0 + 1;
        if (x1 >= frameLength) {
            for (int ch = 0; ch < channels.length; ch++) {
                float y0 = channels[ch][x0] / 32768f;
                frame[ch] = y0;
            }
        } else {
            float t = (float) (pos - x0);
            for (int ch = 0; ch < channels.length; ch++) {
                float y0 = channels[ch][x0] / 32768f;
                float y1 = channels[ch][x1] / 32768f;
                frame[ch] = y0 + (y1 - y0) * t;
            }
        }
    }

    //TODO: move this into AudioSource
    public void getFrameLooping(double pos, float[] frame) {
        int x0 = (int) Math.floor(pos);
        assert x0 < loopEnd;

        int x1 = x0 + 1;
        if (x1 == loopEnd)
            x1 = loopStart;

        float t = (float) (pos - x0);
        for (int ch = 0; ch < channels.length; ch++) {
            float y0 = channels[ch][x0] / 32768f;
            float y1 = channels[ch][x1] / 32768f;
            frame[ch] = y0 + (y1 - y0) * t;
        }
    }

    public static ShortAudioBuffer readCompatible(File file) throws UnsupportedAudioFileException, IOException {
        AudioInputStream stream = AudioSystem.getAudioInputStream(file);
        try {
            stream = AudioConverter.convert(stream, ShortSampleReader.supportedFormats());
            return read(stream);
        } finally {
            stream.close();
        }
    }

    private static ShortAudioBuffer read(AudioInputStream stream) throws IOException, UnsupportedAudioFileException {
        AudioFormat format = stream.getFormat();

        ShortSampleReader sampleReader = ShortSampleReader.getReader(stream, stream.getFormat());
        short[][] channels = new short[format.getChannels()][];

        int frameLength = Math.toIntExact(stream.getFrameLength());
        int framePos = 0;
        if (frameLength == AudioSystem.NOT_SPECIFIED) {
            frameLength = UNKNOWN_INITIAL_FRAME_LENGTH;
            for (int ch = 0; ch < channels.length; ch++)
                channels[ch] = new short[UNKNOWN_INITIAL_FRAME_LENGTH];
            while (true) {
                if (framePos == frameLength) {
                    frameLength = Math.multiplyExact(frameLength, 2);
                    for (int ch = 0; ch < channels.length; ch++)
                        channels[ch] = Arrays.copyOf(channels[ch], frameLength);
                }
                int framesRead = sampleReader.readNFrames(channels, framePos, frameLength - framePos);
                if (framesRead == 0) break;
                framePos += framesRead;
            }
        } else {
            for (int ch = 0; ch < channels.length; ch++)
                channels[ch] = new short[frameLength];
            while (framePos < frameLength) {
                int framesRead = sampleReader.readNFrames(channels, framePos, frameLength - framePos);
                if (framesRead == 0) break;
                framePos += framesRead;
            }
        }
        return new ShortAudioBuffer(channels, framePos, format.getSampleRate());
    }    
}
