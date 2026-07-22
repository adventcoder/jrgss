package jrgss.audio;

import java.util.function.Consumer;

public interface AudioSource extends AutoCloseable {
    public AudioBuffer getBuffer();
    public boolean isLooping();
    public int getPitch();
    public int getPosition();
    public int getVolume();
    public void setVolume(int volume);
    public Consumer<AudioSource> getStopCallback();
    public void setStopCallback(Consumer<AudioSource> callback);
    public boolean isRunning();
    public boolean isClosed();
    public void start();
    public void stop();
    @Override
    public void close();

    public static float linearToDb(float gain) {
        return 20f * (float) Math.log10(gain);
    }

    public static float dbToLinear(float gain) {
        return (float) Math.pow(10, gain / 20f);
    }
}
