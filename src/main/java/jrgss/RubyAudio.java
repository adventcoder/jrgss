package jrgss;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

import jrgss.audio.AudioPlayer;

public class RubyAudio {
    public static void createAudioModule(Ruby runtime) {
        RubyModule mod = runtime.defineModule("Audio");
        RubySupport.Audio = mod;
        mod.defineAnnotatedMethods(RubyAudio.class);
    }

    private static class PlayState {
        private File file;
        private AudioPlayer player;
    }

    private PlayState background;
    private Set<PlayState> effects = new HashSet<>();

    public RubyAudio() {
    }

    public PlayState playBackground(File file, int volume, int pitch, int pos) {
        return null;
    }

    public PlayState playEffect(File file, int volume, int pitch) {
        return null;
    }

    private static final RubyAudio music = new RubyAudio();
    private static final RubyAudio sound = new RubyAudio();

    @JRubyMethod(meta = true, required = 1, optional = 3)
    public static void bgm_play(IRubyObject recv, IRubyObject... args) throws Exception {
        File file = RTP.findFile(recv.getRuntime().getCurrentDirectory(), args[0].asJavaString());
        int volume = args.length >= 2 ? RubySupport.numToIntInRangeClamped(args[1], 0, 200) : 100;
        int pitch = args.length >= 3 ? RubySupport.numToIntInRangeClamped(args[2], 50, 150) : 100;
        int pos = args.length >= 4 ? RubyNumeric.num2int(args[3]) : 0;
    }

    @JRubyMethod(meta = true)
    public static void bgm_stop(IRubyObject recv) {
    }

    @JRubyMethod(meta = true)
    public static void bgm_fade(IRubyObject recv, IRubyObject arg0) {
    }

    @JRubyMethod(meta = true)
    public static IRubyObject bgm_pos(IRubyObject recv) {
        return recv.getRuntime().newFixnum(0);
    }

    @JRubyMethod(meta = true, required = 1, optional = 3)
    public static void me_play(IRubyObject recv, IRubyObject... args) throws Exception {
        File file = RTP.findFile(recv.getRuntime().getCurrentDirectory(), args[0].asJavaString());
        int volume = args.length >= 2 ? RubySupport.numToIntInRangeClamped(args[1], 0, 200) : 100;
        int pitch = args.length >= 3 ? RubySupport.numToIntInRangeClamped(args[2], 50, 150) : 100;
    }

    @JRubyMethod(meta = true)
    public static void me_stop(IRubyObject recv) {
    }

    @JRubyMethod(meta = true)
    public static void me_fade(IRubyObject recv, IRubyObject arg0) {
    }

    @JRubyMethod(meta = true)
    public static IRubyObject me_pos(IRubyObject recv) {
        return recv.getRuntime().newFixnum(0);
    }

    @JRubyMethod(meta = true, required = 1, optional = 3)
    public static void bgs_play(IRubyObject recv, IRubyObject... args) throws Exception {
        File file = RTP.findFile(recv.getRuntime().getCurrentDirectory(), args[0].asJavaString());
        int volume = args.length >= 2 ? RubySupport.numToIntInRangeClamped(args[1], 0, 200) : 100;
        int pitch = args.length >= 3 ? RubySupport.numToIntInRangeClamped(args[2], 50, 150) : 100;
        int pos = args.length >= 4 ? RubyNumeric.num2int(args[3]) : 0;
    }

    @JRubyMethod(meta = true)
    public static void bgs_stop(IRubyObject recv) {
    }

    @JRubyMethod(meta = true)
    public static void bgs_fade(IRubyObject recv, IRubyObject arg0) {
    }

    @JRubyMethod(meta = true)
    public static IRubyObject bgs_pos(IRubyObject recv) {
        return recv.getRuntime().newFixnum(0);
    }

    @JRubyMethod(meta = true, required = 1, optional = 3)
    public static void se_play(IRubyObject recv, IRubyObject... args) throws Exception {
        File file = RTP.findFile(recv.getRuntime().getCurrentDirectory(), args[0].asJavaString());
        int volume = args.length >= 2 ? RubySupport.numToIntInRangeClamped(args[1], 0, 200) : 100;
        int pitch = args.length >= 3 ? RubySupport.numToIntInRangeClamped(args[2], 50, 150) : 100;
    }

    @JRubyMethod(meta = true)
    public static void se_stop(IRubyObject recv) {
    }
}
