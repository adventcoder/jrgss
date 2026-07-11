package jrgss;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.UIManager;

import org.ini4j.Ini;

import lombok.Getter;

public class Game extends Canvas {
    //TODO: replace stderr printing with logger
    public static final Logger logger = Logger.getLogger(Game.class.getName());

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        Ini ini = loadIni();
        String title = Objects.requireNonNullElse(ini.get("Game", "Title"), "Untitled");
        //String scriptsPath = ini.get("Game", "Scripts");

        Game game = new Game(title);

        GameFrame frame = new GameFrame(game);
        frame.setVisible(true);
        frame.awaitOpen();

        try {
            game.requestFocus();
            game.createBufferStrategy(2);

            setupRTP(ini, game);
            setupFonts();

            try (ScriptEngine scriptEngine = new ScriptEngine(game)) {
                scriptEngine.runScripts();
            }
        } finally {
            frame.dispose();
        }

        //
        System.exit(0);
    }

    private static Ini loadIni() {
        Ini ini = new Ini();
        File iniFile = getIniFile();
        if (iniFile.exists()) {
            try {
                ini.load(iniFile);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return ini;
    }

    private static File getIniFile() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null)
            return FileSupport.replaceSuffix(new File(appPath), "ini");
        // logger.warning("app-path not set, using default ini location");
        return new File("Game.ini");
    }

    private static void setupRTP(Ini ini, Game game) {
        String rtpName = ini.get("Game", "RTP");
        if (rtpName == null || rtpName.isEmpty()) return;

        String rtpPath = RTP.getInstalledPath(rtpName);
        if (rtpPath == null) {
            game.showMessageDialog(rtpName + " RTP is required to run this game.", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        RTP.PATH.addLast(rtpPath);
    }

    private static void setupFonts() {
        for (File file : RTP.listDir(null, "Fonts")) {
            String suffix = FileSupport.getSuffix(file);
            if (suffix == null || !suffix.matches("ttf|otf"))
                continue;

            Font font = null;
            try {
                font = Font.createFont(Font.TRUETYPE_FONT, file);
            } catch (IOException | FontFormatException ex) {
                ex.printStackTrace();
                continue;
            }

            if (GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font))
                System.err.println("Registered font " + font.getName());
        }
    }

    public GameFrame frame = null;
    private final @Getter String title;
    private final @Getter KeyboardState.Async asyncKeyboardState = new KeyboardState.Async(this);
    private final AtomicBoolean reset = new AtomicBoolean(false);

    public boolean postReset() {
        return reset.compareAndSet(false, true);
    }

    public boolean pollReset() {
        return reset.compareAndSet(true, false);
    }

    public Game(String title) {
        this.title = title;
        setPreferredSize(new Dimension(544, 416));
        setBackground(Color.BLACK);
        setIgnoreRepaint(true);

        initKeyboardShortcuts();
        initCursorHiding();
    }

    private void initKeyboardShortcuts() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_F1 -> {
                        GamePropertiesDialog dialog = new GamePropertiesDialog(frame);
                        dialog.setVisible(true);
                    }
                    case KeyEvent.VK_F2 -> {
                        frame.setFpsShowing(!frame.isFpsShowing());
                    }
                    case KeyEvent.VK_F12 -> {
                        if (!postReset())
                            System.err.println("Ignoring repeated reset");
                    }
                }
            }
        });
    }

    private void initCursorHiding() {
        BufferedImage transparentImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Cursor transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(transparentImage, new Point(0, 0), "transparent");

        Timer hideCursorTimer = new Timer(500, ae -> setCursor(transparentCursor));
        hideCursorTimer.setRepeats(false);

        MouseAdapter MouseAdapter = new MouseAdapter() {
            public @Override void mouseEntered(MouseEvent e) { mouseActivated(e); }
            public @Override void mousePressed(MouseEvent e) { mouseActivated(e); }
            public @Override void mouseReleased(MouseEvent e) { mouseActivated(e); }
            public @Override void mouseMoved(MouseEvent e) { mouseActivated(e); }
            public @Override void mouseDragged(MouseEvent e) { mouseActivated(e); }

            private void mouseActivated(MouseEvent e) {
                setCursor(null);
                hideCursorTimer.restart();
            }
        };
        addMouseListener(MouseAdapter);
        addMouseMotionListener(MouseAdapter);
    }

    public void clear() {
        Graphics g = getGraphics();
        g.clearRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }

    public void showMessageDialog(String message, int messageType) {
        showMessageDialog(message, messageType, 20, 50);
    }

    public void showMessageDialog(String message, int messageType, int maxRows, int maxCols) {
        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(UIManager.getBorder("Label.border"));
        textArea.setForeground(UIManager.getColor("label.foreground"));
        textArea.setBackground(UIManager.getColor("Label.background"));
        textArea.setFont(UIManager.getFont("Label.font"));

        Color messageForeground = UIManager.getColor("OptionPane.messageForeground");
        if (messageForeground != null) textArea.setForeground(messageForeground);
        Font messageFont = UIManager.getFont("OptionPane.messageFont");
        if (messageFont != null) textArea.setFont(messageFont);

        // Calculate max height/width from rows/cols
        FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
        Insets insets = textArea.getInsets();
        int maxWidth = maxCols*fm.charWidth('m') + insets.left + insets.right;
        int maxHeight = maxRows*fm.getHeight() + insets.top + insets.bottom;

        // enable line wrapping if needed
        int uncappedWidth = textArea.getPreferredSize().width;
        if (uncappedWidth > maxWidth) {
            textArea.setSize(maxWidth, Integer.MAX_VALUE);
            textArea.setLineWrap(true);
        }

        // enable scrolling if needed
        JComponent messageComponent = textArea;
        int uncappedHeight = textArea.getPreferredSize().height;
        if (uncappedHeight > maxHeight) {
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(textArea.getWidth(), maxHeight));
            messageComponent = scrollPane;
        }

        JOptionPane.showMessageDialog(frame, messageComponent, title, messageType);
    }
}
