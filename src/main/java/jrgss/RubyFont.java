package jrgss;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyFile;

import jnr.ffi.Struct.caddr_t;

public class RubyFont extends RubyObject {
    private static RubyFont defaultFont;

    public static void createFontClass(Ruby runtime) {
        RubyClass cls = runtime.defineClass("Font", runtime.getObject(), RubyFont::new);
        RGSS.fontClass = cls;
        cls.defineAnnotatedMethods(RubyFont.class);

        initDefaultFont(runtime);
    }

    private static void initDefaultFont(Ruby runtime) {
        defaultFont = new RubyFont(runtime, RGSS.fontClass, false);
        defaultFont.name = runtime.newString("VL Gothic");
        defaultFont.size = 24;
        defaultFont.bold = false;
        defaultFont.italic = false;
        defaultFont.outline = true;
        defaultFont.shadow = false;
        defaultFont.color = RubyColor.newColor(runtime, 255, 255, 255);
        defaultFont.outColor = RubyColor.newColor(runtime, 0, 0, 0, 128);
    }

    public IRubyObject name;
    public double size;
    public boolean bold;
    public boolean italic;
    public boolean outline;
    public boolean shadow;
    public RubyColor color;
    public RubyColor outColor;

    public RubyFont(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RubyFont(Ruby runtime, RubyClass metaClass, boolean useObjectSpace) {
        super(runtime, metaClass, useObjectSpace);
    }

    public static RubyFont newFont(Ruby runtime) {
        RubyFont font = new RubyFont(runtime, RGSS.fontClass);
        font.initialize_copy(defaultFont);
        return font;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE, optional = 2)
    public void initialize(IRubyObject... args) {
        initialize_copy(defaultFont);
        if (args.length >= 1) name_set(args[0]);
        if (args.length >= 2) size_set(args[1]);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject orig) {
        if (orig == this) return this;
        if (orig instanceof RubyFont font) {
            this.name = font.name;
            this.size = font.size;
            this.bold = font.bold;
            this.italic = font.italic;
            this.outline = font.outline;
            this.shadow = font.shadow;
            this.color = (RubyColor) font.color.dup(); //TODO: copy from java
            this.outColor = (RubyColor) font.outColor.dup(); //TODO: copy from java
        } else {
            throw orig.getRuntime().newTypeError(orig, RGSS.colorClass);
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject name() {
        return name;
    }

    @JRubyMethod
    public IRubyObject size() {
        return getRuntime().newFloat(size);
    }

    @JRubyMethod
    public IRubyObject bold() {
        return bold ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod
    public IRubyObject italic() {
        return italic ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod
    public IRubyObject outline() {
        return outline ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod
    public IRubyObject shadow() {
        return italic ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod
    public IRubyObject color() {
        return color;
    }

    @JRubyMethod
    public IRubyObject out_color() {
        return outColor;
    }

    @JRubyMethod(name = "name=")
    public IRubyObject name_set(IRubyObject obj) {
        this.name = obj;
        return obj;
    }

    @JRubyMethod(name = "size=")
    public IRubyObject size_set(IRubyObject obj) {
        this.size = RGSS.checkRange(obj, "size", 6.0, 96.0);
        return obj;
    }

    @JRubyMethod(name = "bold=")
    public IRubyObject bold_set(IRubyObject obj) {
        this.bold = obj.isTrue();
        return obj;
    }

    @JRubyMethod(name = "italic=")
    public IRubyObject italic_set(IRubyObject obj) {
        this.italic = obj.isTrue();
        return obj;
    }

    @JRubyMethod(name = "outline=")
    public IRubyObject outline_set(IRubyObject obj) {
        this.outline = obj.isTrue();
        return obj;
    }

    @JRubyMethod(name = "shadow=")
    public IRubyObject shadow_set(IRubyObject obj) {
        this.shadow = obj.isTrue();
        return obj;
    }

    @JRubyMethod(name = "color=")
    public IRubyObject color_set(IRubyObject obj) {
        this.color = RGSS.asColor(obj);
        return obj;
    }

    @JRubyMethod(name = "out_color=")
    public IRubyObject out_color_set(IRubyObject obj) {
        this.outColor = RGSS.asColor(obj);
        return obj;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject list(IRubyObject recv) {
        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        RubyArray<?> array = recv.getRuntime().newArray(names.length);
        for (String name : names)
            array.append(RubyString.newString(recv.getRuntime(), name));
        return array;
    }

    @JRubyMethod(name = "exist?", meta = true)
    public static IRubyObject exist_p(IRubyObject recv, IRubyObject obj) {
        String name = obj.asString().asJavaString();
        Font font = new Font(name, Font.PLAIN, 1);
        return font.getFamily().equals(name) ? recv.getRuntime().getTrue() : recv.getRuntime().getFalse();
    }

    public Font toJavaFont(Graphics2D graphics) {
        //TODO: this can also be an array
        String nameAsString = name.asString().asJavaString();

        int style = 0;
        if (bold) style |= Font.BOLD;
        if (italic) style |= Font.ITALIC;

        // Make an initial guess at font size using dpi = 96.
        // It's not worth trying to determine the correct dpi due to inaccuracies and we rescale anyway.
        Font font = new Font(nameAsString, style, (int) (size*0.75));
        FontMetrics metrics = graphics.getFontMetrics(font);

        float scale = (float) size / metrics.getHeight();
        return font.deriveFont(font.getSize2D() * scale);
    }

    public static void main(String[] args) throws IOException {

        int w = 544;
        int h = 416;
        int targetFontSize = 6;

        String str = "Hello\u0001World! 'm'";
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage image = gc.createCompatibleImage(544, 416, Transparency.TRANSLUCENT);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        System.out.println("Expected pt: " + targetFontSize * 72.0f / dpi);
        System.out.println();
        for (int size = 0; size <= 96; size++) {
            Font font = new Font("DejaVu Sans", Font.PLAIN, size);
            FontMetrics fm = graphics.getFontMetrics(font);
            System.out.println(size + "pt -> " + fm.getHeight() + "px");
        }

        Font font = new Font("DejaVu Sans", Font.PLAIN, (int) (targetFontSize*0.75));
        FontMetrics fm = graphics.getFontMetrics(font);
        System.out.println("height1: " + fm.getHeight());

        float scale = (float) targetFontSize / fm.getHeight();
        font = font.deriveFont(font.getSize() * scale);
        fm = graphics.getFontMetrics(font);
        System.out.println("height2: " + fm.getHeight());

        graphics.setComposite(AlphaComposite.Src);

        graphics.setColor(new Color(255, 255, 255));
        graphics.fillRect(0, 0, w, h);

        int strWidth = fm.stringWidth(str);
        graphics.setColor(new Color(0, 255, 255));
        graphics.fillRect(0, (image.getHeight() - fm.getHeight()) / 2, strWidth, fm.getHeight());

        graphics.setColor(new Color(128, 128, 128));
        graphics.fillRect(0, h / 2, w, 1);

        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setColor(new Color(255, 0, 0));
        graphics.drawString(str, 0, (image.getHeight() - fm.getHeight()) / 2 + fm.getAscent());


        graphics.dispose();

        ImageIO.write(image, "png", new File("test/b.png"));
    }
}
