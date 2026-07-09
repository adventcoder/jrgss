package jrgss;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyAudio {
    public static void createAudioModule(Ruby runtime) {
        RubyModule mod = runtime.defineModule("Audio");
        RubySupport.audioModule = mod;
        mod.defineAnnotatedMethods(RubyAudio.class);
    }

    private static Map<File, Clip> clipCache = new HashMap<>();

    public static void setup(GameProperties props) {

    }

    public static void teardown() {
        for (Clip clip : clipCache.values())
            clip.stop();
        clipCache.clear();
    }

    @JRubyMethod(meta = true, required = 1, optional = 2)
    public static void se_play(IRubyObject recv, IRubyObject... args) {
        File file = RTP.findFile(recv.getRuntime(), args[0].asJavaString());

        Clip clip = getClip(recv.getRuntime(), file);
        clip.stop();

        if (args.length >= 2)
            setVolume(clip, RubyNumeric.num2int(args[1]));
        if (args.length >= 3)
            setPitch(clip, RubyNumeric.num2int(args[2]));

        clip.setFramePosition(0);
        clip.start();
    }

    @JRubyMethod(meta = true)
    public static void se_stop(IRubyObject recv) {
        for (Clip clip : clipCache.values())
            clip.stop();
    }

    private static Clip getClip(Ruby runtime, File file) {
        return clipCache.computeIfAbsent(file, k -> loadClip(runtime, file));
    }

    private static Clip loadClip(Ruby runtime, File file) {
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(file));
            return clip;
        } catch (IOException ex) {
            throw runtime.newIOErrorFromException(ex);
        } catch (UnsupportedAudioFileException ex) {
            throw RubySupport.newRGSSError(runtime, "unsupported audio file: " + file);
        } catch (LineUnavailableException ex) {
            throw RubySupport.newRGSSError(runtime, "line unavailable");
        }
    }

    private static void setVolume(Clip clip, int volume) {
        FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        if (volumeControl == null) return;

        volume = Math.min(Math.max(volume, 0), 100);

        if (volume == 0) {
            volumeControl.setValue(volumeControl.getMinimum());
            return;
        }

        float min = volumeControl.getMinimum();
        float max = volumeControl.getMaximum();

        // Convert linear percentage to logarithmic dB
        float gain = (float) (20.0 * Math.log10(volume / 100.0));
        gain = Math.max(min, Math.min(max, gain));

        volumeControl.setValue(gain);
    }

    private static void setPitch(Clip clip, int volume) {

    }
}
