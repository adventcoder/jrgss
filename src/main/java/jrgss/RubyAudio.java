package jrgss;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

import com.google.common.util.concurrent.Uninterruptibles;

public class RubyAudio {
    public static void createAudioModule(Ruby runtime) {
        RubyModule mod = runtime.defineModule("Audio");
        RubySupport.Audio = mod;
        mod.defineAnnotatedMethods(RubyAudio.class);
    }

    private static AudioFormat playbackFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
    private static final float bufferTimeSeconds = 1.0f;

    private Thread thread;
    private AudioInputStream inputStream;
    private SourceDataLine line;
    private FloatControl gainControl;
    private FloatControl sampleRateControl;
    private long frameOffset;
    private File loopFile = null;
    private boolean opened = false;
    private volatile boolean running = false;

    private void open(File file, long pos) throws IOException, UnsupportedAudioFileException {
        if (opened) throw new IllegalStateException("already opened");

        // open the audio file to read from
        inputStream = AudioSystem.getAudioInputStream(file);
        System.out.println(inputStream.getFormat().properties());
        if (!inputStream.getFormat().matches(playbackFormat))
            inputStream = AudioSystem.getAudioInputStream(playbackFormat, inputStream);

        frameOffset = 0L;
        if (pos > 0) {
            inputStream.skip(pos);
            frameOffset += pos;
        }

        // then get a data line to write to, we let java select the mixer.
        // for now we get a new line every time instead of reusing, even if the format is the same.
        try {
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, playbackFormat);
            line = (SourceDataLine) AudioSystem.getLine(lineInfo);
            line.open(playbackFormat);
        } catch (LineUnavailableException ex) {
            try {
                inputStream.close();
            } catch (IOException ioe) {
                ex.addSuppressed(ioe);
            }
            throw new IOException(ex);
        }

        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN))
            gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        if (line.isControlSupported(FloatControl.Type.SAMPLE_RATE))
            sampleRateControl = (FloatControl) line.getControl(FloatControl.Type.SAMPLE_RATE);

        this.opened = true;
    }

    public void close() throws IOException {
        if (!opened) return;
        if (running) throw new IllegalStateException("playback must be stopped before closing");
        try {
            inputStream.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        line.close();
        line = null;
        gainControl = null;
        sampleRateControl = null;
        inputStream = null;
        opened = false;
    }

    public long pos() {
        if (!opened) return 0L;
        return line.getFramePosition() - frameOffset;
    }

    public void setVolume(int volume) {
        if (gainControl == null) return;
        //TODO
    }

    public void setPitch(int pitch) {
        if (sampleRateControl == null) return;
        //TODO
    }

    public void start() {
        if (!opened) throw new IllegalStateException("audio must be opened before playback can start");
        if (running) return;
        running = true;
        thread = new Thread(this::audioLoop, getClass().getSimpleName() + "Thread");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        if (!running) return;
        running = false;
        Uninterruptibles.joinUninterruptibly(thread);
        thread = null;
    }

    public void fade(long millis) {
        if (!running) return;
        //TODO
    }

    private void audioLoop() {
        try {
            byte[] buffer = new byte[calculateBufferSize(playbackFormat)];

            line.start();
            try {
                while (running) {
                    int bytesRead = read(buffer);
                    if (bytesRead == -1) break;
                    if (bytesRead == 0) {
                        Thread.yield();
                        continue;
                    }
                    line.write(buffer, 0, bytesRead);
                }
            } finally {
                line.stop();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static int calculateBufferSize(AudioFormat format) {
        int frames = (int) Math.ceil(format.getFrameRate() * bufferTimeSeconds);
        return frames * format.getFrameSize();
    }

    private int read(byte[] buffer) throws IOException, UnsupportedAudioFileException {
        int bytesRead = inputStream.read(buffer);
        if (bytesRead == -1 && loopFile != null) {
            // line.drain();
            // frameOffset = line.getLongFramePosition();

            // inputStream.close();

            // inputStream = AudioSystem.getAudioInputStream(loopFile);
            // if (!inputStream.getFormat().matches(playbackFormat))
            //     inputStream = AudioSystem.getAudioInputStream(playbackFormat, inputStream);

            // bytesRead = inputStream.read(buffer);
        }
        return bytesRead;
    }

    private static RubyAudio bgm = new RubyAudio();

    @JRubyMethod(meta = true, required = 1, optional = 3)
    public static void bgm_play(IRubyObject recv, IRubyObject... args) throws Exception {
        File file = RTP.findFile(recv.getRuntime().getCurrentDirectory(), args[0].asJavaString());
        int volume = args.length >= 2 ? RubyNumeric.num2int(args[1]) : 100;
        int pitch = args.length >= 3 ? RubyNumeric.num2int(args[2]) : 100;
        long pos = args.length >= 4 ? RubyNumeric.num2long(args[3]) : 0L;

        bgm.stop();
        bgm.close();

        bgm.open(file, pos);
        bgm.setVolume(volume);
        bgm.setPitch(pitch);
        bgm.start();
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
}
