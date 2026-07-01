package jrgss;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
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

    private static int frameRate;
    private static long frameCount;
    private static long frameStartTime;

    public static void reset() {
        frameRate = 30;
        frameCount = 0;
        frameStartTime = System.nanoTime();
    }

    public static @JRubyMethod(meta = true) IRubyObject frame_rate(IRubyObject recv) { return recv.getRuntime().newFixnum(frameRate); }
    public static @JRubyMethod(meta = true) IRubyObject frame_count(IRubyObject recv) { return recv.getRuntime().newFixnum(frameCount); }

    @JRubyMethod(meta = true)
    public static IRubyObject set_frame_rate(IRubyObject recv, IRubyObject obj) {
        frameRate = Math.min(Math.max(RubyNumeric.num2int(obj), 10), 120);
        return obj;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject set_frame_count(IRubyObject recv, IRubyObject obj) {
        frameCount = RubyNumeric.num2long(obj);
        return obj;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject width(IRubyObject recv) {
        return recv.getRuntime().newFixnum(Game.screen.getWidth());
    }

    @JRubyMethod(meta = true)
    public static IRubyObject height(IRubyObject recv) {
        return recv.getRuntime().newFixnum(Game.screen.getHeight());
    }

    @JRubyMethod(meta = true)
    public static void resize_screen(IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        int width = RubyNumeric.num2int(arg0);
        int height = RubyNumeric.num2int(arg1);
        Game.screen.setPreferredSize(new Dimension(width, height));
        Game.repackFrame();
        Game.screen.getBufferStrategy().dispose();
        renderToScreen();
    }

    @JRubyMethod(meta = true)
    public static void frame_reset(IRubyObject recv) {
        frameStartTime = System.nanoTime();
    }

    @JRubyMethod(meta = true)
    public static void update(IRubyObject recv) throws InterruptedException {
        renderToScreen();

        long frameTime = System.nanoTime() - frameStartTime;
        frameStartTime += frameTime;
        frameCount++;

        long desiredFrameTime = 1000_000_000L / frameRate;
        long timeRemaining = desiredFrameTime - frameTime;
        if (timeRemaining > 0) {
            Game.frame.setTitle(String.format("Untitled - %d FPS", frameRate));
            Thread.sleep(timeRemaining / 1000_000L, (int) (timeRemaining % 1000_000L));
        } else {
            int fps = (int) (1000_000_000L / frameTime);
            Game.frame.setTitle(String.format("Untitled - %d FPS", fps));
        }
    }

    @JRubyMethod(meta = true)
    public static IRubyObject snap_to_bitmap(IRubyObject recv) {
        RubyBitmap bmp = RubyBitmap.newBitmap(recv.getRuntime(), Game.screen.getWidth(), Game.screen.getHeight());
        Graphics g = bmp.getGraphics();
        g.setColor(Game.screen.getBackground());
        g.fillRect(0, 0, Game.screen.getWidth(), Game.screen.getHeight());
        render(g);
        return bmp;
    }

    private static void renderToScreen() {
        BufferStrategy bs = Game.getBufferStrategy();
        Graphics g = bs.getDrawGraphics();
        try {
            render(g);
        } finally {
            g.dispose();
        }
        bs.show();
        Toolkit.getDefaultToolkit().sync();
    }

    private static void render(Graphics g) {
        g.setColor(Color.ORANGE);
        g.fillOval(0, 0, Game.screen.getWidth(), Game.screen.getHeight());
    }
}
