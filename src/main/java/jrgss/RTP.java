package jrgss;

import java.io.File;

import org.jruby.Ruby;
import org.jruby.util.JRubyFile;

public class RTP {
    public static File findFile(Ruby runtime, String path) {
        File file = findFileAt(runtime.getCurrentDirectory(), path);
        if (file != null) return file;
        //IMPL: RPG Maker would show an error message box and exit here instead of raising
        throw RGSS.newError(runtime, "Unable to find file: " + path);
    }
 
    private static File findFileAt(String cwd, String path) {
        File file = new JRubyFile(cwd, path);
        if (file.exists() && file.isFile())
            return file;

        File parent = file.getParentFile();
        if (parent == null) return null;

        for (String entry : parent.list()) {
            int dotIndex = entry.lastIndexOf('.');
            if (dotIndex <= 0) continue;

            JRubyFile candidateWithoutExt = new JRubyFile(parent.getPath(), entry.substring(0, dotIndex));
            if (candidateWithoutExt.equals(file)) {
                JRubyFile candidate = new JRubyFile(parent.getPath(), entry);
                if (!candidate.isFile()) continue;
                return candidate;
            }
        }
        return null;
    }
}
