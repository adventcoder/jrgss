package jrgss.audio;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public interface Buffer {
    public Source openSource(float pitch, boolean looping) throws LineUnavailableException;

    public static Buffer read(File file) throws IOException, UnsupportedAudioFileException {
        return SampleBuffer.read(file);
    }
}
