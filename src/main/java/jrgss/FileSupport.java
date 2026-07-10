package jrgss;

import java.io.File;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FileSupport {
    public static String getSuffix(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex <= 0) return null; // no extension or dotfile with no extension
        return name.substring(dotIndex + 1);
    }

    public static File removeSuffix(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex <= 0) return file; // no extension or dotfile with no extension
        return new File(file.getParentFile(), name.substring(0, dotIndex));
    }

    public static File addSuffix(File file, String suffix) {
        if (suffix == null) return file;
        return new File(file.getParentFile(), file.getName() + "." + suffix);
    }

    public static File replaceSuffix(File file, String suffix) {
        return addSuffix(removeSuffix(file), suffix);
    }
}
