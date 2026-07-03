package jrgss;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;

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

    public static GameScreen screen;

    private static volatile boolean running = true;
    private static boolean paused = false;
    private static Object pauseLock = new Object();

    private static int frameRate;
    private static long frameCount;
    private static long frameStartTime;

    public static void reset() {
        frameRate = 30;
        frameCount = 0;
        frameStartTime = System.nanoTime();
    }

    public static void stop() {
        running = false;
    }

    public static void setPaused(boolean paused) {
        synchronized (pauseLock) {
            RubyGraphics.paused = paused;
            pauseLock.notify();
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
        return recv.getRuntime().newFixnum(screen.getWidth());
    }

    @JRubyMethod(meta = true)
    public static IRubyObject height(IRubyObject recv) {
        return recv.getRuntime().newFixnum(screen.getHeight());
    }

    @JRubyMethod(meta = true)
    public static void resize_screen(IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        int width = RubyNumeric.num2int(arg0);
        int height = RubyNumeric.num2int(arg1);
        screen.resize(width, height);
    }

    @JRubyMethod(meta = true)
    public static void frame_reset(IRubyObject recv) {
        frameStartTime = System.nanoTime();
    }

    @JRubyMethod(meta = true)
    public static void update(IRubyObject recv) {
        renderToScreen();

        long desiredFrameTime = 1000_000_000L / frameRate;
        long frameTime = System.nanoTime() - frameStartTime;

        // if there's time left, wait until the end of the frame
        if (frameTime < desiredFrameTime) {
            frameDelay(desiredFrameTime - frameTime, recv.getRuntime());
            frameTime = desiredFrameTime;
        }

        // advance to the next frame
        frameStartTime += frameTime;
        frameCount++;
        screen.window.setFps((int) (1000_000_000L / frameTime));

        pauseWait(recv.getRuntime());
    }

    private static void frameDelay(long time, Ruby runtime) {
        try {
            Thread.sleep(time / 1000_000L, (int) (time % 1000_000L));
        } catch (InterruptedException e) {
            throw stopException(runtime);
        }
    }

    private static void pauseWait(Ruby runtime) {
        if (paused) {
            synchronized (pauseLock) {
                try {
                    while (paused) {
                        pauseLock.wait();
                    }
                } catch (InterruptedException e) {
                    throw stopException(runtime);
                }
            }
        }
    }

    private static RuntimeException stopException(Ruby runtime) {
        return running ? RGSS.newReset(runtime) : new RGSSStop();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject snap_to_bitmap(IRubyObject recv) {
        RubyBitmap bmp = RubyBitmap.newBitmap(recv.getRuntime(), screen.getWidth(), screen.getHeight());
        Graphics2D g = bmp.getGraphics();
        g.setColor(screen.getBackground());
        g.fillRect(0, 0, screen.getWidth(), screen.getHeight());
        render(g);
        return bmp;
    }

    public static void clearScreen() {
        screen.clear();
    }

    private static void renderToScreen() {
        BufferStrategy bs = screen.getBufferStrategy();
        Graphics2D g = (Graphics2D) bs.getDrawGraphics();
        try {
            render(g);
        } finally {
            g.dispose();
        }
        bs.show();
    }

    private static void render(Graphics2D g) {
        g.setColor(Color.getHSBColor(frameCount % 360 / 360.0f, 1.0f, 1.0f));
        g.fillOval(0, 0, screen.getWidth(), screen.getHeight());
    }
}
