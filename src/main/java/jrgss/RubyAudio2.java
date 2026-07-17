package jrgss;

import java.io.File;

import org.jruby.runtime.builtin.IRubyObject;

import jrgss.audio.SampleBuffer;
import jrgss.audio.SampleSource;

public interface RubyAudio2 {
    public static void se_play(IRubyObject recv, File file, int volume, int pitch) {
    }

    public static void se_stop(IRubyObject recv) {

    }

    public static void main(String[] args) throws Exception {
        SampleBuffer buffer = SampleBuffer.read(new File("ShipTest.ogg"));
        System.out.println(buffer);

        SampleSource source = buffer.openSource(1.5f, false);
        source.setGain(0.5f);
        source.setFramePosition(2000000);
        source.start();

        while (source.isRunning()) {
            Thread.sleep(100);
            System.out.println(source.getFramePosition());
        }
        System.out.println(source.getFramePosition());
    }
}
