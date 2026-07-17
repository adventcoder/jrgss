package jrgss.audio;

public interface Source extends AutoCloseable {
    public boolean isLooping();
    public float getPitch();
    public int getFramePosition();
    public void setFramePosition(int frame);
    public boolean isMute();
    public void setMute(boolean mute);
    public float getGain();
    public void setGain(float gain);
    public boolean isRunning();
    public void start();
    public void stop();
    @Override
    public void close();
}
