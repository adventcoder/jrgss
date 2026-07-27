package jrgss.audio.old;

import java.util.concurrent.atomic.AtomicReference;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class AudioSource implements Runnable {
    private static final float bufferSizeInSeconds = 1f;

    private final AudioBuffer buffer;
    private final SourceDataLine line;
    private final BooleanControl muteControl;
    private final FloatControl gainControl;
    private final boolean looping;
    private final Object lock = new Object();
    private volatile Thread thread;
    private volatile boolean stopped = true;
    private volatile long frameOffset;
    private volatile long prevFrameOffset;
    private volatile long startFrame;
    private final AtomicReference<Long> nextPos = new AtomicReference<>();

    public AudioSource(AudioBuffer buffer, float pitch, boolean looping) throws LineUnavailableException {
        this.buffer = buffer;
        this.looping = looping;

        AudioFormat format = buffer.getFormat(pitch);
        int bufferSize = (int) Math.ceil(bufferSizeInSeconds * format.getFrameRate()) * format.getFrameSize();
        line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format, bufferSize));
        line.open();

        muteControl = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
        gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);

        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    public float getGain() {
        if (muteControl.getValue()) {
            return 0f;
        } else {
            return (float) Math.pow(10, gainControl.getValue() / 20f);
        }
    }

    public void setGain(float gain) {
        if (gain < 0)
            throw new IllegalArgumentException("negative gain");
        if (gain == 0) {
            muteControl.setValue(true);
        } else {
            gainControl.setValue(20f * (float) Math.log10(gain));
        }
    }

    public long getPosition() {
        return getFramePosition() * buffer.getFrameSize();
    }

    public void setPosition(long pos) {
        setFramePosition(pos / buffer.getFrameSize());
    }

    public long getFramePosition() {
        Long requestedPos = nextPos.get();
        if (requestedPos != null)
            return requestedPos.longValue();
        long frame = line.getLongFramePosition() - frameOffset;
        if (frame < startFrame) // still working through buffered data
            frame = line.getLongFramePosition() - prevFrameOffset;
        return frame;
    }

    public void setFramePosition(long framePos) {
        nextPos.set(framePos * buffer.getFrameSize());
        line.flush(); // flushes buffered data and interrupts the line if running
    }

    // playing means we haven't finished playing all audio yet.
    // playing is true as soon as the source is created but playback will begin stopped.
    // calling start resumes playback.
    // calling stop pauses playback but doesn't change playing state.
    // when playback reaches the end playing will become false and will also be considered stopped.
    // calling start again will have no effect.
    // calling close aborts playback if not yet finished and disposes all resources.
    public boolean isPlaying() {
        return thread != null;
    }

    // can't use line.isRunning() for this as that is implemented to only become true after the first data is written
    // SourceDataLine also doesn't expose a way to notify when the line enters running state so we must also keep our own lock
    public boolean isStopped() {
        return stopped;
    }

    public void start() {
        if (thread == null || !stopped) return;
        line.start(); // start the line, it will notify it's internal lock but nothing should be waiting for it
        stopped = false;
        synchronized (lock) {
            lock.notifyAll(); // notify our lock
        }
    }

    public void stop() {
        if (thread == null || stopped) return;
        stopped = true; // don't need to notify our lock here, we're only waiting for started or closed
        line.stop(); // close the line, it will notify it's internal lock which will interrupt line.write
    }

    public void close() {
        stop();
        if (thread != null) {
            thread = null;
            synchronized (lock) {
                lock.notifyAll(); // notify our lock
            }
        }
        line.close(); // close the line, it will notify it's internal lock which will interrupt line.write
    }

    @Override
    public void run() {
        long pos = 0;
        while (thread != null) {
            synchronized (lock) {
                while (stopped && thread != null) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }

            while (!stopped && thread != null) {
                Long requestedPos = nextPos.getAndSet(null);
                if (requestedPos != null) {
                    pos = requestedPos.longValue();
                    resetFrameOffset(pos);
                }

                // position outside of data means finsihed
                if (pos < 0 || pos >= buffer.getByteLength()) {
                    line.drain();
                    thread = null;
                    stopped = true;
                    line.stop();
                    break;
                }

                if (looping) {
                    if (pos <= 0 || pos >= buffer.getLoopEnd()) {
                        pos = buffer.getLoopStart();
                        resetFrameOffset(pos);
                    }
                    pos += buffer.writeToLine(line, pos, buffer.getLoopEnd());
                } else {
                    pos += buffer.writeToLine(line, pos, buffer.getByteLength());
                }
            }
        }
    }

    private void resetFrameOffset(long pos) {
        startFrame = pos / buffer.getFrameSize();
        prevFrameOffset = frameOffset;
        frameOffset = line.getLongFramePosition() + (line.getBufferSize() - line.available()) - startFrame;
    }
}
