package jrgss.audio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AudioConverter {
    public static AudioInputStream convert(AudioInputStream source, Mixer mixer, Class<? extends DataLine> sourceLineClass) throws UnsupportedAudioFileException {
        return convert(source, getSourceLineFormats(mixer, sourceLineClass));
    }

    private static Collection<AudioFormat> getSourceLineFormats(Mixer mixer, Class<? extends DataLine> sourceLineClass) {
        for (Line.Info info : mixer.getSourceLineInfo()) {
            if (info.getLineClass() == sourceLineClass)
                return Arrays.asList(((DataLine.Info) info).getFormats());
        }
        return Collections.emptyList();
    }

    public static AudioInputStream convert(AudioInputStream source, Collection<AudioFormat> targetFormats) throws UnsupportedAudioFileException {
        //TODO
        AudioFormat sourceFormat = source.getFormat();
        AudioFormat targetFormat = new AudioFormat(sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), true, sourceFormat.isBigEndian());
        if (!AudioSystem.isConversionSupported(targetFormat, sourceFormat))
            throw new UnsupportedAudioFileException();
        return AudioSystem.getAudioInputStream(targetFormat, source);
    }

    // private static ConversionPath getConversionPath(AudioFormat sourceFormat, Collection<AudioFormat> targetFormats) {

    //     AudioFormat bestTargetFormat = findBestTargetFormatSameEncoding(sourceFormat, targetFormats);
    //     if (bestTargetFormat != null)
    //         return bestTargetFormat;

    //     // two step conversion, first transcode then apply other conversions
    //     Set<AudioFormat.Encoding> targetEncodings = new HashSet<>();
    //     for (AudioFormat targetFormat : targetFormats)
    //         targetEncodings.add(targetFormat.getEncoding());

    //     for (AudioFormat.Encoding encoding : targetEncodings) {
    //         for (AudioFormat transcodedFormat : AudioSystem.getTargetFormats(encoding, sourceFormat)) {
    //             findBestTargetFormatSameEncoding(transcodedFormat, targetFormats);
    //         }
    //     }

    //     return bestTargetFormat;
    // }

    private static AudioFormat findBestTargetFormatSameEncoding(AudioFormat sourceFormat, Collection<AudioFormat> targetFormats) {
        AudioFormat bestTargetFormat = null;
        for (AudioFormat targetFormat : targetFormats) {
            if (targetFormat.getEncoding().equals(sourceFormat.getEncoding()) && AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
                if (bestTargetFormat == null || compareLoss(sourceFormat, targetFormat, bestTargetFormat) < 0)
                    bestTargetFormat = targetFormat;
            }
        }
        return bestTargetFormat;
    }

    private static int compareLoss(AudioFormat sourceFormat, AudioFormat targetFormat1, AudioFormat targetFormat2) {
        assert sourceFormat.getEncoding() == targetFormat1.getEncoding() && sourceFormat.getEncoding() == targetFormat2.getEncoding();

        int cmp = Integer.compare(channelLoss(sourceFormat, targetFormat1), channelLoss(sourceFormat, targetFormat2));
        if (cmp != 0) return cmp;

        cmp = Integer.compare(sampleDepthLoss(sourceFormat, targetFormat1), sampleDepthLoss(sourceFormat, targetFormat2));
        if (cmp != 0) return cmp;

        cmp = Double.compare(sampleRateLoss(sourceFormat, targetFormat1), sampleRateLoss(sourceFormat, targetFormat2));
        if (cmp != 0) return cmp;

        cmp = Integer.compare(endiannessLoss(sourceFormat, targetFormat1), endiannessLoss(sourceFormat, targetFormat2));
        return cmp;
    }

    private static int channelLoss(AudioFormat sourceFormat, AudioFormat targetFormat) {
        if (sourceFormat.getChannels() == AudioSystem.NOT_SPECIFIED) return Integer.MAX_VALUE;
        if (targetFormat.getChannels() == AudioSystem.NOT_SPECIFIED) return 0;
        return Math.max(sourceFormat.getChannels() - targetFormat.getChannels(), 0);
    }

    private static int sampleDepthLoss(AudioFormat sourceFormat, AudioFormat targetFormat) {
        // NOTE: this might not be meaningful if encoding isn't the same
        if (sourceFormat.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED) return Integer.MAX_VALUE;
        if (targetFormat.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED) return 0;
        return Math.max(sourceFormat.getSampleSizeInBits() - targetFormat.getSampleSizeInBits(), 0);
    }

    private static double sampleRateLoss(AudioFormat sourceFormat, AudioFormat targetFormat) {
        if (sourceFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED) return Double.POSITIVE_INFINITY;
        if (targetFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED) return 0.0;
        return Math.abs(Math.log(sourceFormat.getSampleRate() / targetFormat.getSampleRate()));
    }

    private static int endiannessLoss(AudioFormat sourceFormat, AudioFormat targetFormat) {
        return sourceFormat.isBigEndian() == targetFormat.isBigEndian() ? 1 : 0;
    }
}
