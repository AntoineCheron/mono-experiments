public class CompletedCounter {
  private int counter;

  CompletedCounter(int count) { this.counter = count; }

  public int getCounter() { return this.counter; }
  synchronized public void completeOne() { this.counter--; }
  public boolean isCompleted() { return this.counter == 0; }
}
