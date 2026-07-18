package jrgss.audio;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioCache {
    private static final int MAX_ENTRY_SIZE = 44100 * 4 * 180; // 3 minutes, sterio, 16 bits per sample @ 44100 Hz
    private static final int MAX_TOTAL_SIZE = MAX_ENTRY_SIZE * 20;

    private final LinkedHashMap<File, AudioBuffer> cache = new LinkedHashMap<>(16, 0.75f, true);
    private int totalSize = 0;

    public AudioBuffer get(File file) throws IOException, UnsupportedAudioFileException {
        AudioBuffer buffer = cache.get(file);
        if (buffer != null) return buffer;

        buffer = AudioBuffer.read(file);
        if (buffer.getMemorySize() < MAX_ENTRY_SIZE) {
            evict(MAX_TOTAL_SIZE - buffer.getMemorySize());

            cache.put(file, buffer);
            totalSize += buffer.getMemorySize();
        }

        return buffer;
    }

    private void evict(long targetSize) {
        Iterator<Map.Entry<File, AudioBuffer>> it = cache.entrySet().iterator();
        while (totalSize > targetSize && it.hasNext()) {
            Map.Entry<File, AudioBuffer> eldest = it.next();
            totalSize -= eldest.getValue().getMemorySize();
            it.remove();
        }
    }
}