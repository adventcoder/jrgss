package jrgss;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

import jrgss.audio.AudioBuffer;
import jrgss.audio.AudioCache;
import jrgss.audio.AudioSource;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class RubyAudio {
    public static void createAudioModule(Ruby runtime) {
        RubyModule mod = runtime.defineModule("Audio");
        RubySupport.Audio = mod;
        mod.defineAnnotatedMethods(RubyAudio.class);
    }

    private static int globalMusicVolume = 100;
    private static int globalSoundVolume = 100;

    public static void setGlobalMusicVolume(int volume) {
        globalMusicVolume = volume;
        //TODO: update existing bgm and me
    }

    public static void setGlobalSoundVolume(int volume) {
        globalSoundVolume = volume;
        //TODO: update existing bgs and se
    }

    private enum Type {
        BGM, BGS, ME, SE;

        public boolean isBackground() {
            return this == BGM || this == BGS;
        }

        public boolean isMusic() {
            return this == BGM || this == ME;
        }

        public boolean isSound() {
            return this == BGS || this == SE;
        }

        public int adjustVolume(int volume) {
            if (isMusic()) return volume * globalMusicVolume / 100;
            if (isSound()) return volume * globalSoundVolume / 100;
            return volume;
        }
    };

    private static final AudioCache cache = new AudioCache();

    protected final Type type;

    public abstract void play(File file, int volume, int pitch, int pos) throws Exception;
    public abstract void stop();
    public abstract void fade(int millis);
    public abstract int pos();

    public static class Single extends RubyAudio {
        private AudioBuffer buffer = null;
        private AudioSource source = null;

        public Single(Type type) {
            super(type);
        }

        public void play(File file, int volume, int pitch, int pos) throws Exception {
            if (isPlaying(file, pitch)) {
                source.fadeVolume(volume, 0);
                //NOTE: RGSS ignores position here
                return;
            }

            if (source != null) {
                source.stop();
                source.close();
            }

            if (buffer == null || !file.equals(buffer.getFile()))
                buffer = cache.get(file);

            source = buffer.openSource(volume, pitch, pos, type.isBackground());
            source.start();
        }

        private boolean isPlaying(File file, int pitch) {
            return isPlaying() && file.equals(source.getBuffer().getFile()) && pitch == source.getPitch();
        }

        private boolean isPlaying() {
            return source != null && !source.isStopped();
        }

        @Override
        public void stop(){
            if (source == null) return;
            source.stop();
        }

        @Override
        public int pos() {
            return source == null ? 0 : source.getPosition();
        }

        @Override
        public void fade(int millis) {
            if (source == null) return;
            source.fadeVolume(0, millis);
        }
    }

    public static class Multi extends RubyAudio {
        private Set<AudioSource> sources = new HashSet<>();

        public Multi(Type type) {
            super(type);
        }

        @Override
        public void play(File file, int volume, int pitch, int pos) throws Exception {
            removeStopped();
            AudioBuffer buffer = cache.get(file);
            AudioSource source = buffer.openSource(volume, pitch, pos, type.isBackground());
            sources.add(source);
            source.start();
        }

        private void removeStopped() {
            Iterator<AudioSource> it = sources.iterator();
            while (it.hasNext()) {
                AudioSource source = it.next();
                if (source.isStopped()) {
                    source.close();
                    it.remove();
                }
            }
        }

        @Override
        public void stop() {
            sources.forEach(AudioSource::stop);
        }

        @Override
        public void fade(int millis) {
            sources.forEach(source -> source.fadeVolume(0, millis));
        }

        @Override
        public int pos() {
            throw new UnsupportedOperationException();
        }
    }

    private static final RubyAudio bgm = new Single(Type.BGM);
    private static final RubyAudio bgs = new Single(Type.BGS);
    private static final RubyAudio me = new Single(Type.ME);
    private static final RubyAudio se = new Multi(Type.SE);

    @JRubyMethod(meta = true, required = 1, optional = 3)
    public static void bgm_play(IRubyObject recv, IRubyObject... args) throws Exception {
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
    public static void bgs_play(IRubyObject recv, IRubyObject... args) throws Exception {
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

    @JRubyMethod(meta = true, required = 1, optional = 3)
    public static void me_play(IRubyObject recv, IRubyObject... args) throws Exception {
        File file = RTP.findFile(recv.getRuntime().getCurrentDirectory(), args[0].asJavaString());
        int volume = args.length >= 2 ? RubySupport.numToIntInRangeClamped(args[1], 0, 200) : 100;
        int pitch = args.length >= 3 ? RubySupport.numToIntInRangeClamped(args[2], 50, 150) : 100;
        me.play(file, volume, pitch, 0);
    }

    @JRubyMethod(meta = true)
    public static void me_stop(IRubyObject recv) {
        me.stop();
    }

    @JRubyMethod(meta = true)
    public static void me_fade(IRubyObject recv, IRubyObject arg0) {
        me.fade(RubyNumeric.num2int(arg0));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject me_pos(IRubyObject recv) {
        return recv.getRuntime().newFixnum(me.pos());
    }

    @JRubyMethod(meta = true, required = 1, optional = 3)
    public static void se_play(IRubyObject recv, IRubyObject... args) throws Exception {
        File file = RTP.findFile(recv.getRuntime().getCurrentDirectory(), args[0].asJavaString());
        int volume = args.length >= 2 ? RubySupport.numToIntInRangeClamped(args[1], 0, 200) : 100;
        int pitch = args.length >= 3 ? RubySupport.numToIntInRangeClamped(args[2], 50, 150) : 100;
        se.play(file, volume, pitch, 0);
    }

    @JRubyMethod(meta = true)
    public static void se_stop(IRubyObject recv) {
        se.stop();
    }

    @JRubyMethod(meta = true)
    public static void se_fade(IRubyObject recv, IRubyObject arg0) {
        se.fade(RubyNumeric.num2int(arg0));
    }
}
