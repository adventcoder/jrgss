package jrgss.audio;

public interface AudioSource extends AutoCloseable {
    public AudioBuffer getBuffer();
    public boolean isLooping();
    public int getPitch();
    public int getPosition();
    public int getVolume();
    public void setVolume(int volume);
    public void fadeVolume(int volume, int millis);
    public boolean isStopped();
    public boolean isStopping();
    public boolean start();
    public boolean stop();
    @Override
    public void close();

    public static float linearToDb(float gain) {
        return 20f * (float) Math.log10(gain);
    }

    public static float dbToLinear(float gain) {
        return (float) Math.pow(10, gain / 20f);
    }
}
