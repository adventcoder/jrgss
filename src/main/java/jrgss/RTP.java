package jrgss;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

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
            File root = new File(rootPath);
            if (!root.isAbsolute())
                root = new File(cwd, rootPath);

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
        if (file == null)
            throw runtime.newErrnoENOENTError(path);
        return file;
    }

    public static File findFile(String cwd, String path) {
        for (String rootPath : PATH) {
            File root = new File(rootPath);
            if (!root.isAbsolute())
                root = new File(cwd, rootPath);

            File file = new File(root, path);

            // if the file exists then return it directly
            if (file.exists() && file.isFile()) return file;
            File parentFile = file.getParentFile();

            String[] siblingNames = parentFile.list();
            if (siblingNames == null) continue; // the parent doesn't exist either

            // otherwise search for a sibling with the same base name
            for (String name : siblingNames) {
                File siblingFile = new File(parentFile, name);
                if (!siblingFile.isFile()) continue;
                if (FileSupport.removeSuffix(siblingFile).equals(file))
                    return siblingFile;
            }
        }
        return null;
    }

    public static String getInstallPath(String rtpName) {
        Preferences rtpNode = Preferences.systemRoot().node("jrgss/rtp");
        return rtpNode.get(rtpName, null);
    }

    public static void setInstallPath(String rtpName, String rtpPath) throws BackingStoreException {
        Preferences rtpNode = Preferences.systemRoot().node("jrgss/rtp");
        rtpNode.put(rtpName, rtpPath);
        rtpNode.flush();
    }

    public static class Windows {
        // Make sure to run as administrator
        public static void main(String[] args) throws Exception {
            String libName = "RGSS3";
            String rtpName = "RPGVXAce";
            String rtpPath = getEnterbrainInstallPath(libName, rtpName);
            if (rtpPath == null) {
                System.err.println("ERROR: " + rtpName + " RTP not installed for " + libName);
                System.exit(1);
            }
            System.out.println("Found RTP: " + rtpPath);
            setInstallPath(rtpName, rtpPath);
        }

        private static String getEnterbrainInstallPath(String libName, String rtpName) throws IOException {
            Process process = new ProcessBuilder("reg", "query", "HKLM\\SOFTWARE\\Enterbrain\\" + libName + "\\RTP", "/v", rtpName, "/reg:32").start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                while (line != null) {
                    String[] value = line.trim().split("\\s+", 3);
                    if (value.length == 3 && value[0].equals(rtpName) && value[1].equals("REG_SZ"))
                        return value[2];
                    line = reader.readLine();
                }
            }

            return null;
        }
    }

    public static class Linux {
        public static void main(String[] args) throws Exception {
            String rtpName = "RPGVXAce";
            setInstallPath(rtpName, System.getProperty("user.home") + "/.local/share/jrgss/" + rtpName);
        }
    }
}
