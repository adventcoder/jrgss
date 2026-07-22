package jrgss.audio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import jrgss.RTP;

public interface AudioBuffer {
    public AudioSource openSource(int pitch, int pos, boolean looping) throws LineUnavailableException;
    public File getFile();
    public int getMemorySize();

    public static AudioBuffer read(File file) throws IOException, UnsupportedAudioFileException {
        try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
            return read(stream, file);
        }
    }

    public static AudioBuffer read(InputStream stream, File file) throws IOException, UnsupportedAudioFileException {
        try {
            MidiFileFormat midiFormat = MidiSystem.getMidiFileFormat(stream);
            throw new UnsupportedOperationException("midi not supported: " + midiFormat);
        } catch (InvalidMidiDataException midiEx) {
            try {
                return SampledBuffer.read(stream, file);
            } catch (UnsupportedAudioFileException ex) {
                ex.addSuppressed(midiEx);
                throw ex;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        RTP.PATH.add(RTP.getInstallPath("RPGVXAce"));
        File airship = RTP.findFile((String) null, "Audio/BGM/Airship.ogg");
        File lonely = new File("Only-The-Lonely-2.mid");
        AudioBuffer buffer = read(lonely);
        System.out.println(buffer);
    }
}
