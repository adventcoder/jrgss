package jrgss;

public class ParsedArgs {
    public boolean test;
    public boolean btest;
    public boolean console;

    public ParsedArgs(String[] args) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("test")) {
                test = true;
            } else if (arg.equalsIgnoreCase("btest")) {
                test = true;
            } else if (arg.equalsIgnoreCase("console")) {
                console = true;
            }
        }
    }
}
