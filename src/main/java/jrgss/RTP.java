package jrgss;

import java.io.File;

import org.jruby.Ruby;
import org.jruby.util.JRubyFile;

public class RTP {
    public static File findFile(Ruby runtime, String path) {
        return findFileAt(runtime.getCurrentDirectory(), path);
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

            JRubyFile candidateWithoutExt = new JRubyFile(parent.getPath(), entry.substring(dotIndex));
            if (candidateWithoutExt.equals(file)) {
                JRubyFile candidate = new JRubyFile(parent.getPath(), entry);
                if (!candidate.isFile()) continue;
                return candidate;
            }
        }
        return null;
    }
}
