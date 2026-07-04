package jrgss;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.SwingUtilities;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyGraphics {
    public static void createGraphicsModule(Ruby runtime) {
        RubyModule mod = runtime.defineModule("Graphics");
        RGSS.graphicsModule = mod;
        mod.defineAnnotatedMethods(RubyGraphics.class);
    }

    private static Canvas canvas;

    private static AtomicBoolean resetRequested = new AtomicBoolean(false);

    private static ReentrantLock pauseLock = new ReentrantLock();
    private static Condition unpaused = pauseLock.newCondition();
    private static boolean paused = true;

    private static int frameRate = 60;
    private static long frameCount = 0;
    private static long frameStartTime = 0;

    public static void init() {
        canvas = new Canvas();
        canvas.setFocusable(false);
        canvas.setBackground(Color.BLACK);
        canvas.setIgnoreRepaint(true);
        canvas.setPreferredSize(new Dimension(544, 416));
        reset();
    }

    public static void reset() {
        clear();
        frameRate = 60;
        frameCount = 0;
        frameStartTime = System.nanoTime();
    }

    public static Frame createFrame(String title) {
        Thread thread = Thread.currentThread();
        GameFrame frame = new GameFrame(title, canvas);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                thread.interrupt();
                ThreadSupport.runUninterruptibly(thread::join);
                frame.dispose();
            }

            @Override
            public void windowActivated(WindowEvent e) {
                setPaused(false);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                setPaused(true);
            }
        });
        return frame;
    }

    private static void setPaused(boolean paused) {
        pauseLock.lock();
        try {
            RubyGraphics.paused = paused;
            if (!paused) unpaused.signalAll();
        } finally {
            pauseLock.unlock();
        }
    }

    public static @JRubyMethod(meta = true) IRubyObject frame_rate(IRubyObject recv) {
        return recv.getRuntime().newFixnum(frameRate);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject set_frame_rate(IRubyObject recv, IRubyObject obj) {
        frameRate = Math.min(Math.max(RubyNumeric.num2int(obj), 10), 120);
        return obj;
    }

    public static @JRubyMethod(meta = true) IRubyObject frame_count(IRubyObject recv) {
        return recv.getRuntime().newFixnum(frameCount);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject set_frame_count(IRubyObject recv, IRubyObject obj) {
        frameCount = RubyNumeric.num2long(obj);
        return obj;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject width(IRubyObject recv) {
        return recv.getRuntime().newFixnum(canvas.getWidth());
    }

    @JRubyMethod(meta = true)
    public static IRubyObject height(IRubyObject recv) {
        return recv.getRuntime().newFixnum(canvas.getHeight());
    }

    @JRubyMethod(meta = true)
    public static void resize_screen(IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        int width = RubyNumeric.num2int(arg0);
        int height = RubyNumeric.num2int(arg1);
        canvas.setPreferredSize(new Dimension(width, height));
        Window window = SwingUtilities.getWindowAncestor(canvas);
        if (window != null) {
            Point pos = window.getLocation();
            Point center = new Point(pos.x + window.getWidth() / 2, pos.y + window.getHeight() / 2);
            window.pack();
            window.setLocation(center.x - window.getWidth() / 2, center.y - window.getHeight() / 2);
        }
    }

    @JRubyMethod(meta = true)
    public static void frame_reset(IRubyObject recv) {
        frameStartTime = System.nanoTime();
    }

    @JRubyMethod(meta = true)
    public static void update(IRubyObject recv) throws InterruptedException {
        render();
        endFrame();
        reset(recv.getRuntime());
        pause();
    }

    private static void endFrame() throws InterruptedException {
        long desiredFrameTime = 1000_000_000L / frameRate;
        long frameTime = System.nanoTime() - frameStartTime;

        // if there's time left, wait until the end of the frame
        if (frameTime < desiredFrameTime) {
            ThreadSupport.sleep(desiredFrameTime - frameTime);
            frameTime = desiredFrameTime;
        }

        // advance to the next frame
        frameStartTime += frameTime;
        frameCount++;
    }

    private static void reset(Ruby runtime) {
        if (resetRequested.compareAndExchange(true, false))
            throw RGSS.newReset(runtime);
    }

    private static void pause() {
        pauseLock.lock();
        try {
            if (paused)
                ThreadSupport.runUninterruptibly(unpaused::await);
        } finally {
            pauseLock.unlock();
        }
    }

    @JRubyMethod(meta = true)
    public static IRubyObject snap_to_bitmap(IRubyObject recv) {
        RubyBitmap bmp = RubyBitmap.newBitmap(recv.getRuntime(), canvas.getWidth(), canvas.getHeight());
        Graphics2D g = bmp.getGraphics();
        g.setColor(canvas.getBackground());
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        render(g);
        return bmp;
    }

    public static void clear() {
        Graphics g = canvas.getGraphics();
        if (g != null) {
            g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g.dispose();
        }
    }

    private static void render() {
        BufferStrategy bs = canvas.getBufferStrategy();
        if (bs == null) {
            canvas.createBufferStrategy(2);
            bs = canvas.getBufferStrategy();
        }
        Graphics2D g = (Graphics2D) bs.getDrawGraphics();
        render(g);
        g.dispose();
        bs.show();
    }

    public static void render(Graphics2D g) {
        g.setColor(Color.getHSBColor(frameCount % 360 / 360.0f, 1.0f, 1.0f));
        g.fillOval(0, 0, canvas.getWidth(), canvas.getHeight());
    }
}
