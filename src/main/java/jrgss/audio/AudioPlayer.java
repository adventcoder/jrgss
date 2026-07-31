package jrgss.audio;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class AudioPlayer {
    private final SourceDataLine line;

    public AudioPlayer() throws LineUnavailableException {
        line = AudioSystem.getSourceDataLine(null);
    }

    public void playBackground() {
        
    }

    public void playEffect() {

    }

    public void update() {

    }

    public void close() {

    }
}
