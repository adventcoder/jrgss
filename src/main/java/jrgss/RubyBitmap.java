package jrgss;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.bmp.BMPImageWriteParam;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyFile;

public class RubyBitmap extends RubyObject {
    public static RubyClass createBitmapClass(Ruby runtime) {
        RubyClass cls = runtime.defineClass("Bitmap", runtime.getObject(), RubyBitmap::new);
        cls.defineAnnotatedMethods(RubyBitmap.class);
        return cls;
    }

    private BufferedImage image;
    private Graphics2D graphics;

    public RubyBitmap(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    @JRubyMethod
    public void initialize(IRubyObject arg0, IRubyObject arg1) {
        //NOTE: maybe the image format should be compatible with the screen? (or the window surface?)
        // image = GraphicsEnvironment
        //     .getLocalGraphicsEnvironment()
        //     .getDefaultScreenDevice()
        //     .getDefaultConfiguration()
        //     .createCompatibleImage(RubyNumeric.num2int(arg0), RubyNumeric.num2int(arg1), Transparency.TRANSLUCENT);
        image = new BufferedImage(RubyNumeric.num2int(arg0), RubyNumeric.num2int(arg1), BufferedImage.TYPE_INT_ARGB_PRE);
    }

    @JRubyMethod
    public void initialize(IRubyObject arg) {
        File file = RTP.findFile(getRuntime(), arg.asJavaString());
        try {
            image = ImageIO.read(file);
            if (image == null)
                throw new IOException("unsupported image format");
        } catch (IOException ioe) {
            throw RGSS.newError(getRuntime(), "failed to create bitmap: " + ioe.getMessage());
        }

        if (image.getType() != BufferedImage.TYPE_INT_ARGB_PRE) {
            BufferedImage oldImage = image;
            image = new BufferedImage(oldImage.getWidth(), oldImage.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
            graphics = image.createGraphics();
            graphics.drawImage(oldImage, 0, 0, null);
            oldImage.flush();
        }
    }

    @JRubyMethod
    public void dispose() {
        if (graphics != null) {
            graphics.dispose();
            graphics = null;
        }
        image.flush();
        image = null;
    }
 
    public boolean isDisposed() {
        return image == null;
    }

    @JRubyMethod(name = "disposed?")
    public IRubyObject disposed_p() {
        return isDisposed() ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    private void checkDisposed() {
        if (isDisposed())
            throw RGSS.newError(getRuntime(), "disposed bitmap");
    }

    public Graphics2D getGraphics() {
        checkDisposed();
        if (graphics == null) {
            graphics = image.createGraphics();
            graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );
        }
        return graphics;
    }

    @JRubyMethod
    public IRubyObject width() {
        checkDisposed();
        return getRuntime().newFixnum(image.getWidth());
    }

    @JRubyMethod
    public IRubyObject height() {
        checkDisposed();
        return getRuntime().newFixnum(image.getHeight());
    }

    @JRubyMethod
    public IRubyObject rect() {
        checkDisposed();
        return new RubyRect(getRuntime(), 0, 0, image.getWidth(), image.getHeight());
    }

    @JRubyMethod
    public IRubyObject get_pixel(IRubyObject arg0, IRubyObject arg1) {
        checkDisposed();
        int x = RubyNumeric.num2int(arg0);
        int y = RubyNumeric.num2int(arg1);

        RubyColor color = new RubyColor(getRuntime());
        if ((x >= 0) && (y >= 0) && (x < image.getWidth()) && (y < image.getHeight()))
            image.getRaster().getPixel(x, y, color.rgba);

        return color;
    }

    @JRubyMethod
    public IRubyObject set_pixel(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        checkDisposed();
        int x = RubyNumeric.num2int(arg0);
        int y = RubyNumeric.num2int(arg1);
        RubyColor color = RGSS.asColor(arg2);

        if ((x >= 0) && (y >= 0) && (x < image.getWidth()) && (y < image.getHeight()))
            image.getRaster().setPixel(x, y, color.rgba);
        return color;
    }

    @JRubyMethod
    public void clear() {
        clear_rect(rect());
    }

    @JRubyMethod(rest = true)
    public void clear_rect(IRubyObject... args) {
        if (args.length >= 1 && args[0] instanceof RubyRect) {
            clear_rect((RubyRect) args[0]);
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 4, 4);
            clear_rect(RGSS.rectFromArgs(args, 0, false));
        }
    }

    public void clear_rect(RubyRect rect) {
        Graphics2D g = getGraphics();
        g.setColor(new Color(0, true));
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
    }

    @JRubyMethod(rest = true)
    public void fill_rect(IRubyObject... args) {
        if (args.length == 2) {
            fill_rect(RGSS.asRect(args[0]), RGSS.asColor(args[1]));
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 5, 5);
            fill_rect(RGSS.rectFromArgs(args, 0, false), RGSS.asColor(args[4]));
        }
    }

