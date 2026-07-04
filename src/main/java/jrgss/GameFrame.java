package jrgss;

import java.awt.Canvas;

import javax.swing.JFrame;

public class GameFrame extends JFrame {
    public GameFrame(String title, Canvas screen) {
        setTitle(title);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        add(screen);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
