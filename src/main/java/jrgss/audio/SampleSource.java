package jrgss.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import lombok.Getter;

public class SampleSource implements Source, Runnable {
    private final SampleBuffer buffer;
    private final @Getter float pitch;
    private final @Getter boolean looping;

    private @Getter boolean mute;
    private @Getter float gain;

    private SourceDataLine line;
    private FloatControl gainControl;
    private BooleanControl muteControl;

    private volatile Thread thread;
    private long frameOffset;
    private int framePos;

    public SampleSource(SampleBuffer buffer, float pitch, boolean looping) throws LineUnavailableException {
        this.buffer = buffer;
        this.pitch = pitch;
        this.looping = looping;

        AudioFormat format = buffer.getFormat(pitch);
        int bufferSize = buffer.secondsToBytes(1f);

        line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format, bufferSize));
        line.open(format);

        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN))
            gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        if (line.isControlSupported(BooleanControl.Type.MUTE))
            muteControl = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
    }

    public int getFramePosition() {
        long frame = line.getLongFramePosition() - frameOffset;
        if (looping) {
            int loopLength = buffer.loopEndFrame - buffer.loopStartFrame;
            return buffer.loopStartFrame + Math.floorMod(frame - buffer.loopStartFrame, loopLength);
        } else {
            assert frame >= 0 && frame < buffer.frameLength;
            return (int) frame;
        }
    }

    public void setFramePosition(int frame) {
        if (looping && frame >= buffer.loopEndFrame) {
            this.framePos = buffer.loopStartFrame;
        } else if (frame < 0 || frame >= buffer.frameLength) {
            this.framePos = 0;
        } else {
            this.framePos = frame;
        }
        line.flush();
        this.frameOffset = line.getFramePosition() - framePos;
    }

    public void setMute(boolean mute) {
        this.mute = mute;
        if (muteControl != null)
            muteControl.setValue(mute || gain <= 0f);
    }

    public void setGain(float gain) {
        this.gain = gain;
        if (muteControl != null)
            muteControl.setValue(mute || gain <= 0f);
        if (gainControl != null) {
            if (gain <= 0f) {
                gainControl.setValue(gainControl.getMinimum());
            } else {
                float gainDb = 20f * (float) Math.log10(gain);
                gainControl.setValue(Math.min(Math.max(gainDb, gainControl.getMinimum()), gainControl.getMaximum()));
            }
        }
    }

    public boolean isRunning() {
        return thread != null || line.isRunning();
    }

    public void start() {
        thread = new Thread(this, "sample-source-thread");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        Thread oldThread = thread;
        thread = null;
        oldThread.interrupt();
    }

    @Override
    public void run() {
        line.start();
        try {
            int endFrame = looping ? buffer.loopEndFrame : buffer.frameLength;
            int maxWriteFrames = Integer.MAX_VALUE; // when we implement fade may want to limit this
            while (thread == Thread.currentThread()) {
                if (framePos == endFrame) {
                    if (!looping) break;
                    framePos = buffer.loopStartFrame;
                }
                int toWrite = Math.min(endFrame - framePos, maxWriteFrames);
                int bytesWritten = buffer.writeSamples(line, framePos, toWrite);
                framePos += buffer.bytesToFrames(bytesWritten);
            }
            line.drain();
        } finally {
            line.stop();
        }
    }

    @Override
    public void close() {
        gainControl = null;
        muteControl = null;
        line.close();
        line = null;
    }
}
