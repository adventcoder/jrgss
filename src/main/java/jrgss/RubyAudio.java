package jrgss;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

import com.github.trilarion.sound.vorbis.sampled.spi.VorbisAudioFileReader;

import lombok.RequiredArgsConstructor;

//TODO: midi support? (probably don't care about this)
@RequiredArgsConstructor
public class RubyAudio implements LineListener {
    public static void createAudioModule(Ruby runtime) {
        RubyModule mod = runtime.defineModule("Audio");
        RubySupport.Audio = mod;
        mod.defineAnnotatedMethods(RubyAudio.class);
    }

    //TODO: do we need these static properties or should we get directly from game properties (probably we should)
    private static boolean musicMuted;
    private static boolean soundMuted;

    public static void setMusicMuted(boolean muted) {
        musicMuted = muted;
        bgm.updateMuted();
        me.updateMuted();
    }

    public static void setSoundMuted(boolean muted) {
        soundMuted = muted;
        bgs.updateMuted();
        se.updateMuted();
    }

    private static float linearToDb(int volume) {
        if (volume == 0) return Float.NEGATIVE_INFINITY;
        return 20 * (float) Math.log10(volume / 100f);
    }

    public enum Type {
        BGM, BGS, ME, SE;

        public boolean isLooping() {
            return this == BGM || this == BGS;
        }

        public boolean isMusic() {
            return this == BGM || this == ME;
        }

        public boolean isSound() {
            return this == BGS || this == SE;
        }

        public boolean isMuted() {
            if (isMusic()) return musicMuted;
            if (isSound()) return soundMuted;
            return false;
        }
    }

    private final Type type;
    private Clip clip;
    private File clipFile;
    private long frameOffset;
    private boolean paused;
    private boolean running; // whether audio should be playing irrespective of current pause state

    public void play(File file, int volume, int pitch, int pos) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        // TODO: should we always stop here? RGSS continues playing but still applies volume and pitch adjustments if the file is the same
        // it also just ignores position... sometimes? needs more testing
        // this would prevent music effects pausing the bgm and restarting it briefly
        stop();

