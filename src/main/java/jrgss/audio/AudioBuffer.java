package jrgss.audio;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import lombok.Getter;

public class AudioBuffer {
    private static final int UNKNOWN_INITIAL_FRAME_LENGTH = 4096;

    public final short[][] samples;
    public final int frameLength;
    public final float sampleRate;
    private @Getter int loopStartFrame;
    private @Getter int loopEndFrame; // (exclusive)

    private AudioBuffer(short[][] samples, int frameLength, float sampleRate) {
        this.samples = samples;
        this.frameLength = frameLength;
        this.sampleRate = sampleRate;
        this.loopStartFrame = 0;
        this.loopEndFrame = frameLength;
    }

    public void setLoopStart(int frame) {
        if (frame < 0 || frame >= loopEndFrame)
            throw new IllegalArgumentException("invalid loop start");
        this.loopStartFrame = frame;
    }

    public void setLoopEnd(int frame) {
        if (frame <= loopStartFrame || frame > frameLength)
            throw new IllegalArgumentException("invalid loop start");
        this.loopEndFrame = frame;
    }

    public void setLoopLength(int frames) {
        if (frames <= 0 || loopStartFrame + frames >= frameLength)
            throw new IllegalArgumentException("invalid loop length");
        this.loopEndFrame = loopStartFrame + frames;
    }

    public static AudioBuffer read(File file) throws UnsupportedAudioFileException, IOException {
        AudioInputStream stream = AudioSystem.getAudioInputStream(file);
        try {
            AudioFormat format = stream.getFormat();
            if (SampleReader.supports(format)) {
                return readSamples(stream);
            } else {
                // other compressed formats get decoded to 16 bit signed pcm
                AudioFormat newFormat = new AudioFormat(format.getSampleRate(), 16, format.getChannels(), true, format.isBigEndian());
                return readSamples(AudioSystem.getAudioInputStream(newFormat, stream));
            }
        } finally {
            stream.close();
        }
    }

    private static AudioBuffer readSamples(AudioInputStream stream) throws IOException {
        AudioFormat format = stream.getFormat();
        int frameLength = Math.toIntExact(stream.getFrameLength());
 
        SampleReader sampleReader = SampleReader.getReader(stream, stream.getFormat());
 
        short[][] samples = new short[format.getChannels()][];
        int framePos = 0;
        if (frameLength == AudioSystem.NOT_SPECIFIED) {
            frameLength = UNKNOWN_INITIAL_FRAME_LENGTH;
            for (int ch = 0; ch < samples.length; ch++)
                samples[ch] = new short[UNKNOWN_INITIAL_FRAME_LENGTH];
            while (true) {
                if (framePos == frameLength) {
                    frameLength = Math.multiplyExact(frameLength, 2);
                    for (int ch = 0; ch < samples.length; ch++)
                        samples[ch] = Arrays.copyOf(samples[ch], frameLength);
                }
                int framesRead = sampleReader.readNFrames(samples, framePos, frameLength - framePos);
                if (framesRead == 0) break;
                framePos += framesRead;
            }
        } else {
            for (int ch = 0; ch < samples.length; ch++)
                samples[ch] = new short[frameLength];
            while (framePos < frameLength) {
                int framesRead = sampleReader.readNFrames(samples, framePos, frameLength - framePos);
                if (framesRead == 0) break;
                framePos += framesRead;
            }
        }
        return new AudioBuffer(samples, framePos, format.getSampleRate());
    }    
}
