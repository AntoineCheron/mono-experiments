import java.time.Instant;

public class Utils {

  public static String timePastFrom(Instant beginning) {
    return giveTimeDifference(beginning, Instant.now());
  }

  public static String giveTimeDifference(Instant beginning, Instant end) {
    return String.valueOf(end.toEpochMilli() - beginning.toEpochMilli()) + "ms";
  }

  public static void sleep(int ms) {
    try { Thread.sleep(ms); }
    catch (InterruptedException e) { e.printStackTrace(); }
  }
}
