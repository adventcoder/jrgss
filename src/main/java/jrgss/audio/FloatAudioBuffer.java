package jrgss.audio;

import java.util.Arrays;

public class FloatAudioBuffer {
    public final float[][] channels;
    public final int frameLength;
    public final float sampleRate;

    public FloatAudioBuffer(int frameLength, int numChannels, float sampleRate) {
        this.frameLength = frameLength;
        this.sampleRate = sampleRate;
        this.channels = new float[numChannels][frameLength];
    }

    public void clear() {
        for (int ch = 0; ch < channels.length; ch++)
            Arrays.fill(channels[ch], 0f);
    }

    public void mixFrame(int i, float[] frame) {
        if (frame.length == 1) { // mono -> N (duplicate)
            float sample = frame[0];
            for (int ch = 0; ch < channels.length; ch++)
                channels[ch][i] += sample;
        } else if (channels.length == 1) { // N -> mono (average)
            double sample = 0.0;
            for (int ch = 0; ch < frame.length; ch++)
                sample += frame[ch];
            channels[0][i] = (float) (sample / frame.length);
        } else {
            // otherwise just copy
            int limit = Math.min(channels.length, frame.length);
            for (int ch = 0; ch < limit; ch++)
                channels[ch][i] += frame[i];
        }
    }
}
