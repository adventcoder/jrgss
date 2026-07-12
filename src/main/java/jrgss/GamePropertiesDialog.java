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
    private final GameProperties properties;

    private JCheckBox fullScreenBox;
    private JCheckBox musicBox;
    private JCheckBox soundBox;

    public GamePropertiesDialog(Frame frame, GameProperties properties) {
        super(frame, "Game Properties", true);
        this.properties = properties;

        setResizable(false);

        buildUI();
        pack();
        setLocationRelativeTo(frame);
    }

    private void buildUI() {
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(new EmptyBorder(6, 6, 6, 6));

        JTabbedPane tabs = new JTabbedPane();

        JPanel generalTab = new JPanel(new BorderLayout());
        generalTab.setOpaque(false);
        generalTab.setPreferredSize(new Dimension(320, 210));
        generalTab.setBorder(new EmptyBorder(12, 12, 12, 12));
        generalTab.setLayout(new BoxLayout(generalTab, BoxLayout.Y_AXIS));

        fullScreenBox = new JCheckBox("Launch in Full Screen");
        fullScreenBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        fullScreenBox.setOpaque(false);
        fullScreenBox.setSelected(properties.launchInFullScreen);

        musicBox = new JCheckBox("Play BGM and ME");
        musicBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        musicBox.setOpaque(false);
        musicBox.setSelected(properties.playMusic);

        soundBox = new JCheckBox("Play BGS and SE");
        soundBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        soundBox.setOpaque(false);
        soundBox.setSelected(properties.playSound);

        generalTab.add(fullScreenBox);
        generalTab.add(Box.createVerticalStrut(6));
        generalTab.add(musicBox);
        generalTab.add(Box.createVerticalStrut(6));
        generalTab.add(soundBox);

        tabs.addTab("General", generalTab);

        JPanel keyboardTab = new JPanel(new BorderLayout());
        keyboardTab.setOpaque(false);
        tabs.addTab("Keyboard", keyboardTab);

        contentPane.add(tabs, BorderLayout.CENTER);

        //
        // Buttons
        //
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            properties.launchInFullScreen = fullScreenBox.isSelected();
            properties.playMusic = musicBox.isSelected();
            properties.playSound = soundBox.isSelected();
            properties.apply(false);
            properties.save();

            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(okButton);
        buttons.add(cancelButton);

        contentPane.add(buttons, BorderLayout.SOUTH);

        setContentPane(contentPane);

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
    }
}
