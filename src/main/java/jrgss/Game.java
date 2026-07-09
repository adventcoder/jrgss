package jrgss;

import java.awt.AWTEvent;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.UIManager;

import org.ini4j.Ini;

import com.google.common.base.MoreObjects;

public class Game extends Canvas implements KeyboardState {
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        Ini ini = loadIni();
        String title = MoreObjects.firstNonNull(ini.get("Game", "Title"), "Untitled");
        String scriptsPath = ini.get("Game", "Scripts");

        Game game = new Game(title);

        GameFrame frame = new GameFrame(game);
        frame.setVisible(true);

        game.requestFocus();
        game.createBufferStrategy(2);

        try {
            setupRTP(ini, game);
            setupFonts();

            ScriptEngine scriptEngine = new ScriptEngine(game);
            scriptEngine.loadScripts(scriptsPath);
            scriptEngine.runScripts();
        } finally {
            frame.dispose();
        }
    }

    private static Ini loadIni() {
        //TODO: Get this from the app path?
        // File appFile = new File(System.getProperty("jpackage.app-path"));
        // File iniFile = FileSupport.addSuffix(FileSupport.removeSuffix(appFile), "ini");
        File iniFile = new File("Game.ini");
        Ini ini = new Ini();
        if (iniFile.exists()) {
            try {
                ini.load(iniFile);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return ini;
    }

    private static void setupRTP(Ini ini, Game game) {
        String rtpName = ini.get("Game", "RTP");
        if (rtpName == null || rtpName.isEmpty()) return;

        Preferences rtpNode = Preferences.systemRoot().node("com/example/jrgss/rtp");
        String rtpPath = rtpNode.get(rtpName, null);
        if (rtpPath == null) {
            game.showMessageDialog("RTP not found: " + rtpName, JOptionPane.ERROR_MESSAGE);
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

    public final String title;

    public GameFrame frame = null;

    private final AtomicBoolean reset = new AtomicBoolean(false);

    private final ReentrantLock activeLock = new ReentrantLock();
    private final Condition activated = activeLock.newCondition();
    private boolean active = false;

    public final Set<Integer> pressed = ConcurrentHashMap.newKeySet();

    private final Cursor transparentCursor;
    {
        BufferedImage transparentImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(transparentImage, new Point(0, 0), "transparent");
    }

    private final Timer hideCursorTimer = new Timer(500, this::hideCursor);
    {
        hideCursorTimer.setRepeats(false);
    }

    public Game(String title) {
        this.title = title;
        setPreferredSize(new Dimension(544, 416));
        setBackground(Color.BLACK);
        setIgnoreRepaint(true);
        enableEvents(AWTEvent.KEY_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    @Override
    public void processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);
        switch (e.getID()) {
            case KeyEvent.KEY_PRESSED -> {
                pressed.add(e.getKeyCode());
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_F1 -> {
                        Dialog dialog = new GamePropertiesDialog(frame);
                        dialog.setVisible(true);
                    }
                    case KeyEvent.VK_F2 -> {
                        frame.toggleFpsShowing();
                    }
                    case KeyEvent.VK_F12 -> {
                        if (!reset())
                            System.err.println("Ignoring repeated reset");
                    }
                }
            }
            case KeyEvent.KEY_RELEASED -> {
                pressed.remove(e.getKeyCode());
            }
        }
    }

    @Override
    public void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        switch (e.getID()) {
            case MouseEvent.MOUSE_ENTERED, MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED -> {
                mouseActivated(e);
            }
        }
    }

    @Override
    public void processMouseMotionEvent(MouseEvent e) {
        super.processMouseEvent(e);
        switch (e.getID()) {
            case MouseEvent.MOUSE_MOVED, MouseEvent.MOUSE_DRAGGED -> {
                mouseActivated(e);
            }
        }
    }

    private void mouseActivated(MouseEvent e) {
        showCursor(e);
        hideCursorTimer.restart();
    }

    private void showCursor(MouseEvent e) {
        setCursor(null);
    }

    private void hideCursor(ActionEvent e) {
        setCursor(transparentCursor);
    }

    public void clear() {
        Graphics g = getGraphics();
        g.clearRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }

    public void showMessageDialog(String message, int messageType) {
        JOptionPane.showMessageDialog(frame, message, title, messageType);
    }

    public boolean reset() {
        return reset.compareAndSet(false, true);
    }

    public boolean pollReset() {
        return reset.compareAndSet(true, false);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        activeLock.lock();
        try {
            this.active = active;
            if (active)
                activated.signalAll();
        } finally {
            activeLock.unlock();
        }
    }

    public void awaitActive() throws InterruptedException {
        activeLock.lock();
        try {
            while (!active)
                activated.await();
        } finally {
            activeLock.unlock();
        }
    }

    @Override
    public boolean isPressed(int keyCode) {
        return pressed.contains(keyCode);
    }

    public KeyboardState getKeyboardState() {
        return new KeyboardState.Snapshot(pressed);
    }
}
