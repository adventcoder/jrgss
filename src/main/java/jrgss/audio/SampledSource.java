package jrgss.audio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import lombok.Getter;
import lombok.Setter;

public class SampledSource implements AudioSource, Runnable {
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    private final @Getter SampledBuffer buffer;
    private final @Getter boolean looping;
    private final int startFrame;
    private final int endFrame;
    private final @Getter int pitch;
    private @Getter int volume = 100;
    private @Getter @Setter Consumer<AudioSource> stopCallback;
    private final SourceDataLine line;
    private final Future<?> future;
    private volatile boolean running;
    private volatile boolean closed;

    public SampledSource(SampledBuffer buffer, int pitch, int pos, boolean looping) throws LineUnavailableException {
        this.buffer = buffer;
        this.pitch = pitch;
        this.looping = looping;
        this.endFrame = looping ? buffer.loopEndFrame : buffer.frameLength - 1;
        this.startFrame = Math.min(Math.max(buffer.bytesToFrames(pos), 0), endFrame);

        AudioFormat format = buffer.getFormat(pitch);
        line = AudioSystem.getSourceDataLine(format);
        line.open(format);

        future = executorService.submit(this);
    }

    @Override
    public int getPosition() {
        return buffer.framesToBytes(getFramePosition());
    }

    public int getFramePosition() {
        long framePos = startFrame + line.getLongFramePosition();
        if (looping && framePos > endFrame) {
            int loopLength = buffer.loopEndFrame - buffer.loopStartFrame + 1;
            return buffer.loopStartFrame + Math.floorMod(framePos - buffer.loopStartFrame, loopLength);
        }
        return (int) framePos;
    }

    @Override
    public void setVolume(int volume) {
        this.volume = volume;
        if (line.isControlSupported(BooleanControl.Type.MUTE)) {
            BooleanControl muteControl = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
            muteControl.setValue(volume <= 0);
        }
        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            if (volume <= 0) {
                gainControl.setValue(gainControl.getMinimum());
            } else {
                gainControl.setValue(Math.min(Math.max(AudioSource.linearToDb(volume / 100f), gainControl.getMinimum()), gainControl.getMaximum()));
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized void start() {
        if (running) return;
        running = true;
        notifyAll();
        line.start();
    }

    @Override
    public synchronized void stop() {
        if (!running) return;
        running = false;
        line.stop(); // this will unblock line.write and stop io activity
        if (stopCallback != null)
            stopCallback.accept(this);
    }

    @Override
    public synchronized void close() {
        closed = true;
        notifyAll();
        line.close(); // this will unblock line.write and stop io activity
    }

    @Override
    public void run() {
        int framePos = startFrame;
        while (!closed) {
            synchronized (this) {
                while (!running && !closed) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            while (running && !closed) {
                int framesWritten = buffer.writeSamples(line, framePos, endFrame + 1 - framePos);
                framePos += framesWritten;
                if (framePos > endFrame) {
                    if (looping) {
                        framePos = buffer.loopStartFrame;
                    } else {
                        line.drain();
                        stop();
                    }
                }
            }
        }
    }
}
