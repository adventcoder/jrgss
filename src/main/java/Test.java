
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public class Test {

    public static void main(String[] args) throws Exception {
        //File file = new File("test/ShipTest.ogg");
        File file = new File("test/sample-3s.mp3");
        //File file = new File("test/ahem_x.wav");
        //File file = new File("test/Only-The-Lonely-2.mid");
        // File file = new File("test/sample-3s-8khz-ulaw.wav");
        int volume = 100; // allowed values 0 - 200
        int pitch = 100; // allowed values 50 - 150
        int startPos = 0;
        int loopCount = 0;

        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
        System.out.println("bits/sample: " + fileFormat.getFormat().getSampleSizeInBits());

        byte[] data;
        AudioFormat format;
        int frameSize;
        AudioInputStream stream = AudioSystem.getAudioInputStream(file);
        try {

            System.out.println("fileFormat: " + fileFormat);

            format = stream.getFormat();
            if (format.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED || format.getEncoding() == AudioFormat.Encoding.ULAW || format.getEncoding() == AudioFormat.Encoding.ALAW) {
                System.out.println("Decoding!");
                format = new AudioFormat(format.getSampleRate(), 16, format.getChannels(), true, format.isBigEndian());
                stream = AudioSystem.getAudioInputStream(format, stream);
            }

            frameSize = format.getFrameSize();
            if (frameSize == AudioSystem.NOT_SPECIFIED) throw new IOException("unknown frame size");

            int frameLength = Math.toIntExact(stream.getFrameLength());
            if (frameLength == AudioSystem.NOT_SPECIFIED) {
                data = stream.readAllBytes();
                frameLength = data.length / frameSize;
            } else {;
                data = stream.readNBytes(frameLength * frameSize);
            }
            System.out.println("frameLength: " + frameLength);
            System.out.println("byteLength: " + data.length);
        } finally {
            stream.close();
        }

        int oggLoopStart = -1;
        int oggLoopLength = -1;
        for (Map.Entry<String, Object> prop : fileFormat.properties().entrySet()) {
            if (prop.getKey().startsWith("ogg.comment.ext.")) {
                String comment = prop.getValue().toString();
                System.out.println(comment);
                if (comment.toLowerCase().startsWith("loopstart=")) {
                    oggLoopStart = Integer.parseUnsignedInt(comment.substring("loopstart=".length()));
                } else if (comment.toLowerCase().startsWith("looplength=")) {
                    oggLoopLength = Integer.parseUnsignedInt(comment.substring("looplength=".length()));
                }
            }
        }
        int loopStart = oggLoopStart >= 0 ? oggLoopStart*frameSize : 0;
        int loopEnd = oggLoopLength >= 0 ? loopStart + oggLoopLength*frameSize : data.length;
        System.out.println("loopStart: " + loopStart);
        System.out.println("loopEnd: " + loopEnd);
        System.out.println("end: " + data.length);
        System.out.println();

        if (pitch != 100) {
            float newSampleRate = format.getSampleRate() * pitch / 100f;
            float newFrameRate = format.getFrameRate() * pitch / 100f;
            format = new AudioFormat(format.getEncoding(), newSampleRate, format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), newFrameRate, format.isBigEndian());
        }


        DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, format);

        System.out.println("Available mixers:");
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(info);
            if (mixer.getSourceLineInfo().length > 0) {
                System.out.println("- " + info);
            }
        }
        System.out.println();

        Mixer defaultMixer = AudioSystem.getMixer(null);
        System.out.println("mixer: " + defaultMixer.getMixerInfo());
        for (Line.Info info : defaultMixer.getSourceLineInfo()) {
            if (info.getLineClass() == SourceDataLine.class) {
                DataLine.Info sourceLineInfo = (DataLine.Info) info;
                System.out.println("buffer size: " + sourceLineInfo.getMinBufferSize() + " - " + sourceLineInfo.getMaxBufferSize());
                for (AudioFormat mixerFormat : sourceLineInfo.getFormats()) {
                    System.out.println("- " + mixerFormat);
                }
            }
        }
        System.out.println("max lines: " + defaultMixer.getMaxLines(lineInfo));
        System.out.println();

        SourceDataLine line = (SourceDataLine) defaultMixer.getLine(lineInfo);
        line.open();
        try {
            FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            System.out.println(gainControl.getMinimum());
            System.out.println(gainControl.getMaximum());
            if (volume == 0) {
                gainControl.setValue(gainControl.getMinimum());
            } else if (volume <= 100) {
                float t = (volume - 1) / (float) (100 - 1);
                float gain = -35f * (1f - t);
                gainControl.setValue(Math.max(gain, gainControl.getMinimum()));
            }
            System.out.println("Volume: " + gainControl.getValue() + " dB");
            System.out.println();

            Thread thread = new Thread(() -> {
                line.start();
                try {
                    int pos = startPos;
                    int loopsRemaining = loopCount;
                    while (pos < loopStart)
                        pos += line.write(data, pos, loopStart - pos);
                    while (loopsRemaining > 0) {
                        while (pos < loopEnd)
                            pos += line.write(data, pos, loopEnd - pos);
                        pos = loopStart;
                        loopsRemaining -= 1;
                    }
                    while (pos < data.length)
                        pos += line.write(data, pos, data.length - pos);
                    line.drain();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    line.stop();
                }
            });
            thread.setDaemon(true);
            thread.start();

            System.out.println("---");
            while (thread.isAlive()) {
                System.out.println(line.getLongFramePosition());
                Thread.sleep(100);
            }
            System.out.println(line.getLongFramePosition());

        } finally {
            line.close();
        }
    }
}
