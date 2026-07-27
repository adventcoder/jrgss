package jrgss.audio;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import com.google.common.util.concurrent.Uninterruptibles;

import lombok.Getter;

public class AudioPlayer implements AutoCloseable, Runnable, LineListener {
    private final SourceDataLine line;
    private final BooleanControl muteControl;
    private final FloatControl gainControl;

    private @Getter Consumer<AudioPlayer> stopCallback;
    private @Getter byte[] data;
    private @Getter int frameLength;
    private @Getter int loopStart;
    private @Getter int loopEnd;
    private @Getter boolean looping;

    private final Object lock = new Object();
    private int framePos;
    private final AtomicReference<Integer> pendingFramePos = new AtomicReference<>();
    private volatile Thread thread;

    public AudioPlayer(AudioFormat format) throws LineUnavailableException {
        line = AudioSystem.getSourceDataLine(format);
        line.addLineListener(this);
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

    public AudioFormat getFormat() {
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
        return gainControl == null ? 0f : gainControl.getValue();
    }

    public void setVolume(float volume) {
        if (gainControl != null)
            gainControl.setValue(Math.min(Math.max(volume, gainControl.getMinimum()), gainControl.getMaximum()));
    }

    public void setStopCallback(Consumer<AudioPlayer> callback) {
        if (isRunning()) throw new IllegalStateException("not stopped");
        this.stopCallback = callback;
    }

    // data is not copied, but it should not be modified
    // this also resets loop points
    public void setData(byte[] data, int frameLength) {
        if (isRunning()) throw new IllegalStateException("not stopped");
        Objects.checkFromIndexSize(0, frameLength, data.length);
        this.data = Objects.requireNonNull(data);
        this.frameLength = frameLength;
        this.loopStart = 0;
        this.loopEnd = frameLength;
    }

    public void setLoopStart(int newLoopStart) {
        if (isRunning()) throw new IllegalStateException("not stopped");
        if (newLoopStart < 0 || newLoopStart >= loopEnd)
            throw new IllegalArgumentException("invalid loop start");
        this.loopStart = newLoopStart;
    }

    public void setLoopEnd(int newLoopEnd) {
        if (isRunning()) throw new IllegalStateException("not stopped");
        if (newLoopEnd <= loopStart || newLoopEnd > frameLength)
            throw new IllegalArgumentException("invalid loop end");
        this.loopEnd = newLoopEnd;
    }

    public void setLooping(boolean looping) {
        if (isRunning()) throw new IllegalStateException("not stopped");
        this.looping = looping;
    }

    public void setPosition(int pos) {
        if (isRunning()) {
            line.flush();
            pendingFramePos.set(pos);
        } else {
            pendingFramePos.set(null);
            this.framePos = pos;
        }
    }

    public boolean isClosed() {
        return !line.isOpen();
    }

    public boolean isRunning() {
        return thread != null && thread.isAlive();
    }

    public void start() {
        if (isClosed())
            throw new IllegalStateException("closed");
        if (isRunning()) return;

        // we know the thread is not running here

        if (data == null)
            throw new IllegalStateException("data not set");
        if (isFinished()) return;

        line.start();

        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        if (isClosed())
            throw new IllegalStateException("closed");

        Thread oldThread = thread;
        synchronized (lock) {
            thread = null;
        }

        // shouldn't matter if the line is already stopped
        //NOTE: line.stop() interrupts line.write()/line.drain()
        line.stop();

        if (oldThread != null && oldThread != Thread.currentThread())
            Uninterruptibles.joinUninterruptibly(oldThread);
    }

    public void close() {
        Thread oldThread = thread;
        synchronized (lock) {
            thread = null;
        }

        // shouldn't matter if the line is already closed
        //NOTE: line.close() interrupts line.write()/line.drain()
        line.close();

        if (oldThread != null && oldThread != Thread.currentThread())
            Uninterruptibles.joinUninterruptibly(oldThread);
    }

    @Override
    public void run() {
        while (Thread.currentThread() == thread) {
            Integer newFramePos = pendingFramePos.getAndSet(null);
            if (newFramePos != null)
                framePos = newFramePos.intValue();

            if (isFinished()) {
                // javadoc says drain blocks if line is stopped/closed and won't actually drain.
                // contrary to that, it appears that calling stop/close while it is draining interrupts it.
                // this could happen while we are stopping/closing.
                // we need to not call it since we wait for the thread to join which would cause a deadlock.
                synchronized (lock) {
                    if (thread == Thread.currentThread())
                        line.drain();
                }
                line.stop();
                thread = null;
            } else {
                int endFrame = looping ? loopEnd : frameLength;
                int frameSize = getFormat().getFrameSize();
                int bytesWritten = line.write(data, framePos * frameSize, (endFrame - framePos) * frameSize);
                framePos += bytesWritten / frameSize;
                if (framePos == endFrame && looping)
                    framePos = loopStart;
            }
        }
    }

    // this is only ever called from the playback thread or outside if we know it's not running
    private boolean isFinished() {
        int endFrame = looping ? loopEnd : frameLength;
        return framePos < 0 || framePos >= endFrame;
    }

    @Override
    public void update(LineEvent event) {
        if (event.getType() == LineEvent.Type.STOP) {
            if (stopCallback != null)
                stopCallback.accept(this);
        }
    }
}
