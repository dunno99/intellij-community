// "Convert field to local variable in constructor" "true-preview"
import java.util.ArrayList;

class ITest {

    public IntelliJBugConvertToLocal(int x, int z) {

    if (x == 5) {
      mayBeLocal.add("jjj");
    }

    if (x > z) {
      useIt(mayBeLocal);
    }
  }
  @SuppressWarnings("UnusedParameters")
  private void useIt(Object data) {
    System.out.println(data);
  }
}