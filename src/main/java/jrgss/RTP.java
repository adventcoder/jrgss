package jrgss;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.swing.JOptionPane;

import org.jruby.Ruby;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RTP {
    public static final Deque<String> PATH = new ArrayDeque<>();

    static {
        PATH.add(".");
    }

    public static List<File> listDir(String cwd, String path) {
        List<File> files = new ArrayList<>();
        for (String rootPath : PATH) {
            File root = new File(cwd, rootPath);
            File dir = new File(root, path);

            String[] names = dir.list();
            if (names == null) continue;

            for (String name : names)
                files.add(new File(dir, name));
        }
        return files;
    }

    public static File findFile(Ruby runtime, String path) {
        File file = findFile(runtime.getCurrentDirectory(), path);
        if (file == null) {
            Game game = RubySupport.getGame(runtime);
            game.showMessageDialog("Unable to find file: " + path, JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }
        return file;
    }

    public static File findFile(String cwd, String path) {
        for (String rootPath : PATH) {
            File root = new File(cwd, rootPath);
            File file = new File(root, path);

            // if the file exists then return it directly
            if (file.isFile()) return file;
            File parentFile = file.getParentFile();

            String[] siblingNames = (parentFile == null ? new File(".") : parentFile).list();
            if (siblingNames == null) continue; // the parent doesn't exist either

            // otherwise search for a sibling with the same base name
            for (String name : siblingNames) {
                File siblingFile = new File(parentFile, name);
                if (siblingFile.isDirectory()) continue;
                if (FileSupport.removeSuffix(siblingFile).equals(file))
                    return siblingFile;
            }
        }
        return null;
    }
 }