        if (clip == null || !file.equals(clipFile)) { // different file we need to load a new clip
            closeClip();
            openClip(file);
        }

        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(Math.min(Math.max(linearToDb(volume), gainControl.getMinimum()), gainControl.getMaximum()));
        }

        // this is likely not supported
        if (clip.isControlSupported(FloatControl.Type.SAMPLE_RATE)) {
            FloatControl sampleRateControl = (FloatControl) clip.getControl(FloatControl.Type.SAMPLE_RATE);
            float baseSampleRate = clip.getFormat().getSampleRate();
            sampleRateControl.setValue(baseSampleRate * pitch / 100f);
        }

        updateMuted();

        // we shouldn't change position if the clip is currently running, only matters if we don't always call stop() at the beginning of this method
        int framePos = pos / clip.getFormat().getFrameSize();
        if (framePos < 0 || framePos >= clip.getFrameLength())
            framePos = 0;
        clip.setFramePosition(framePos);
        frameOffset = clip.getLongFramePosition() - framePos;

        start();
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        if (!paused)
            clip.loop(type.isLooping() ? Clip.LOOP_CONTINUOUSLY : 0);
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        if (!paused)
            clip.stop();
    }

    public synchronized void pause() {
        if (paused) return;
        paused = true;
        if (running)
            clip.stop();
    }

    public synchronized void resume() {
        if (!paused) return;
        paused = false;
        if (running)
            clip.loop(type.isLooping() ? Clip.LOOP_CONTINUOUSLY : 0);
    }

    public void fade(int millis) {
        if (clip == null) return;
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            // this is not linear, but it's not actually supported by java audio anyway, it just sets the target value
            gain.shift(gain.getValue(), gain.getMinimum(), millis*1000);
        }
    }

    public int pos() {
        if (clip == null) return 0;
        long framePos = clip.getFramePosition() - frameOffset;
        if (type.isLooping()) {
            // need to take into account loop start/end
            framePos = framePos % clip.getFrameLength();
        }
        return (int) framePos * clip.getFormat().getFrameSize();
    }

    private void openClip(File file) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        AudioInputStream source = AudioSystem.getAudioInputStream(file);
        try {
            AudioFormat sourceFormat = source.getFormat();
            if (source.getFormat().getEncoding().equals(VorbisAudioFileReader.VORBISENC)) {
                // vorbis spi decodes to 16 bit signed pcm samples (seems to be little endian)
                // retain sample rate and channels from source
                sourceFormat = new AudioFormat(sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), true, false);
                source = AudioSystem.getAudioInputStream(sourceFormat, source);
            }

            clip = (Clip) AudioSystem.getLine(new DataLine.Info(Clip.class, sourceFormat));
            clip.open(source);
            if (type == Type.ME)
                clip.addLineListener(this);
            clipFile = file;

            //TODO: need to extract loop points for ogg files
            // clip.setLoopPoints(loopStart, loopEnd);
        } finally {
            source.close();
        }
    }

    private void closeClip() {
        if (clip != null) {
            clip.removeLineListener(this);
            clip.close();
            clip = null;
        }
    }

    @Override
    public void update(LineEvent event) {
        if (type == Type.ME) {
            if (event.getType() == LineEvent.Type.START) {
                bgm.pause();
            } if (event.getType() == LineEvent.Type.STOP) {
                bgm.resume();
            }
        }
    }

    private void updateMuted() {
        if (clip == null) return;
        if (clip.isControlSupported(BooleanControl.Type.MUTE)) {
            BooleanControl muteControl = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
            muteControl.setValue(type.isMuted());
        }
    }

    private static final RubyAudio bgm = new RubyAudio(Type.BGM);
    private static final RubyAudio bgs = new RubyAudio(Type.BGS);
    private static final RubyAudio me = new RubyAudio(Type.ME);
    private static final RubyAudio se = new RubyAudio(Type.SE);

    @JRubyMethod(meta = true, required = 1, optional = 3)
    public static void bgm_play(IRubyObject recv, IRubyObject... args) throws Exception { //TODO: exception handling
        File file = RTP.findFile(recv.getRuntime().getCurrentDirectory(), args[0].asJavaString());
        int volume = args.length >= 2 ? RubySupport.numToIntInRangeClamped(args[1], 0, 200) : 100;
        int pitch = args.length >= 3 ? RubySupport.numToIntInRangeClamped(args[2], 50, 150) : 100;
        int pos = args.length >= 4 ? RubyNumeric.num2int(args[3]) : 0;

        bgm.play(file, volume, pitch, pos);
    }

    @JRubyMethod(meta = true)
    public static void bgm_stop(IRubyObject recv) {
        bgm.stop();
    }

    @JRubyMethod(meta = true)
    public static void bgm_fade(IRubyObject recv, IRubyObject arg0) {
        bgm.fade(RubyNumeric.num2int(arg0));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject bgm_pos(IRubyObject recv) {
        return recv.getRuntime().newFixnum(bgm.pos());
    }

    @JRubyMethod(meta = true, required = 1, optional = 3)
    public static void bgs_play(IRubyObject recv, IRubyObject... args) throws Exception { //TODO: exception handling
        File file = RTP.findFile(recv.getRuntime().getCurrentDirectory(), args[0].asJavaString());
        int volume = args.length >= 2 ? RubySupport.numToIntInRangeClamped(args[1], 0, 200) : 100;
        int pitch = args.length >= 3 ? RubySupport.numToIntInRangeClamped(args[2], 50, 150) : 100;
        int pos = args.length >= 4 ? RubyNumeric.num2int(args[3]) : 0;

        bgs.play(file, volume, pitch, pos);
    }

    @JRubyMethod(meta = true)
    public static void bgs_stop(IRubyObject recv) {
        bgs.stop();
    }

    @JRubyMethod(meta = true)
    public static void bgs_fade(IRubyObject recv, IRubyObject arg0) {
        bgs.fade(RubyNumeric.num2int(arg0));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject bgs_pos(IRubyObject recv) {
        return recv.getRuntime().newFixnum(bgs.pos());
    }

    @JRubyMethod(meta = true, required = 1, optional = 2)
    public static void me_play(IRubyObject recv, IRubyObject... args) throws Exception { //TODO: exception handling
        File file = RTP.findFile(recv.getRuntime().getCurrentDirectory(), args[0].asJavaString());
        int volume = args.length >= 2 ? RubySupport.numToIntInRangeClamped(args[1], 0, 200) : 100;
        int pitch = args.length >= 3 ? RubySupport.numToIntInRangeClamped(args[2], 50, 150) : 100;

        me.play(file, volume, pitch, 0);
    }

    @JRubyMethod(meta = true)
    public static void me_fade(IRubyObject recv, IRubyObject arg0) {
        me.fade(RubyNumeric.num2int(arg0));
    }

    @JRubyMethod(meta = true)
    public static void me_stop(IRubyObject recv) {
        me.stop();
    }

   @JRubyMethod(meta = true, required = 1, optional = 2)
    public static void se_play(IRubyObject recv, IRubyObject... args) throws Exception { //TODO: exception handling
        File file = RTP.findFile(recv.getRuntime().getCurrentDirectory(), args[0].asJavaString());
        int volume = args.length >= 2 ? RubySupport.numToIntInRangeClamped(args[1], 0, 200) : 100;
        int pitch = args.length >= 3 ? RubySupport.numToIntInRangeClamped(args[2], 50, 150) : 100;

        //TODO: multiple simultaneous sounds effects + filtering when the same sound is played rapidly
        se.play(file, volume, pitch, 0);
    }

    @JRubyMethod(meta = true)
    public static void se_stop(IRubyObject recv) {
        se.stop();
    }
}
