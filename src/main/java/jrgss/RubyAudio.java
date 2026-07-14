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
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

import com.github.trilarion.sound.vorbis.sampled.spi.VorbisAudioFileReader;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

//TODO: integrate with game properties playMusic/playSound on/off
//TODO: midi support? (probably don't care about this)
@RequiredArgsConstructor
public class RubyAudio {
    public static void createAudioModule(Ruby runtime) {
        RubyModule mod = runtime.defineModule("Audio");
        RubySupport.Audio = mod;
        mod.defineAnnotatedMethods(RubyAudio.class);
    }

    private static float linearToDb(float val) {
        return 20f * (float) Math.log10(val);
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
    }

    private final @NonNull Type type;
    private Clip clip;
    private File clipFile;
    private long frameOffset;

    public void play(File file, int volume, int pitch, int pos) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        stop();

        //TODO: pitch adjustment, we will have to create a new clip with the source sample rate adjusted

        if (clip == null || !file.equals(clipFile)) { // we need to create a new clip
            if (clip != null)
                clip.close();

            AudioInputStream source = AudioSystem.getAudioInputStream(file);
            try {
                AudioFormat sourceFormat = source.getFormat();
                if (source.getFormat().getEncoding().equals(VorbisAudioFileReader.VORBISENC)) {
                    // vorbis decodes to 16 bit signed pcm samples (TODO: seems to be little endian? is this platform dependent?)
                    // retain sample rate and channels from source
                    sourceFormat = new AudioFormat(sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), true, false);
                    source = AudioSystem.getAudioInputStream(sourceFormat, source);
                }

                clip = (Clip) AudioSystem.getLine(new DataLine.Info(Clip.class, sourceFormat));
                clip.open(source);
                clipFile = file;

                //TODO: need to extract these for ogg files
                // clip.setLoopPoints(loopStart, loopEnd);
            } finally {
                source.close();
            }
        }

        if (volume == 0) {
            BooleanControl mute = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
            mute.setValue(true);
        } else {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gain.setValue(Math.min(Math.max(linearToDb(volume / 100f), gain.getMinimum()), gain.getMaximum()));
        }

        int framePos = pos / clip.getFormat().getFrameSize();
        if (framePos < 0 || framePos >= clip.getFrameLength())
            framePos = 0;

        clip.setFramePosition(framePos);
        frameOffset = clip.getLongFramePosition() - framePos;

        if (type.isLooping()) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } else {
            clip.start();
        }
    }

    public void stop() {
        if (clip != null)
            clip.stop();
    }

    public void fade(long millis) {
        //TODO: probably will have to spawn a thread for this
        // how does stop/play interact?
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
        bgm.fade(RubyNumeric.num2long(arg0));
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
        bgs.fade(RubyNumeric.num2long(arg0));
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

        //TODO: bgm should stop temporarily and restart afterwards
        // the bgm stops immediately but there is a fade in effect on restart
        // I think this is implemented by stopping as we normally do, then restarting from the start of the track when the me completes
        // need to test what happens if the bgm has been played/stopped in the meantime

        //TODO: if the same me is played before the current finishes it resumes from the current position not from the start. (note no pos passed in for this method)
        //      volume and pitch are adjusted
        //      similar behavior for bgm/bgs - if file/volume/pitch are all the same the new pos is ignored and nothing happens
        me.play(file, volume, pitch, 0);
    }

    @JRubyMethod(meta = true)
    public static void me_fade(IRubyObject recv, IRubyObject arg0) {
        me.fade(RubyNumeric.num2long(arg0));
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
