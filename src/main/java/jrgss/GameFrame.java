package jrgss;

import javax.swing.JFrame;

public class GameFrame extends JFrame {
    public GameFrame(String title) {
        setTitle(title);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}
