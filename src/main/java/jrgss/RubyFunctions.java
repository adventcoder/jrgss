package jrgss;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JOptionPane;

import org.jruby.Ruby;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.JRubyFile;

public class RubyFunctions {
    @JRubyMethod
    public static void rgss_main(IRubyObject recv) {
        //TODO
    }

    @JRubyMethod
    public static void rgss_stop(IRubyObject recv) {
        //TODO
    }

    @JRubyMethod
    public static IRubyObject load_data(IRubyObject recv, IRubyObject arg) {
        Ruby runtime = recv.getRuntime();
        JRubyFile file = new JRubyFile(runtime.getCurrentDirectory(), arg.asJavaString());
        try (FileInputStream in = new FileInputStream(file)) {
            return loadObject(runtime, in, false);
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
    }

    private static IRubyObject loadObject(Ruby runtime, InputStream in, boolean taint) throws IOException {
        try (UnmarshalStream stream = new UnmarshalStream(runtime, in, runtime.getNil(), taint)) {
            return stream.unmarshalObject();
        }
    }

    @JRubyMethod
    public static void save_data(IRubyObject recv, IRubyObject obj, IRubyObject arg) {
        Ruby runtime = recv.getRuntime();
        JRubyFile file = new JRubyFile(runtime.getCurrentDirectory(), arg.asJavaString());
        try (FileOutputStream out = new FileOutputStream(file)) {
            dumpObject(out, obj, -1);
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
    }

    private static boolean dumpObject(OutputStream out, IRubyObject obj, int depthLimit) throws IOException {
        try (MarshalStream stream = new MarshalStream(obj.getRuntime(), out, depthLimit)) {
            stream.dumpObject(obj);
            return stream.isTainted();
        }
    }

    @JRubyMethod(rest = true)
    public static void msgbox(IRubyObject recv, IRubyObject... args) {
        Game game = RubySupport.getGame(recv.getRuntime());
        game.showMessageDialog("TODO: msgbox", JOptionPane.PLAIN_MESSAGE);
    }

    @JRubyMethod(rest = true)
    public static void msgbox_p(IRubyObject recv, IRubyObject... args) {
        Game game = RubySupport.getGame(recv.getRuntime());
        game.showMessageDialog("TODO: msgbox_p", JOptionPane.PLAIN_MESSAGE);
    }
}
