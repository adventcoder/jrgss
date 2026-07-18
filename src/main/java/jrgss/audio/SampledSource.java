package jrgss.audio;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import com.google.common.util.concurrent.Uninterruptibles;

import lombok.Getter;

public class SampledSource implements AudioSource, Runnable {
    private static ExecutorService executorService = Executors.newCachedThreadPool();
    private final @Getter SampledBuffer buffer;
    private final @Getter boolean looping;
    private final int endFrame;
    private final @Getter int pitch;
    private @Getter int volume;
    private int targetVolume;
    private long fadeDuration;
    private SourceDataLine line;
    private Future<?> task;
    private volatile int framePos;

    public SampledSource(SampledBuffer buffer, int volume, int pitch, int pos, boolean looping) throws LineUnavailableException {
        this.buffer = buffer;
        this.pitch = pitch;
        this.looping = looping;
        this.endFrame = looping ? buffer.loopEndFrame : buffer.frameLength - 1;

        AudioFormat format = buffer.getFormat(pitch);
        int bufferSize = buffer.millisToBytes(1000);
        DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, format, bufferSize);

        line = (SourceDataLine) AudioSystem.getLine(lineInfo);
        line.open(format);

        framePos = Math.min(Math.max(buffer.bytesToFrames(pos), 0), endFrame);

        setVolume(volume);
    }

    @Override
    public int getPosition() {
        return buffer.framesToBytes(getFramePosition());
    }

    public int getFramePosition() {
        //TODO: this will be ahead of the frames actually written to the line
        //      we should subtract the current number of buffered frames that haven't been written yet?
        //      and take into looping...
        return framePos;
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
    public void fadeVolume(int volume, int millis) {
        if (millis <= 0) {
            setVolume(volume);
            targetVolume = volume;
            fadeDuration = 0;
        } else {
            targetVolume = volume;
            fadeDuration = millis*1000000L;
        }
    }

    @Override
    public boolean isStopped() {
        return task == null || task.isDone();
    }

    @Override
    public boolean isStopping() {
        return !isStopped() && task.isCancelled();
    }

    @Override
    public boolean start() {
        waitIfStopping();
        if (!isStopped()) return false;
        task = executorService.submit(this);
        return true;
    }

    @Override
    public boolean stop() {
        if (isStopped()) return false;
        return task.cancel(true);
    }

    @Override
    public void close() {
        waitIfStopping();
        if (!isStopped())
            throw new IllegalStateException("not stopped");
        line.close();
    }

    private void waitIfStopping() {
        if (!isStopping()) return;
        try {
            Uninterruptibles.getUninterruptibly(task);
        } catch (CancellationException | ExecutionException ignored) {
        }
    }

    @Override
    public void run() {
        line.start();
        long startTime = System.nanoTime();
        try {
            while (!Thread.currentThread().isInterrupted()) {
                int framesAvailable = buffer.bytesToFrames(line.available());
                if (framesAvailable < buffer.millisToFrames(100)) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    int framesToWrite = Math.min(endFrame + 1 - framePos, framesAvailable);
                    int framesWritten = buffer.writeSamples(line, framePos, framesToWrite);
                    framePos += framesWritten;
                    if (framePos > endFrame) {
                        if (looping) {
                            framePos = buffer.loopStartFrame;
                        } else {
                            line.drain();
                            break;
                        }
                    }
                }
                long dt = System.nanoTime() - startTime;
                updateFade(dt);
                startTime += dt;
            }
        } finally {
            line.stop();
        }
    }

    private void updateFade(long dt) {
        if (fadeDuration == 0) return;
        setVolume((int) ((volume*(fadeDuration-dt) + targetVolume*dt) / fadeDuration));
        fadeDuration -= dt;
    }
}
