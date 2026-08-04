package jrgss.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class AudioSource implements AutoCloseable {
    private final AudioBuffer buffer;
    private final boolean looping;

    private final SourceDataLine line;
    private final BooleanControl muteControl;
    private final FloatControl gainControl;

    private boolean running = false;
    private int framePos = 0;
    private long frameOffset = 0L;

    public AudioSource(AudioBuffer buffer, float pitch, boolean looping) throws LineUnavailableException {
        this.buffer = buffer;
        this.looping = looping;

        line = AudioSystem.getSourceDataLine(applyPitch(buffer.format, pitch));
        line.open();

        if (line.isControlSupported(BooleanControl.Type.MUTE)) {
            muteControl = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
        } else {
            muteControl = null;
        }

        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        } else {
            gainControl = null;
        }
    }

    private static AudioFormat applyPitch(AudioFormat format, float pitch) {
        if (pitch == 1f) return format;
        return new AudioFormat(format.getEncoding(), format.getSampleRate() * pitch, format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), format.getFrameRate() * pitch, format.isBigEndian());
    }

    public float getBufferTime() {
        AudioFormat format = line.getFormat();
        return (line.getBufferSize() / format.getFrameSize()) * format.getFrameRate();
    }

    public boolean isMute() {
        return muteControl == null ? false : muteControl.getValue();
    }

    public void setMute(boolean mute) {
        if (muteControl != null)
            muteControl.setValue(mute);
    }

    public float getGainDecibels() {
        return gainControl == null ? 0f : gainControl.getValue();
    }

    public void setGainDecibels(float gain) {
        if (gainControl != null)
            gainControl.setValue(Math.min(Math.max(gain, gainControl.getMinimum()), gainControl.getMaximum()));
    }

    public int getFramePosition() {
        long frameCount = line.getLongFramePosition() - frameOffset;
        if (looping && framePos < buffer.loopEnd) {
            if (frameCount < buffer.loopStart)
                return (int) framePos;
            return buffer.loopStart + Math.floorMod(frameCount - buffer.loopStart, buffer.loopEnd - buffer.loopStart);
        } else {
            assert frameCount < buffer.frameLength;
            return (int) frameCount;
        }
    }

    public void setFramePosition(int framePos) {
        if (running) throw new IllegalStateException("not stopped");
        this.framePos = framePos;
        line.flush();
        frameOffset = line.getLongFramePosition() - framePos;
    }

    public void start() {
        running = true;
        line.start();
    }

    public void stop() {
        running = false;
        line.stop();
    }

    public boolean atEnd() {
        return framePos < 0 || framePos >= buffer.frameLength;
    }

    public void update() {
        if (!running) return;

        int framesRemaining = line.available() / line.getFormat().getFrameSize();
        while (framesRemaining > 0 && !atEnd()) {
            if (looping && framePos < buffer.loopEnd) {
                int framesWritten = writeFrames(framePos, Math.min(buffer.loopEnd - framePos, framesRemaining));
                framePos += framesWritten;
                if (framePos == buffer.loopEnd)
                    framePos = buffer.loopStart;
                framesRemaining -= framesWritten;
            } else {
                int framesWritten = writeFrames(framePos, Math.min(buffer.frameLength - framePos, framesRemaining));
                framePos += framesWritten;
                framesRemaining -= framesWritten;
            }
        }
    }

    private int writeFrames(int start, int size) {
        int frameSize = line.getFormat().getFrameSize();
        return line.write(buffer.data, start*frameSize, size*frameSize) / frameSize;
    }

    @Override
    public void close() {
        line.close();
    }
}
