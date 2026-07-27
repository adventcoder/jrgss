package jrgss.audio;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.google.common.primitives.Ints;

import lombok.RequiredArgsConstructor;

public class AudioBuffer {
    public final short[] samples;
    public final int frameLength;
    public final int channels;
    public final float sampleRate;

    private AudioBuffer(short[] samples, int frameLength, int channels, float sampleRate) {
        this.samples = samples;
        this.frameLength = frameLength;
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    public AudioBuffer read(File file) throws UnsupportedAudioFileException, IOException {
        extractLoopData(file);
        return readSamples(file);
    }

    private static AudioBuffer readSamples(File file) throws UnsupportedAudioFileException, IOException {
        AudioInputStream stream = AudioSystem.getAudioInputStream(file);
        try {
            AudioFormat format = stream.getFormat();

            // use AudioSystem to convert to 16 bit signed pcm
            AudioFormat targetFormat = new AudioFormat(format.getSampleRate(), 16, format.getChannels(), true, format.isBigEndian());
            if (!format.matches(targetFormat)) {
                stream = AudioSystem.getAudioInputStream(targetFormat, stream);
                format = targetFormat;
            }

            int frameLength = Math.toIntExact(stream.getFrameLength());

            int bufferSamples = 256 * format.getChannels();
            ByteBuffer buffer = ByteBuffer.allocate(bufferSamples * 2);
            buffer.order(format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

            short[] samples;
            int sampleSize = 0;
            if (frameLength == AudioSystem.NOT_SPECIFIED) {
                samples = new short[4096 * format.getChannels()];
                while (true) {
                    int bytesRead = stream.read(buffer.array(), 0, buffer.capacity());
                    if (bytesRead == -1) break;
                    buffer.limit(bytesRead);
                    while (buffer.hasRemaining()) {
                        if (sampleSize == samples.length)
                            samples = Arrays.copyOf(samples, Math.multiplyExact(samples.length, 2));
                        samples[sampleSize++] = buffer.getShort();
                    }
                    buffer.clear();
                }
            } else {
                samples = new short[Math.multiplyExact(frameLength, format.getChannels())];
                while (sampleSize < samples.length) {
                    int bytesRead = stream.read(buffer.array(), 0, Math.min((samples.length - sampleSize) * 2, buffer.capacity()));
                    if (bytesRead == -1) break;
                    buffer.limit(bytesRead);
                    while (buffer.hasRemaining())
                        samples[sampleSize++] = buffer.getShort();
                    buffer.clear();
                }
            }

            return new AudioBuffer(samples, sampleSize / format.getChannels(), format.getChannels(), format.getSampleRate());
        } finally {
            stream.close();
        }
    }

    @RequiredArgsConstructor
    public static class LoopData {
        public final int startFrame;
        public final int endFrame;
    }

    public static LoopData extractLoopData(File file) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
        switch (fileFormat.getType().toString().toUpperCase()) {
            case "MIDI" -> {
                try {
                    return extractMidiLoopData(MidiSystem.getSequence(file));
                } catch (InvalidMidiDataException ex) {
                    throw new UnsupportedAudioFileException(ex.getMessage());
                }
            }
            case "OGG" -> extractOggLoopData(fileFormat);
        }
        return null;
    }

    private static LoopData extractOggLoopData(AudioFileFormat fileFormat) {
        Integer loopStart = null;
        Integer loopLength = null;
        for (Map.Entry<String, Object> prop : fileFormat.properties().entrySet()) {
            if (prop.getKey().startsWith("ogg.comment.ext.")) {
                String comment = prop.getValue().toString();
                String[] pair = comment.split("=");
                if (pair.length < 2) continue;
                switch (pair[0].toUpperCase()) {
                    case "LOOPSTART" -> loopStart = Ints.tryParse(pair[1]);
                    case "LOOPLENGTH" -> loopLength = Ints.tryParse(pair[2]);
                }
            }
        }

        if (loopStart != null) {
            if (loopLength != null)
                return new LoopData(loopStart.intValue(), loopStart.intValue() + loopLength.intValue());
            return new LoopData(loopStart.intValue(), fileFormat.getFrameLength());
        }
        if (loopLength != null)
            return new LoopData(0, loopLength.intValue());
        return null;
    }

    private static LoopData extractMidiLoopData(Sequence sequence) {
        return null;
    }
}
