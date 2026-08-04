package jrgss.audio;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SoftAudioSource {
    private final ShortAudioBuffer source;
    private final FloatAudioBuffer dest;
    private final boolean looping;
    private float gain = 1f;
    private float pitch = 1f;
    private double pos = 0.0;

    public boolean atEnd() {
        return pos < 0 || pos >= source.frameLength;
    }

    public void update() {
        //TODO: fetch frames into a work buffer instead of one at a time.
        double step = ((double) source.sampleRate * pitch) / dest.sampleRate; // can be negative for reverse playback
        float[] frame = new float[source.channels.length];
        if (looping) {
            for (int i = 0; i < dest.frameLength && !atEnd(); i++) {
                source.getFrameLooping(pos, frame);
                applyGain(frame);
                dest.mixFrame(i, frame);

                boolean inLoop = pos >= source.loopStart && pos < source.loopEnd;
                pos += step;
                if (inLoop && !(pos >= source.loopStart && pos < source.loopEnd))
                    pos = wrap(pos, source.loopStart, source.loopEnd - source.loopStart);
            }
        } else {
            for (int i = 0; i < dest.frameLength && !atEnd(); i++) {
                source.getFrame(pos, frame);
                applyGain(frame);
                dest.mixFrame(i, frame);
                pos += step;
            }
        }
    }

    private static double wrap(double pos, double start, double length) {
        return start + (pos - start - Math.floor((pos - start) / length) * length);
    }
 
    private void applyGain(float[] frame) {
        for (int ch = 0; ch < frame.length; ch++)
            frame[ch] *= gain;
    }
}
