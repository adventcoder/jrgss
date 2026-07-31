import java.io.File;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;

public class Test2 {

    public static void main(String[] args) throws Exception {
        File file = new File("test/Town3.mid");
        int pitch = 150;
        int volume = 50;

        MidiFileFormat fileFormat = MidiSystem.getMidiFileFormat(file);
        System.out.println("file type: " + fileFormat.getType());
        System.out.println("file length: " + fileFormat.getByteLength() + "bytes");
        System.out.println("file properites: " + fileFormat.properties());
        System.out.println();

        Sequence sequence = MidiSystem.getSequence(file);
        System.out.println("Tracks: " + sequence.getTracks().length);
        System.out.println("Tick Length: " + sequence.getTickLength());
        long loopStart = 0;
        for (Track track : sequence.getTracks()) {
            System.out.println("---");
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                if (event.getMessage() instanceof ShortMessage sm) {
                    if (sm.getCommand() == ShortMessage.CONTROL_CHANGE) {
                        if (sm.getData1() == 7 || sm.getData1() == 11 || sm.getData1() == 111)
                            System.out.println("[" + i + "] tick: " + event.getTick() + ", msg: [command: " + sm.getCommand() + ", data1: " + sm.getData1() + ", data2: " + sm.getData2() + " channel: " + sm.getChannel() + "]");
                        if (sm.getData1() == 111)
                            loopStart = event.getTick();
                    }
                }
            }
        }
        System.out.println();

        Synthesizer synth = MidiSystem.getSynthesizer();
        synth.open();
        try {
            setMasterVolume(synth.getReceiver(), volume / 100f);
            // for (MidiChannel channel : synth.getChannels()) {
            //     channel.controlChange(7, volume * 127 / 100);
            // }

            Sequencer sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            try {
                sequencer.getTransmitter().setReceiver(synth.getReceiver());
                sequencer.setSequence(sequence);
                sequencer.setTempoFactor(pitch / 100f);
                sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
                sequencer.setLoopStartPoint(loopStart);

                sequencer.start();
                while (sequencer.isRunning()) {
                    System.out.println("pos: " + sequencer.getTickPosition());
                    Thread.sleep(100);
                }
                System.out.println("pos: " + sequencer.getTickPosition());

            } finally {
                sequencer.close();
            }
        } finally {
            synth.close();
        }
    }

    private static void setMasterVolume(Receiver receiver, double volume) throws InvalidMidiDataException {
        volume = Math.max(0.0, Math.min(1.0, volume));

        int value = (int)(volume * 16383);

        byte[] bytes = {
            (byte)0xF0,
            0x7F,             // Universal real-time
            0x7F,             // Device ID: all devices
            0x04,             // Sub-ID #1: Device Control
            0x01,             // Sub-ID #2: Master Volume
            (byte)(value & 0x7F),
            (byte)((value >> 7) & 0x7F),
            (byte)0xF7
        };

        SysexMessage msg = new SysexMessage();
        msg.setMessage(bytes, bytes.length);
        receiver.send(msg, -1);
    }
}
