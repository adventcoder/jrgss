package jrgss;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.lang.reflect.InvocationTargetException;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyGraphics {
    public static void createGraphicsModule(Ruby runtime) {
        RubyModule mod = runtime.defineModule("Graphics");
        RubySupport.Graphics = mod;
        mod.defineAnnotatedMethods(RubyGraphics.class);
    }

    public static int frameRate;
    public static long frameCount;
    private static long frameStartTime;
    private static int fps;
    private static long totalFrameTime;

    static {
        reset();
    }

    public static void reset() {
        frameRate = 60;
        frameCount = 0;
        frameStartTime = System.nanoTime();
        fps = 0;
        totalFrameTime = 0;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject frame_rate(IRubyObject recv) {
        return recv.getRuntime().newFixnum(frameRate);
    }

    @JRubyMethod(meta = true, name = "frame_rate=")
    public static IRubyObject set_frame_rate(IRubyObject recv, IRubyObject obj) {
        frameRate = Math.min(Math.max(RubyNumeric.num2int(obj), 10), 120);
        return obj;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject frame_count(IRubyObject recv) {
        return recv.getRuntime().newFixnum(frameCount);
    }

    @JRubyMethod(meta = true, name = "frame_count=")
    public static IRubyObject set_frame_count(IRubyObject recv, IRubyObject obj) {
        frameCount = RubyNumeric.num2long(obj);
        return obj;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject width(IRubyObject recv) {
        Game game = RubySupport.getGame(recv.getRuntime());
        return recv.getRuntime().newFixnum(game.getWidth());
    }

    @JRubyMethod(meta = true)
    public static IRubyObject height(IRubyObject recv) {
        Game game = RubySupport.getGame(recv.getRuntime());
        return recv.getRuntime().newFixnum(game.getHeight());
    }

    @JRubyMethod(meta = true)
    public static void resize_screen(IRubyObject recv, IRubyObject arg0, IRubyObject arg1) throws InterruptedException, InvocationTargetException {
        Game game = RubySupport.getGame(recv.getRuntime());
        int width = RubyNumeric.num2int(arg0);
        int height = RubyNumeric.num2int(arg1);
        //TODO: somtimes this breaks rendering... also needs to clamp width/height
        EventQueue.invokeAndWait(() -> {
            game.setPreferredSize(new Dimension(width, height));
            game.frame.repack();
            game.createBufferStrategy(2);
        });
    }

    @JRubyMethod(meta = true)
    public static IRubyObject snap_to_bitmap(IRubyObject recv) {
        Game game = RubySupport.getGame(recv.getRuntime());

        RubyBitmap bmp = new RubyBitmap(recv.getRuntime(), RubySupport.Bitmap);
        bmp.initialize(recv.getRuntime().newFixnum(game.getWidth()), recv.getRuntime().newFixnum(game.getHeight()));

        Graphics2D g = bmp.getGraphics();
        g.setColor(game.getBackground());
        g.fillRect(0, 0, game.getWidth(), game.getHeight());
        render(g);

        return bmp;
    }

    @JRubyMethod(meta = true)
    public static void frame_reset(IRubyObject recv) {
        frameStartTime = System.nanoTime();
    }

    //TODO: handle interrupts?
    @JRubyMethod(meta = true)
    public static void update(IRubyObject recv) throws InterruptedException {
        Game game = RubySupport.getGame(recv.getRuntime());

        if (frameStartTime >= 0)
            endFrame(game);

        game.frame.awaitActive();

        startFrame(game, recv.getRuntime());
    }

    private static void startFrame(Game game, Ruby runtime) throws InterruptedException {
        frameStartTime = System.nanoTime();

        pollGameEvents(game, runtime);
    }

    private static void endFrame(Game game) throws InterruptedException {
        render(game.getBufferStrategy());

        long desiredFrameTime = 1000_000_000L / frameRate;
        long frameTime = System.nanoTime() - frameStartTime;

        // if there's time left, wait until the end of the frame
        long timeRemaining = desiredFrameTime - frameTime;
        if (timeRemaining > 0) {
            Thread.sleep(timeRemaining / 1000_000L, (int) (timeRemaining % 1000_000L));
            frameTime += timeRemaining;
        }

        frameCount++;
        updateFps(game, frameTime);
    }

    private static void pollGameEvents(Game game, Ruby runtime) {
        if (game.pollReset())
            throw RubySupport.newRGSSReset(runtime);
    }

    private static void updateFps(Game game, long frameTime) {
        totalFrameTime += frameTime;
        if (totalFrameTime >= 1000_000_000L) {
            game.frame.setFps(fps);
            fps = 0;
            totalFrameTime = 0;
        } else {
            fps++;
        }
    }

    public static void render(BufferStrategy bs) {
        Graphics2D g = (Graphics2D) bs.getDrawGraphics();
        try {
            render(g);
        } finally {
            g.dispose();
        }
        bs.show();
    }

    public static void render(Graphics2D g) {
        g.setColor(Color.getHSBColor(frameCount % 360 / 360.0f, 1.0f, 1.0f));
        g.fillOval(0, 0, 544, 416);
    }
}
