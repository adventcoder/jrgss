package jrgss;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class PropertiesDialog extends Dialog implements ActionListener {

    private Checkbox vsyncBox;
    private Checkbox musicBox;
    private Checkbox soundsBox;
    private Checkbox fullscreenBox;

    private Button okButton;
    private Button cancelButton;

    public PropertiesDialog(Frame owner) {
        super(owner, "Properties", true);

        setLayout(new BorderLayout());

        Panel centerPanel = new Panel(new GridLayout(4, 1));

        fullscreenBox = new Checkbox("Launch in Fullscreen", false);
        vsyncBox = new Checkbox("Enable VSync", false);
        musicBox = new Checkbox("Play Music", false);
        soundsBox = new Checkbox("Play Sounds", false);

        centerPanel.add(vsyncBox);
        centerPanel.add(musicBox);
        centerPanel.add(soundsBox);
        centerPanel.add(fullscreenBox);

        add(centerPanel, BorderLayout.CENTER);

        Panel buttonPanel = new Panel(new FlowLayout(FlowLayout.RIGHT));

        okButton = new Button("OK");
        cancelButton = new Button("Cancel");

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            System.out.println("fullscreen: " + fullscreenBox.getState());
            System.out.println("vsync: " + vsyncBox.getState());
            System.out.println("music: " + musicBox.getState());
            System.out.println("sounds: " + soundsBox.getState());
            dispose();
        } else if (e.getSource() == cancelButton) {
            dispose();
        }
    }
}
