import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

public class Test {
    
    public static void main(String[] args) {
        Ruby runtime = Ruby.newInstance();
        IRubyObject result = runtime.executeScript("exit(0)", "test");
        System.out.println("result: " + result);
    }
}
