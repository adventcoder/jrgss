package jrgss;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.Transparency;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;
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
    private RubyFont font;

    public RubyBitmap(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
        font = new RubyFont(runtime);
    }

    public static RubyBitmap newBitmap(Ruby runtime, int width, int height) {
        //TODO
        return new RubyBitmap(runtime, RubySupport.bitmapClass);
    }

    @JRubyMethod
    public void initialize(IRubyObject arg0, IRubyObject arg1) {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        image = gc.createCompatibleImage(RubyNumeric.num2int(arg0), RubyNumeric.num2int(arg1), Transparency.TRANSLUCENT);
        font.initialize(new IRubyObject[0]);
    }

    @JRubyMethod
    public void initialize(IRubyObject arg) {
        File file = RTP.findFile(getRuntime(), arg.asJavaString());
        try {
            image = ImageIO.read(file);
            if (image == null)
                throw new IOException("unsupported image format");
        } catch (IOException ioe) {
            throw RubySupport.newRGSSError(getRuntime(), "failed to create bitmap: " + ioe.getMessage());
        }

        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        if (gc.getColorModel().isCompatibleRaster(image.getRaster())) {
            image.coerceData(gc.getColorModel().isAlphaPremultiplied());
        } else {
            BufferedImage oldImage = image;
            image = gc.createCompatibleImage(oldImage.getWidth(), oldImage.getHeight(), Transparency.TRANSLUCENT);
            createGraphics();
            graphics.drawImage(oldImage, 0, 0, null);
        }

        font.initialize(new IRubyObject[0]);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject orig) {
        if (orig == this) return this;
        if (orig instanceof RubyBitmap other) {
            image = other.copyImage();
            flushGraphics();
            font.initialize_copy(other.font);
        } else {
            throw getRuntime().newTypeError(orig, RubySupport.bitmapClass);
        }
        return this;
    }

    private BufferedImage copyImage() {
        return new BufferedImage(image.getColorModel(), image.copyData(null), image.isAlphaPremultiplied(), null);
    }

    @JRubyMethod
    public void dispose() {
        flushGraphics();
        image.flush();
        image = null;
    }
 
    public boolean isDisposed() {
        return image == null;
    }

    @JRubyMethod(name = "disposed?")
    public IRubyObject disposed_p() {
        return getRuntime().newBoolean(isDisposed());
    }

    private void checkDisposed() {
        if (isDisposed())
            throw RubySupport.newRGSSError(getRuntime(), "disposed bitmap");
    }

    public Graphics2D getGraphics() {
        checkDisposed();
        if (graphics == null)
            createGraphics();
        return graphics;
    }

    private void createGraphics() {
        graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    }

    public void flushGraphics() {
        if (graphics != null) {
            graphics.dispose();
            graphics = null;
        }
    }

    @JRubyMethod
    public IRubyObject font() {
        return font;
    }

    @JRubyMethod(name = "font=")
    public IRubyObject set_font(IRubyObject obj) {
        font.initialize_copy(obj);
        return obj;
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
        return RubyRect.newRect(getRuntime(), 0, 0, image.getWidth(), image.getHeight());
    }

    @JRubyMethod
    public IRubyObject get_pixel(IRubyObject arg0, IRubyObject arg1) {
        checkDisposed();
        int x = RubyNumeric.num2int(arg0);
        int y = RubyNumeric.num2int(arg1);

        int argb = 0;
        if ((x >= 0) && (y >= 0) && (x < image.getWidth()) && (y < image.getHeight()))
            argb = image.getRGB(x, y);

        return RubyColor.newColor(getRuntime(), argb);
    }

    @JRubyMethod
    public IRubyObject set_pixel(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        checkDisposed();
        int x = RubyNumeric.num2int(arg0);
        int y = RubyNumeric.num2int(arg1);
        RubyColor color = RubySupport.asColor(arg2);

        if ((x >= 0) && (y >= 0) && (x < image.getWidth()) && (y < image.getHeight()))
            image.setRGB(x, y, color.getARGB());

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
            clear_rect(RubyRect.newRectLight(getRuntime(), args, 0));
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
            fill_rect(RubySupport.asRect(args[0]), RubySupport.asColor(args[1]));
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 5, 5);
            fill_rect(RubyRect.newRectLight(getRuntime(), args, 0), RubySupport.asColor(args[4]));
        }
    }

    public void fill_rect(RubyRect rect, RubyColor color) {
        Graphics2D g = getGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setColor(color.getColor());
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
    }

    @JRubyMethod(rest = true)
    public void gradient_fill_rect(IRubyObject... args) {
        if (args.length >= 3 && args.length <= 4) {
            gradient_fill_rect(RubySupport.asRect(args[0]), RubySupport.asColor(args[1]), RubySupport.asColor(args[2]), args.length >= 4 ? args[3] : getRuntime().getFalse());
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 6, 7);
            gradient_fill_rect(RubyRect.newRectLight(getRuntime(), args, 0), RubySupport.asColor(args[4]), RubySupport.asColor(args[5]), args.length >= 7 ? args[6] : getRuntime().getFalse());
        }
    }

    public void gradient_fill_rect(RubyRect rect, RubyColor color1, RubyColor color2, IRubyObject vertical) {
        Graphics2D g = getGraphics();
        g.setComposite(AlphaComposite.Src);
        if (vertical.isTrue()) {
            g.setPaint(new GradientPaint(
                rect.x, rect.y, color1.getColor(),
                rect.x, rect.y + rect.height - 1, color2.getColor()
            ));
        } else {
            g.setPaint(new GradientPaint(
                rect.x, rect.y, color1.getColor(),
                rect.x + rect.width - 1, rect.y, color2.getColor()
            ));
        }
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
    }

    @JRubyMethod(rest = true)
    public void blt(IRubyObject... args) {
        Arity.checkArgumentCount(getRuntime(), args, 4, 5);
        blt(RubyNumeric.num2int(args[0]), RubyNumeric.num2int(args[1]), RubySupport.asBitmap(args[2]), RubySupport.asRect(args[3]), args.length >= 5 ? RubyNumeric.num2int(args[4]) : 255);
    }

    public void blt(int x, int y, RubyBitmap src, RubyRect srcRect, int opacity) {
        RubyRect dstRect = RubyRect.newRectLight(getRuntime(), x, y, srcRect.width, srcRect.height);
        stretch_blt(dstRect, src, srcRect, opacity);
    }

    @JRubyMethod(rest = true)
    public void stretch_blt(IRubyObject... args) {
        Arity.checkArgumentCount(getRuntime(), args, 3, 4);
        stretch_blt(RubySupport.asRect(args[0]), RubySupport.asBitmap(args[1]), RubySupport.asRect(args[2]), args.length >= 4 ? RubyNumeric.num2int(args[3]) : 255);
    }

    public void stretch_blt(RubyRect dstRect, RubyBitmap src, RubyRect srcRect, int opacity) {
        if (opacity < 0 || opacity > 255 || src.isDisposed()) return;

        Graphics2D g = getGraphics();
        g.setComposite(AlphaComposite.SrcOver.derive(opacity / 255.0f));
        g.drawImage(src.image,
            dstRect.x, dstRect.y, dstRect.x + dstRect.width, dstRect.y + dstRect.height,
            srcRect.x, srcRect.y, srcRect.x + srcRect.width, srcRect.y + srcRect.height,
            null
        );
    }

    @JRubyMethod(rest = true)
    public void tile_blt(IRubyObject... args) {
        Arity.checkArgumentCount(getRuntime(), args, 3, 4);
        tile_blt(RubySupport.asRect(args[0]), RubySupport.asBitmap(args[1]), RubySupport.asRect(args[2]), args.length >= 4 ? RubyNumeric.num2int(args[3]) : 255);
    }

    public void tile_blt(RubyRect rect, RubyBitmap src, RubyRect srcRect, int opacity) {
        if (opacity < 0 || opacity > 255 || src.isDisposed()) return;

        Graphics2D g = getGraphics();
        g.setComposite(AlphaComposite.Src.derive(opacity / 255.0f));
        g.setPaint(new TexturePaint(src.image, srcRect.getRectangle()));
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
    }

    @JRubyMethod(rest = true)
    public void draw_text(IRubyObject... args) {
        if (args.length >= 2 && args.length <= 3) {
            draw_text(RubySupport.asRect(args[0]), args[1], args.length >= 3 ? RubyNumeric.num2int(args[2]) : 0);
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 5, 6);
            draw_text(RubyRect.newRectLight(getRuntime(), args, 0), args[4], args.length >= 6 ? RubyNumeric.num2int(args[5]) : 0);
        }
    }

    public void draw_text(RubyRect rect, IRubyObject obj, int align) {
        String str = obj.asString().asJavaString();

        Graphics2D g = getGraphics();
        g.setComposite(AlphaComposite.SrcOver);
        g.setClip(rect.x, rect.y, rect.width, rect.height);
        g.setFont(font.getFont());

        //TODO: scale to fit width

        boolean outlineShapeRendering = false;
        if (font.outline && outlineShapeRendering) {
            GlyphVector gv = g.getFont().createGlyphVector(g.getFontRenderContext(), str);
            Rectangle2D.Float bounds = (Rectangle2D.Float) gv.getLogicalBounds();

            float x = rect.x + switch (align) {
                case 1 -> Math.max(rect.width - bounds.width, 0) / 2;
                case 2 -> Math.max(rect.width - bounds.width, 0);
                default -> 0;
            };
            float y = rect.y + Math.max(rect.height - bounds.height, 0) / 2 - bounds.y;

            if (font.shadow) {
                Shape shadowShape = gv.getOutline(x + 1, y + 1);
                g.setColor(font.getShadowColor());
                g.fill(shadowShape);
                g.draw(shadowShape);
            }

            Shape shape = gv.getOutline(x, y);

            g.setColor(font.getColor());
            g.fill(shape);

            g.setColor(font.getOutColor());
            g.draw(shape);
        } else {
            Rectangle2D.Float bounds = (Rectangle2D.Float) g.getFont().getStringBounds(str, g.getFontRenderContext());

            float x = rect.x + switch (align) {
                case 1 -> Math.max(rect.width - bounds.width, 0) / 2;
                case 2 -> Math.max(rect.width - bounds.width, 0);
                default -> 0;
            };
            float y = rect.y + Math.max(rect.height - bounds.height, 0) / 2 - bounds.y;

            if (font.outline) {
                g.setColor(font.getOutColor());
                g.drawString(str, x - 1, y - 1);
                g.drawString(str, x + 1, y - 1);
                g.drawString(str, x - 1, y + 1);
                g.drawString(str, x + 1, y + 1);
            }

            if (font.shadow) {
                g.setColor(font.getShadowColor());
                g.drawString(str, x + 1, y + 1);
            }

            g.setColor(font.getColor());
            g.drawString(str, x, y);
        }

        g.setClip(null);
    }

    @JRubyMethod
    public IRubyObject text_size(IRubyObject obj) {
        String str = obj.asString().asJavaString();

        Graphics2D g = getGraphics();
        Rectangle2D bounds = font.getFont().getStringBounds(str, g.getFontRenderContext());
        // hack since values should be very close to integers but otherwise we want to round up
        int width = (int) (bounds.getWidth() + 0.99);
        int height = (int) (bounds.getHeight() + 0.99);

        return RubyRect.newRect(getRuntime(), 0, 0, width, height);
    }

    @JRubyMethod
    public void hue_change(IRubyObject arg) {
        checkDisposed();
        float dh = RubyNumeric.num2int(arg) / 360.f;
        float[] hsb = new float[3];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                Color.RGBtoHSB(r, g, b, hsb);
                hsb[0] += dh;

                int newRGB = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                image.setRGB(x, y, (argb & 0xFF000000) | (newRGB & 0x00FFFFFF));
            }
        }
    }

    @JRubyMethod(optional = 1)
    public void blur(IRubyObject... args) {
        checkDisposed();
        int radius = args.length >= 1 ? RubyNumeric.num2int(args[0]) : 1;
        if (radius <= 0 || radius > image.getWidth() || radius > image.getHeight()) return;

        //NOTE: we could cache the temp raster
        WritableRaster temp = image.getRaster().createCompatibleWritableRaster();
        RasterOps.blurX(image.getRaster(), temp, radius);
        RasterOps.blurY(temp, image.getRaster(), radius);
    }

    @JRubyMethod
    public void radial_blur(IRubyObject arg0, IRubyObject arg1) {
        checkDisposed();
        int amplitude = RubyNumeric.num2int(arg0);
        int divisions = Math.min(Math.max(RubyNumeric.num2int(arg1), 2), 100);
        if (amplitude <= 0) return;

        //NOTE: we could cache the temp raster
        WritableRaster tempRaster = image.getRaster().createCompatibleWritableRaster();
        image.copyData(tempRaster);
        RasterOps.radialBlur(tempRaster, image.getRaster(), amplitude, divisions);
    }

    @JRubyMethod
    public void save(IRubyObject arg) {
        //NOTE: we could support a background color option for bmp and jpg.
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
            throw RubySupport.newRGSSError(getRuntime(), "failed saving bitmap: " + ioe.getMessage());
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
            //NOTE: we could parse a ruby opts hash into params here, e.g. quality

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
