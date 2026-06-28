package jrgss;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Stroke;
import java.awt.image.BufferedImage;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyFont extends RubyObject {
    //NOTE: this should be stored per runtime
    //TODO: maybe store on RGSS where we have other per runtime properties
    private static RubyFont defaultFont;

    public static void createFontClass(Ruby runtime) {
        RubyClass cls = runtime.defineClass("Font", runtime.getObject(), RubyFont::new);
        RGSS.fontClass = cls;
        cls.defineAnnotatedMethods(RubyFont.class);
        initDefaultFont(runtime);
    }

    private static void initDefaultFont(Ruby runtime) {
        defaultFont = new RubyFont(runtime, false);
        defaultFont.name = runtime.newString(Font.SANS_SERIF);
        defaultFont.size = 24;
        defaultFont.bold = false;
        defaultFont.italic = false;
        defaultFont.outline = true;
        defaultFont.shadow = false;
        defaultFont.color = RubyColor.newColor(runtime, 255, 255, 255);
        defaultFont.outColor = RubyColor.newColor(runtime, 0, 0, 0, 128);
    }

    public static @JRubyMethod(meta = true) IRubyObject default_name(IRubyObject recv) { return defaultFont.name(); }
    public static @JRubyMethod(meta = true) IRubyObject default_size(IRubyObject recv) { return defaultFont.size(); }
    public static @JRubyMethod(meta = true) IRubyObject default_bold(IRubyObject recv) { return defaultFont.bold(); }
    public static @JRubyMethod(meta = true) IRubyObject default_italic(IRubyObject recv) { return defaultFont.italic(); }
    public static @JRubyMethod(meta = true) IRubyObject default_outline(IRubyObject recv) { return defaultFont.outline(); }
    public static @JRubyMethod(meta = true) IRubyObject default_shadow(IRubyObject recv) { return defaultFont.shadow(); }
    public static @JRubyMethod(meta = true) IRubyObject default_color(IRubyObject recv) { return defaultFont.color(); }
    public static @JRubyMethod(meta = true) IRubyObject default_out_color(IRubyObject recv) { return defaultFont.out_color(); }

    public static @JRubyMethod(meta = true, name = "default_name=") IRubyObject set_default_name(IRubyObject recv, IRubyObject obj) { return defaultFont.set_name(obj); }
    public static @JRubyMethod(meta = true, name = "default_size=") IRubyObject set_default_size(IRubyObject recv, IRubyObject obj) { return defaultFont.set_size(obj); }
    public static @JRubyMethod(meta = true, name = "default_bold=") IRubyObject set_default_bold(IRubyObject recv, IRubyObject obj) { return defaultFont.set_bold(obj); }
    public static @JRubyMethod(meta = true, name = "default_italic=") IRubyObject set_default_italic(IRubyObject recv, IRubyObject obj) { return defaultFont.set_italic(obj); }
    public static @JRubyMethod(meta = true, name = "default_outline=") IRubyObject set_default_outline(IRubyObject recv, IRubyObject obj) { return defaultFont.set_outline(obj); }
    public static @JRubyMethod(meta = true, name = "default_shadow=") IRubyObject set_default_shadow(IRubyObject recv, IRubyObject obj) { return defaultFont.set_shadow(obj); }
    public static @JRubyMethod(meta = true, name = "default_color=") IRubyObject set_default_color(IRubyObject recv, IRubyObject obj) { return defaultFont.set_color(obj); }
    public static @JRubyMethod(meta = true, name = "default_out_color=") IRubyObject set_default_out_color(IRubyObject recv, IRubyObject obj) { return defaultFont.set_out_color(obj); }

    protected IRubyObject name;
    protected double size;
    protected boolean bold, italic, outline, shadow;
    protected RubyColor color, outColor;

    public RubyFont(Ruby runtime, RubyClass metaClass, boolean useObjectSpace) {
        super(runtime, metaClass, useObjectSpace);
        // we shouldn't expose nulls after Font.allocate
        name = runtime.getNil();
        color = new RubyColor(runtime);
        outColor = new RubyColor(runtime);
    }

    public RubyFont(Ruby runtime, RubyClass metaClass) {
        this(runtime, metaClass, true);
    }

    public RubyFont(Ruby runtime, boolean useObjectSpace) {
        this(runtime, RGSS.fontClass, useObjectSpace);
    }

    public RubyFont(Ruby runtime) {
        this(runtime, true);
    }

    public static RubyFont newFont(Ruby runtime) {
        RubyFont font = new RubyFont(runtime);
        font.set(defaultFont);
        return font;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE, optional = 2)
    public void initialize(IRubyObject... args) {
        set(defaultFont);
        if (args.length >= 1) set_name(args[0]);
        if (args.length >= 2) set_size(args[1]);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(IRubyObject obj) {
        if (obj == this) return this;
        if (obj instanceof RubyFont font) {
            set(font);
        } else {
            throw getRuntime().newTypeError(obj, RGSS.fontClass);
        }
        return this;
    }

    public @JRubyMethod IRubyObject name() { return name; }
    public @JRubyMethod IRubyObject size() { return getRuntime().newFloat(size); }
    public @JRubyMethod IRubyObject bold() { return getRuntime().newBoolean(bold); }
    public @JRubyMethod IRubyObject italic() { return getRuntime().newBoolean(italic); }
    public @JRubyMethod IRubyObject outline() { return getRuntime().newBoolean(outline); }
    public @JRubyMethod IRubyObject shadow() { return getRuntime().newBoolean(shadow); }
    public @JRubyMethod IRubyObject color() { return color; }
    public @JRubyMethod IRubyObject out_color() { return outColor; }

    private void set(RubyFont font) {
        this.name = font.name;
        this.size = font.size;
        this.bold = font.bold;
        this.italic = font.italic;
        this.outline = font.outline;
        this.shadow = font.shadow;
        this.color.set(font.color);
        this.outColor.set(font.outColor);
    }

    @JRubyMethod(name = "name=")
    public IRubyObject set_name(IRubyObject obj) {
        name = obj;
        return obj;
    }

    @JRubyMethod(name = "size=")
    public IRubyObject set_size(IRubyObject obj) {
        size = RGSS.checkRange(obj, "size", 6, 96);
        return obj;
    }

    @JRubyMethod(name = "bold=")
    public IRubyObject set_bold(IRubyObject obj) {
        this.bold = obj.isTrue();
        return obj;
    }

    @JRubyMethod(name = "italic=")
    public IRubyObject set_italic(IRubyObject obj) {
        this.italic = obj.isTrue();
        return obj;
    }

    @JRubyMethod(name = "outline=")
    public IRubyObject set_outline(IRubyObject obj) {
        this.outline = obj.isTrue();
        return obj;
    }

    @JRubyMethod(name = "shadow=")
    public IRubyObject set_shadow(IRubyObject obj) {
        this.shadow = obj.isTrue();
        return obj;
    }

    @JRubyMethod(name = "color=")
    public IRubyObject set_color(IRubyObject obj) {
        this.color.set(obj);
        return obj;
    }

    @JRubyMethod(name = "out_color=")
    public IRubyObject set_out_color(IRubyObject obj) {
        this.outColor.set(obj);
        return obj;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject list(IRubyObject recv) {
        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        RubyArray<?> array = recv.getRuntime().newArray(names.length);
        for (String name : names) {
            array.append(RubyString.newString(recv.getRuntime(), name));
        }
        return array;
    }

    @JRubyMethod(name = "exist?", meta = true)
    public static IRubyObject exist_p(IRubyObject recv, IRubyObject obj) {
        String name = obj.asString().asJavaString();
        Font font = new Font(name, Font.PLAIN, 1);
        return recv.getRuntime().newBoolean(font.getFamily().equalsIgnoreCase(name));
    }

    public Stroke getOutStroke() {
        return new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    public Color getOutColor() {
        int r = (int) (outColor.red + 0.5);
        int g = (int) (outColor.green + 0.5);
        int b = (int) (outColor.blue + 0.5);
        int a = (int) (outColor.alpha * color.alpha / 255.0 + 0.5);
        return new Color(r, g, b, a);
    }

    public Color getShadowColor() {
        int a = (int) (color.alpha + 0.5);
        return new Color(0, 0, 0, a);
    }

    public Font getFont(Graphics2D g) {
        //TODO: this needs to be cached
        //      size and bold/italic can be rederived on the fly as long as we also store the pointsPerPixel.
        Font base = getFont(1);

        double pixelsPerPoint = base.getLineMetrics("", g.getFontRenderContext()).getHeight();
        double pointsPerPixel = 1 / pixelsPerPoint;
        // subtract 0.5 to cancel with the rounding performed by FontMetrics

        return base.deriveFont((float) (size * pointsPerPixel) - 0.5f);
    }

    private Font getFont(int sizePts) {
        int style = getFontStyle();

        if (name instanceof RubyArray) {
            RubyArray<?> nameAsArray = (RubyArray<?>) name;
            for (int i = 0; i < nameAsArray.getLength(); i++) {
                IRubyObject elt = nameAsArray.eltOk(i);
                String faceName = elt.asString().asJavaString();
                Font font = new Font(faceName, Font.PLAIN, sizePts);
                if (font.getFontName().equalsIgnoreCase(faceName))
                    return font;
            }
        } else {
            String faceName = name.asString().asJavaString();
            Font font = new Font(faceName, style, sizePts);
            if (font.getFontName().equalsIgnoreCase(faceName))
                return font;
        }

        return new Font(Font.SANS_SERIF, style, sizePts);
    }

    private int getFontStyle() {
        int style = 0;
        if (bold) style |= Font.BOLD;
        if (italic) style |= Font.ITALIC;
        return style;
    }

    public static void main(String[] args) {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage image = gc.createCompatibleImage(1, 1);
        Graphics2D g = image.createGraphics();

        Font base = new Font("DejaVu SANS Bold", 0, 1);
        System.out.println(base.getName());
        System.out.println(base.getFamily());
        System.out.println(base.getFontName());

        System.out.println(base.getFamily());
        float pixelsPerPoint = base.getLineMetrics("", g.getFontRenderContext()).getHeight();
        float pointsPerPixel = 1 / pixelsPerPoint;

        for (int sizePx = 6; sizePx <= 96; sizePx++) {
            g.setFont(base.deriveFont(sizePx * pointsPerPixel - 0.5f));
            FontMetrics fm = g.getFontMetrics();
            if (fm.getHeight() != sizePx)
                System.out.println("Expecting: " + sizePx + "px, got: " + fm.getHeight() + "px");
        }
    }
}
