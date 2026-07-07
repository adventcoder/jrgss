package jrgss;

import java.awt.Color;
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
        RubySupport.graphicsModule = mod;
        mod.defineAnnotatedMethods(RubyGraphics.class);
    }

    public static int frameRate;
    public static long frameCount;
    private static long frameStartTime;

    static {
        //screen = Game.window.screen;
        reset();
    }

    public static void reset() {
        frameRate = 60;
        frameCount = 0;
        frameStartTime = System.nanoTime();
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
        //TODO: do something about this nested property access 
        return recv.getRuntime().newFixnum(Game.window.screen.getWidth());
    }

    @JRubyMethod(meta = true)
    public static IRubyObject height(IRubyObject recv) {
        //TODO: do something about this nested property access 
        return recv.getRuntime().newFixnum(Game.window.screen.getHeight());
    }

    @JRubyMethod(meta = true)
    public static void resize_screen(IRubyObject recv, IRubyObject arg0, IRubyObject arg1) throws InterruptedException, InvocationTargetException {
        int width = RubyNumeric.num2int(arg0);
        int height = RubyNumeric.num2int(arg1);
        EventQueue.invokeAndWait(() -> Game.window.resizeScreen(width, height));
        //TODO: do we need to rerender?
    }

    @JRubyMethod(meta = true)
    public static void frame_reset(IRubyObject recv) {
        frameStartTime = System.nanoTime();
    }

    @JRubyMethod(meta = true)
    public static void update(IRubyObject recv) throws InterruptedException {
        render();

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

        if (Game.clearStopping())
            throw new RGSSStop();
        if (Game.clearResetting())
            throw RubySupport.newRGSSReset(recv.getRuntime());

        Game.waitWhileInactive();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject snap_to_bitmap(IRubyObject recv) {
        RubyBitmap bmp = RubyBitmap.newBitmap(recv.getRuntime(), Game.window.screen.getWidth(), Game.window.screen.getHeight());
        Graphics2D g = bmp.getGraphics();
        g.setColor(Game.window.screen.getBackground());
        g.fillRect(0, 0, Game.window.screen.getWidth(), Game.window.screen.getHeight());
        render(g);
        return bmp;
    }

    private static void render() {
        BufferStrategy buffer = Game.window.screen.getBufferStrategy();
        Graphics2D g = (Graphics2D) buffer.getDrawGraphics();
        render(g);
        g.dispose();
        buffer.show();
    }

    public static void render(Graphics2D g) {
        g.setColor(Color.getHSBColor(frameCount % 360 / 360.0f, 1.0f, 1.0f));
        g.fillOval(0, 0, Game.window.screen.getWidth(), Game.window.screen.getHeight());
    }
}
