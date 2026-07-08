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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.UIManager;

public class Game extends Canvas implements Callable<Integer>, KeyboardState {
    private static Cursor transparentCursor;

    static {
        BufferedImage transparentImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(transparentImage, new Point(0, 0), "transparent");
    }

    public static void main(String[] args) throws Throwable {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        //TODO: ini parsing

        //setupRTP(rtpName);
        setupFonts();

        Game game = new Game("Untitled", new ParsedArgs(args));

        GameFrame frame = new GameFrame(game);
        frame.setVisible(true);
    }

    private static void setupFonts() {
        //TODO: integrate with RTP
        File fontsDir = new File("Fonts");

        String[] fileNames = fontsDir.list();
        if (fileNames == null) return;

        for (String fileName : fileNames) {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex <= 0) continue;

            String suffix = fileName.substring(dotIndex + 1);
            if (suffix.equalsIgnoreCase("ttf")) {
                File fontFile = new File(fontsDir, fileName);

                Font font = null;
                try {
                    font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                } catch (IOException | FontFormatException e) {
                    e.printStackTrace();
                    continue;
                }

                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            }
        }
    }

    public final String title;
    private final ParsedArgs args;

    public GameFrame frame = null;

    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final AtomicBoolean reset = new AtomicBoolean(false);

    private final ReentrantLock activeLock = new ReentrantLock();
    private final Condition activated = activeLock.newCondition();
    private volatile boolean active = false;

    public final Set<Integer> pressed = ConcurrentHashMap.newKeySet();

    private final Timer hideCursorTimer = new Timer(500, this::hideCursor);
    {
        hideCursorTimer.setRepeats(false);
    }

    public Game(String title, ParsedArgs args) {
        this.title = title;
        this.args = args;

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
                        reset();
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

    public void messageBox(String message, int messageType) {
        JOptionPane.showMessageDialog(frame, message, title, messageType);
    }

    public boolean stop() {
        return stop.compareAndSet(false, true);
    }

    public boolean reset() {
        return reset.compareAndSet(false, true);
    }

    public boolean pollStop() {
        return stop.compareAndSet(true, false);
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

    // this is called after the game window has opened
    @Override
    public Integer call() throws Exception {
        ScriptEngine scriptEngine = new ScriptEngine(this);
        scriptEngine.setGlobalVariable("$TEST", args.test || args.btest);
        scriptEngine.setGlobalVariable("$BTEST", args.btest);
        return scriptEngine.runScripts();
    }
}
