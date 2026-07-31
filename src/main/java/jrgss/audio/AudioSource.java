package jrgss.audio;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AudioSource {
    private final AudioBuffer buffer;
    private final boolean looping;
    private float gain = 1f;
    private float pitch = 1f;
    private double framePos;

    public void render(float[] mixBuffer, float mixRate, int nFrames) {
        float sampleRate = buffer.sampleRate * pitch;
        double step = sampleRate / mixRate;

        for (int i = 0; i < nFrames; i++) {
            int iPos = (int) Math.floor(framePos);
            float fPos = (float) (framePos - iPos);

            float y0 = buffer.samples[iPos] / 32768f;
            float y1 = buffer.samples[iPos + 1] / 32768f;
            float sample = y0 + (y1 - y0) * fPos;

            mixBuffer[2*i] += sample * gain;
            mixBuffer[2*i+1] += sample * gain;

            framePos += step;
        }
    }
}
