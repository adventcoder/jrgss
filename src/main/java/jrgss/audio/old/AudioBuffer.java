package jrgss.audio.old;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.google.common.primitives.Longs;

import lombok.Getter;

public class AudioBuffer {
    private final byte[] data;
    private final AudioFormat format;
    private @Getter long loopStart;
    private @Getter long loopEnd;

    private AudioBuffer(AudioInputStream stream) throws IOException {
        format = stream.getFormat();

        if (getFrameSize() == AudioSystem.NOT_SPECIFIED)
            throw new IOException("unknown frame size");

        if (stream.getFrameLength() == AudioSystem.NOT_SPECIFIED) {
            data = stream.readAllBytes();
        } else {
            data = stream.readNBytes(Math.toIntExact(stream.getFrameLength()*getFrameSize()));
        }

        this.loopStart = 0;
        this.loopEnd = data.length;
    }

    public long getByteLength() {
        return data.length;
    }

    public int getFrameSize() {
        return format.getFrameSize();
    }

    public AudioFormat getFormat(float pitch) {
        if (pitch == 1f) return format;
        return new AudioFormat(format.getEncoding(), format.getSampleRate()*pitch, format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), format.getFrameRate()*pitch, format.isBigEndian());
    }

    public int writeToLine(SourceDataLine line, long start, long end) {
        int off = Math.toIntExact(start);
        int len = Math.toIntExact(end) - off;
        return line.write(data, off, len);
    }

    public static AudioBuffer read(File file) throws IOException, UnsupportedAudioFileException {
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
        AudioBuffer buffer = new AudioBuffer(getAudioInputStream(file));
        buffer.setLoopPoints(fileFormat);
        return buffer;
    }

    private static AudioInputStream getAudioInputStream(File file) throws IOException, UnsupportedAudioFileException {
        AudioInputStream stream = AudioSystem.getAudioInputStream(file);
        AudioFormat format = stream.getFormat();
        if (format.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED || format.getEncoding() == AudioFormat.Encoding.ULAW || format.getEncoding() == AudioFormat.Encoding.ALAW) {
            format = new AudioFormat(format.getSampleRate(), 16, format.getChannels(), true, format.isBigEndian());
            stream = AudioSystem.getAudioInputStream(format, stream);
        }
        return stream;
    }

    public void setLoopPoints(AudioFileFormat fileFormat) {
        Long oggLoopStart = null;
        Long oggLoopLength = null;
        Long oggLoopEnd = null;

        for (Map.Entry<String, Object> prop : fileFormat.properties().entrySet()) {
            if (prop.getKey().startsWith("ogg.comment.ext.")) {
                String comment = prop.getValue().toString();
                if (!comment.contains("=")) continue;

                String[] pair = comment.split("=", 2);
                switch (pair[0].toLowerCase()) {
                    case "loopstart" -> oggLoopStart = Longs.tryParse(pair[1]);
                    case "looplength" -> oggLoopLength = Longs.tryParse(pair[1]);
                    case "loopend" -> oggLoopEnd = Longs.tryParse(pair[1]);
                }
            }
        }

        // NOTE: this does not check for values out of range to match RGSS behavior
        //       when playing the rule is:
        //       if loopend is out of range it is ignored, playback goes to end of stream
        //       when playback reaches loopend it is set back to loopstart
        //       but if loopstart is out of range (or ahead of loopend) the playback just stops and it doesn't loop

        System.out.println("oggLoopStart: " + oggLoopStart);
        System.out.println("oggLoopLength: " + oggLoopLength);
        System.out.println("oggLoopEnd: " + oggLoopEnd);

        if (oggLoopStart != null) {
            this.loopStart = oggLoopStart*getFrameSize();
        }

        if (oggLoopLength != null) {
            this.loopEnd = this.loopStart + oggLoopLength*getFrameSize();
        } else if (oggLoopEnd != null) {
            this.loopEnd = oggLoopEnd*getFrameSize();
        }
    }
}