    public void fill_rect(RubyRect rect, RubyColor color) {
        Graphics2D g = getGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setColor(color.toAwtColor());
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
    }

    @JRubyMethod(rest = true)
    public void gradient_fill_rect(IRubyObject... args) {
        if (args.length >= 3 && args.length <= 4) {
            gradient_fill_rect(RGSS.asRect(args[0]), RGSS.asColor(args[1]), RGSS.asColor(args[2]), args.length >= 4 ? args[3] : getRuntime().getFalse());
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 6, 7);
            gradient_fill_rect(RGSS.rectFromArgs(args, 0, false), RGSS.asColor(args[4]), RGSS.asColor(args[5]), args.length >= 7 ? args[6] : getRuntime().getFalse());
        }
    }

    public void gradient_fill_rect(RubyRect rect, RubyColor color1, RubyColor color2, IRubyObject vertical) {
        Graphics2D g = getGraphics();
        g.setComposite(AlphaComposite.Src);
        if (vertical.isTrue()) {
            g.setPaint(new GradientPaint(
                rect.x, rect.y, color1.toAwtColor(),
                rect.x, rect.y + rect.height - 1, color2.toAwtColor()
            ));
        } else {
            g.setPaint(new GradientPaint(
                rect.x, rect.y, color1.toAwtColor(),
                rect.x + rect.width - 1, rect.y, color2.toAwtColor()
            ));
        }
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
    }

    @JRubyMethod(rest = true)
    public void blt(IRubyObject... args) {
        Arity.checkArgumentCount(getRuntime(), args, 4, 5);
        int x = RubyNumeric.num2int(args[0]);
        int y = RubyNumeric.num2int(args[1]);
        RubyBitmap src = RGSS.asBitmap(args[2]);
        RubyRect srcRect = RGSS.asRect(args[3]);
        int opacity = args.length >= 5 ? RubyNumeric.num2int(args[4]) : 255;

        if (opacity < 0 || opacity > 255 || src.isDisposed()) return;

        Graphics2D g = getGraphics();
        g.setComposite(AlphaComposite.SrcOver.derive(opacity / 255.0f));
        g.drawImage(src.image,
            x, y, x + srcRect.width, y + srcRect.height,
            srcRect.x, srcRect.y, srcRect.x + srcRect.width, srcRect.y + srcRect.height,
            null
        );
    }

    @JRubyMethod(rest = true)
    public void stretch_blt(IRubyObject... args) {
        Arity.checkArgumentCount(getRuntime(), args, 3, 4);
        RubyRect rect = RGSS.asRect(args[0]);
        RubyBitmap src = RGSS.asBitmap(args[1]);
        RubyRect srcRect = RGSS.asRect(args[2]);
        int opacity = args.length >= 4 ? RubyNumeric.num2int(args[3]) : 255;

        if (opacity < 0 || opacity > 255 || src.isDisposed()) return;

        Graphics2D g = getGraphics();
        g.setComposite(AlphaComposite.SrcOver.derive(opacity / 255.0f));
        g.drawImage(src.image,
            rect.x, rect.y, rect.x + rect.width, rect.y + rect.height,
            srcRect.x, srcRect.y, srcRect.x + srcRect.width, srcRect.y + srcRect.height,
            null
        );
    }

    @JRubyMethod(rest = true)
    public void tile_blt(IRubyObject... args) {
        Arity.checkArgumentCount(getRuntime(), args, 3, 4);
        RubyRect rect = RGSS.asRect(args[0]);
        RubyBitmap src = RGSS.asBitmap(args[1]);
        RubyRect srcRect = RGSS.asRect(args[2]);
        int opacity = args.length >= 4 ? RubyNumeric.num2int(args[3]) : 255;

        if (opacity < 0 || opacity > 255 || src.isDisposed()) return;

        Graphics2D g = getGraphics();
        g.setComposite(AlphaComposite.SrcOver.derive(opacity / 255.0f));
        g.setPaint(new TexturePaint(src.image, new Rectangle(srcRect.x, srcRect.y, srcRect.width, srcRect.height)));
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
    }

    @JRubyMethod(rest = true)
    public void draw_text(IRubyObject... args) {
        if (args.length >= 2 && args.length <= 3) {
            draw_text(RGSS.asRect(args[0]), args[1], args.length >= 3 ? RubyNumeric.num2int(args[2]) : 0);
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 5, 6);
            draw_text(RGSS.rectFromArgs(args, 0, false), args[4], args.length >= 6 ? RubyNumeric.num2int(args[5]) : 0);
        }
    }

    public void draw_text(RubyRect rect, IRubyObject obj, int align) {
        String str = obj.asString().asJavaString();
        //TODO
    }

    @JRubyMethod
    public IRubyObject text_size(IRubyObject obj) {
        String str = obj.asString().asJavaString();
        //TODO
        return getRuntime().getNil();
    }

    @JRubyMethod
    public void hue_change(IRubyObject arg) {
        checkDisposed();
        int angle = RubyNumeric.num2int(arg);
        ImageOps.hueChange(image, angle / 360.f);
    }

    @JRubyMethod(optional = 1)
    public void blur(IRubyObject... args) {
        checkDisposed();
        int radius = args.length >= 1 ? RubyNumeric.num2int(args[0]) : 1;
        if (radius == 0) return;

        WritableRaster raster = image.getRaster();
        //NOTE: we could cache the scratch raster
        WritableRaster scratchRaster = raster.createCompatibleWritableRaster();
        ImageOps.blurX(raster, scratchRaster, radius);
        ImageOps.blurY(scratchRaster, raster, radius);
    }

    @JRubyMethod
    public void radial_blur(IRubyObject arg0, IRubyObject arg1) {
        checkDisposed();
        int amplitude = RubyNumeric.num2int(arg0);
        if (amplitude <= 0) return;
        int divisions = Math.min(Math.max(RubyNumeric.num2int(arg1), 2), 100);

        WritableRaster raster = image.getRaster();
        //NOTE: we could cache the scratch raster
        //NOTE: we could also pad here, instead of wrapping inside the loop
        WritableRaster scratchRaster = raster.createCompatibleWritableRaster();
        scratchRaster.setRect(raster);
        ImageOps.radialBlur(scratchRaster, raster, amplitude, divisions);
    }

    @JRubyMethod
    public void save(IRubyObject arg) {
        String path = arg.asJavaString();
        JRubyFile file = new JRubyFile(getRuntime().getCurrentDirectory(), path);

        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex <= 0)
            throw getRuntime().newArgumentError("missing file extension: " + path);
        String suffix = name.substring(dotIndex + 1);

        try {
            if (!writeBySuffix(file, suffix))
                throw new IOException("unsupported file extension: " + suffix);
        } catch (IOException ioe) {
            throw RGSS.newError(getRuntime(), "failed saving bitmap: " + ioe.getMessage());
        }
    }

    private boolean writeBySuffix(Object output, String suffix) throws IOException {
        ImageWriter writer = getWriterBySuffix(suffix);
        if (writer == null)
            return false;

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            if (ios == null)
                throw new IIOException("Can't create ImageOutputStream!");

            ImageWriteParam param = writer.getDefaultWriteParam();
            // we could parse a ruby opts hash into params here, e.g. quality

            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
            ios.flush();
        } finally {
            writer.dispose();
        }

        return true;
    }

    private ImageWriter getWriterBySuffix(String suffix) {
        Iterator<ImageWriter> it = ImageIO.getImageWritersBySuffix(suffix);
        while (it.hasNext()) {
            ImageWriter writer = it.next();
            ImageWriterSpi provider = writer.getOriginatingProvider();
            if (provider.canEncodeImage(image))
                return writer;
        }
        return null;
    }
}
