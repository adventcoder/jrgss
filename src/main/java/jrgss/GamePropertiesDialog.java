package jrgss;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.KeyEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

public class GamePropertiesDialog extends JDialog {
    private final JCheckBox fullscreenBox;
    private final JCheckBox musicBox;
    private final JCheckBox soundsBox;

    public GamePropertiesDialog(Frame owner) {
        super(owner, "Game Properties", true);

        // GameProperties props = new GameProperties.load();

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(6, 6, 6, 6));

        JTabbedPane tabs = new JTabbedPane();

        JPanel general = new JPanel(new BorderLayout());
        general.setOpaque(false);
        general.setPreferredSize(new Dimension(320, 210));
        general.setBorder(new EmptyBorder(12, 12, 12, 12));
        general.setLayout(new BoxLayout(general, BoxLayout.Y_AXIS));

        fullscreenBox = new JCheckBox("Launch in Fullscreen");
        fullscreenBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        fullscreenBox.setOpaque(false);

        musicBox = new JCheckBox("Play Music");
        musicBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        musicBox.setOpaque(false);

        soundsBox = new JCheckBox("Play Sounds");
        soundsBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        soundsBox.setOpaque(false);

        general.add(fullscreenBox);
        general.add(Box.createVerticalStrut(6));
        general.add(musicBox);
        general.add(Box.createVerticalStrut(6));
        general.add(soundsBox);

        tabs.addTab("General", general);

        JPanel keyboard = new JPanel(new BorderLayout());
        keyboard.setOpaque(false);
        tabs.addTab("Keyboard", keyboard);

        root.add(tabs, BorderLayout.CENTER);

        //
        // Buttons
        //
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            System.out.println("fullscreen: " + fullscreenBox.isSelected());
            System.out.println("music: " + musicBox.isSelected());
            System.out.println("sounds: " + soundsBox.isSelected());

            // props.apply();
            // props.save();
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(okButton);
        buttons.add(cancelButton);

        root.add(buttons, BorderLayout.SOUTH);

        //
        // Enter = OK
        //
        getRootPane().setDefaultButton(okButton);

        //
        // Escape = Cancel
        //
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setContentPane(root);
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);
    }
}
