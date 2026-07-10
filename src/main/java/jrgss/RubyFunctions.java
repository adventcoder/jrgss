package jrgss;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JOptionPane;

import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyLocalJumpError;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.JRubyFile;

public class RubyFunctions {
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public static void rgss_main(IRubyObject self, Block block) {
        callLoop(self, (context, args, innerBlock) -> {
            try {
                block.call(context);
            } catch (RaiseException ex) {
                if (RubySupport.RGSSReset.isInstance(ex.getException())) {
                    // TODO: reset handling goes here!
                    return context.nil; // continue loop
                } else {
                    throw ex;
                }
            }

            throw new JumpException.FlowControlException(RubyLocalJumpError.Reason.BREAK, context.getCurrentTarget(), context.nil);
        });
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public static void rgss_stop(IRubyObject self) {
        callLoop(self, (context, args, block) -> {
            RubySupport.Graphics.callMethod("update");
            return context.nil;
        });
    }

    private static IRubyObject callLoop(IRubyObject self, BlockCallback callback) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        Block block = CallBlock.newCallClosure(self, self.getRuntime().getKernel(), Signature.OPTIONAL, callback, context);
        return self.callMethod(context, "loop", new IRubyObject[0], block);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public static IRubyObject load_data(IRubyObject self, IRubyObject fileArg) {
        JRubyFile file = new JRubyFile(self.getRuntime().getCurrentDirectory(), fileArg.asJavaString());
        try {
            return loadData(self.getRuntime(), file);
        } catch (FileNotFoundException ex) {
            throw self.getRuntime().newErrnoENOENTError(fileArg.asJavaString());
        } catch (IOException ex) {
            throw self.getRuntime().newIOErrorFromException(ex);
        }
    }

    public static IRubyObject loadData(Ruby runtime, File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            @SuppressWarnings("resource")
            UnmarshalStream stream = new UnmarshalStream(runtime, in, runtime.getNil(), true);
            return stream.unmarshalObject();
        }
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public static void save_data(IRubyObject self, IRubyObject obj, IRubyObject fileArg) {
        JRubyFile file = new JRubyFile(self.getRuntime().getCurrentDirectory(), fileArg.asJavaString());
        try {
            saveData(self.getRuntime(), file, obj);
        } catch (FileNotFoundException ex) {
            throw self.getRuntime().newErrnoENOENTError(fileArg.asJavaString());
        } catch (IOException ex) {
            throw self.getRuntime().newIOErrorFromException(ex);
        }
    }

    public static void saveData(Ruby runtime, File file, IRubyObject obj) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            @SuppressWarnings("resource")
            MarshalStream stream = new MarshalStream(runtime, out, -1);
            stream.dumpObject(obj);
        }
    }

    @JRubyMethod(visibility = Visibility.PRIVATE, rest = true)
    public static void msgbox(ThreadContext context, IRubyObject self, IRubyObject... args) {
        //NOTE: we could call RubyIO.print() but would need a StringIO to write to
        IRubyObject RS = context.getRuntime().getGlobalVariables().get("$\\");
        IRubyObject FS = context.getRuntime().getGlobalVariables().get("$,");

        StringBuilder msg = new StringBuilder();
        if (args.length == 0) {
            msg.append(context.getLastLine().asString());
        } else {
            for (int i = 0; i < args.length; i++) {
                if (i > 0 && !FS.isNil())
                    msg.append(FS.asString());
                msg.append(args[i].asString());
            }
        }
        if (!RS.isNil())
            msg.append(RS.asString());

        msgbox(context, msg.toString());
    }

    @JRubyMethod(visibility = Visibility.PRIVATE, rest = true)
    public static void msgbox_p(ThreadContext context, IRubyObject self, IRubyObject... args) {
        IRubyObject defaultRS = context.getRuntime().getGlobalVariables().getDefaultSeparator();

        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            msg.append(RubyBasicObject.rbInspect(context, args[i]).asString());
            msg.append(defaultRS.asString());
        }

        msgbox(context, msg.toString());
    }

    private static void msgbox(ThreadContext context, String msg) {
        Game game = RubySupport.getGame(context.getRuntime());
        game.showMessageDialog(msg.toString(), JOptionPane.PLAIN_MESSAGE);
    }
}
