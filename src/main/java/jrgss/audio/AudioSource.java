package jrgss.audio;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AudioSource {
    private final AudioBuffer buffer;
    private final boolean looping;
    private float gain = 1f;
    private double pitch = 1.0;
    private double pos;
    private boolean stoppped = false;

    public void render(float[] mixBuffer) {

    }
}
