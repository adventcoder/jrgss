import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;

public class Test {

    public static void main(String[] args) throws Exception {
        AudioInputStream source = AudioSystem.getAudioInputStream(new File("ShipTest.ogg"));
        System.out.println(source.getFormat()); // VORBISENC 44100.0 Hz, unknown bits per sample, stereo, 1 bytes/frame, 24000.0 frames/second
        System.out.println(source.getFormat().getSampleRate());
        System.out.println(source.getFormat().getFrameRate());
        System.out.println();
        System.out.println(source.getFormat().properties());

        // Convert to 16 bit pcm signed samples that java sound can play (this produces static noise if I set big endian true, does this depend on native endianness?)
        AudioFormat sourceFormat = source.getFormat();
        AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels()*2, sourceFormat.getSampleRate(), sourceFormat.isBigEndian());
        if (!sourceFormat.matches(targetFormat)) {
            System.out.println("CONVERTING!");
            source = AudioSystem.getAudioInputStream(targetFormat, source);
            sourceFormat = source.getFormat();
            System.out.println(sourceFormat); // PCM_SIGNED 44100.0 Hz, 16 bit, stereo, 4 bytes/frame, little-endian
            System.out.println();
        }

        int pitch = 75; // allowed values 50 - 150
        if (pitch != 100) {
            float newSampleRate = sourceFormat.getSampleRate() * pitch / 100f;
            // reinterpret the stream with the same samples but adjusted sample rate
            source = new AudioInputStream(source, new AudioFormat(sourceFormat.getEncoding(), newSampleRate, sourceFormat.getSampleSizeInBits(), sourceFormat.getChannels(), sourceFormat.getFrameSize(), newSampleRate, sourceFormat.isBigEndian()), source.getFrameLength());
        }

        Clip clip = (Clip) AudioSystem.getLine(new DataLine.Info(Clip.class, source.getFormat()));
        //SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, source.getFormat())); // for streaming
        System.out.println(clip.getClass()); // class com.sun.media.sound.DirectAudioDevice$DirectClip
        clip.open(source);
        System.out.println("frame length: " + clip.getFrameLength()); // this is the same even 

        int volume = 40; // allowed values between 0 - 200
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            if (volume == 0) {
                gain.setValue(gain.getMinimum());
            } else {
                gain.setValue(Math.min(Math.max(linearToDb(volume / 100f), gain.getMinimum()), gain.getMaximum()));
            }
        }

        clip.setFramePosition(volume);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        clip.start();
        // clip.isRunning() is not immediately true...
        do {
            Thread.sleep(100);
            System.out.println(clip.getLongFramePosition() % clip.getFrameLength()); // is this right? what if there is a loop start/end set?
        } while (clip.isRunning());

        clip.close();
    }

    private static float linearToDb(float gain) {
        return 20f * (float) Math.log10(gain);
    }
}
