package jrgss.audio;

import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class RealTimeAudioPlayer implements AutoCloseable {
    private static final AudioFormat DEFAULT_FORMAT = new AudioFormat(44100, 16, 2, true, false);
    private static final float VOLUME_BOOST = 2f;
    private static final float VOLUME_FACTOR = VOLUME_BOOST * 20f / (float) Math.log(10);

    private SourceDataLine line;
    private BooleanControl muteControl = null;
    private FloatControl gainControl = null;

    public RealTimeAudioPlayer() throws LineUnavailableException {
        this(DEFAULT_FORMAT);
    }

    public RealTimeAudioPlayer(AudioFormat format) throws LineUnavailableException {
        line = AudioSystem.getSourceDataLine(format);
        line.open();

        if (line.isControlSupported(BooleanControl.Type.MUTE))
            muteControl = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN))
            gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
    }

    public AudioFormat getFormat() {
        line.drain();
        return line.getFormat();
    }

    public boolean isMute() {
        return muteControl == null ? false : muteControl.getValue();
    }

    public void setMute(boolean mute) {
        if (muteControl != null)
            muteControl.setValue(mute);
    }

    public float getVolume() {
        return (float) Math.exp(getVolumeDecibels() / VOLUME_FACTOR);
    }

    public void setVolume(float volume) {
        setVolumeDecibels(VOLUME_FACTOR * (float) Math.log(volume));
    }

    public float getVolumeDecibels() {
        return gainControl == null ? 1f : gainControl.getValue();
    }

    public void setVolumeDecibels(float volumeDB) {
        if (gainControl != null)
            gainControl.setValue(Math.min(Math.max(volumeDB, gainControl.getMinimum()), gainControl.getMaximum()));
    }

    public void update() {
    }

    public void close() {
        line.close();
    }
}
