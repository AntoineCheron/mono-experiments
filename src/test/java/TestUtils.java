import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class TestUtils {

  private static Scheduler elastic = Schedulers.elastic();
  private static Scheduler parallel = Schedulers.parallel();
  private static Scheduler immediate = Schedulers.immediate();
  private static Scheduler single = Schedulers.single();

  public static <T> long mesureFluxOnScheduler(Flux<T> flux, Scheduler scheduler) {
    BiConsumer<Instant, Completed> task = (beginning, completed) -> flux.subscribeOn(scheduler).subscribe(
      el -> /*System.out.println("[" + Thread.currentThread().getName() + "] " + el)*/ el.toString(),
      error -> System.err.println(error.getMessage()),
      () -> completed.complete(Instant.now().toEpochMilli() - beginning.toEpochMilli())
    );
    return mesureExecution(task, scheduler);
  }

  public static <T> long mesureParallelFluxOnScheduler(ParallelFlux<T> flux, Scheduler scheduler) {
    BiConsumer<Instant, Completed> task = (beginning, completed) -> flux.runOn(scheduler).subscribe(
      el -> /*System.out.println("[" + Thread.currentThread().getName() + "] " + el)*/ el.toString(),
      error -> System.err.println(error.getMessage()),
      () -> completed.complete(Instant.now().toEpochMilli() - beginning.toEpochMilli())
    );
    return mesureExecution(task, scheduler);
  }

  public static long mesureExecution(BiConsumer<Instant, Completed> task, Scheduler scheduler) {
    Completed completed = new Completed();
    Instant beginning = Instant.now();

    task.accept(beginning, completed);

    while (!completed.isCompleted()) {
      try { Thread.sleep(10); }
      catch (InterruptedException e) { e.printStackTrace(); }
    }
    Map<Scheduler, String> schedulers = getSchedulers();
    System.out.println("Operation last " + completed.getDuration() + "ms on scheduler : " + schedulers.get(scheduler));
    return completed.getDuration();
  }

  public static long mesureTaskExecution(Runnable task) {
    Instant beginning = Instant.now();
    task.run();
    return Instant.now().toEpochMilli() - beginning.toEpochMilli();
  }

  public static Map<Scheduler, String> getSchedulers() {
    Map<Scheduler, String> schedulers =  new HashMap<>();
    schedulers.put(elastic, "elastic");
    schedulers.put(parallel, "parallel");
    schedulers.put(immediate, "immediate");
    schedulers.put(single, "single");
    return schedulers;
  }

  public static class Completed {
    private boolean completed = false;
    private long duration = -1;
    void complete(long duration) { this.completed = true; this.duration = duration; }
    boolean isCompleted() { return this.completed; }
    long getDuration() { return this.duration; }
  }

  public static void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}
