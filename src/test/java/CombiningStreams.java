import java.time.Instant;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class CombiningStreams {

  @Test
  public void waitingMap() {
    Scheduler scheduler = Schedulers.elastic();

    Mono<Instant> mapMono = Mono.fromCallable(this::now).
      map(beginning -> { this.hello(); printTimeFromBeginning(beginning, "map"); return beginning; }).
      map(beginning -> { this.world(); printTimeFromBeginning(beginning); return beginning; }).
      subscribeOn(scheduler);
    System.out.println("Start executing mapMono");
    mapMono.subscribe(beginning -> this.printTimeFromBeginning(beginning, "Subscribe"));

    // Result : 1600ms

    try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }
  }

  @Test
  public void waitingFlatMap() {
    Scheduler scheduler = Schedulers.elastic();

    Mono<Instant> flatMapMono = Mono.fromCallable(this::now).
      map(beginning -> { this.hello(); printTimeFromBeginning(beginning, "map"); return beginning; }).
      flatMap(beginning ->
        Mono.fromCallable(() -> { this.world();printTimeFromBeginning(beginning, "flatMap"); return beginning; }).subscribeOn(scheduler)
      ).subscribeOn(scheduler);

    System.out.println("Starting executing flatMapMono");
    flatMapMono.subscribe(beginning -> this.printTimeFromBeginning(beginning, "Subscribe"));

    // Result : 1600ms

    try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }
  }

  @Test
  public void combiningMono() {
    Scheduler scheduler = Schedulers.elastic();

    Mono<Instant> mono1 = Mono.fromCallable(this::now).
      map(beginning -> { this.hello(); printTimeFromBeginning(beginning, "map1"); return beginning; }).
      subscribeOn(scheduler);

    Mono<Instant> mono2 = Mono.fromCallable(this::now).
      map(beginning -> { this.world(); printTimeFromBeginning(beginning, "map2"); return beginning; }).
      subscribeOn(scheduler);

    System.out.println("Starting executing third test");
    mono1.flatMap(beginning1 ->
      mono2.map(beginning2 -> {
        System.out.println("Difference in the start between the two Mono: " + (beginning2.toEpochMilli() - beginning1.toEpochMilli()));
        return beginning1;
      })).
      subscribeOn(scheduler).
      subscribe(beginning -> this.printTimeFromBeginning(beginning, "Subscribe"));

    // Result : 1600ms

    try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }
  }

  @Test
  public void combiningMonosWithFlux() {
    Scheduler scheduler = Schedulers.elastic();

    Mono<Instant> mono1 = Mono.fromCallable(this::now).
      map(beginning -> { this.hello(); printTimeFromBeginning(beginning, "map1"); return beginning; }).
      subscribeOn(scheduler);

    Mono<Instant> mono2 = Mono.fromCallable(this::now).
      map(beginning -> { this.world(); printTimeFromBeginning(beginning, "map2"); return beginning; }).
      subscribeOn(scheduler);

    ParallelFlux.from(mono1, mono2).
      runOn(scheduler).
      reduce((beginning1, beginning2) -> {
        System.out.println("Difference in the start between the two Mono: " + (beginning2.toEpochMilli() - beginning1.toEpochMilli()));
        return beginning1;
      }).subscribe(beginning -> this.printTimeFromBeginning(beginning, "Subscribe"));

    // Result : 1000ms
    // !!!!! The best way of achieving short time for multiple requests

    try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }
  }

  private Instant now() {
    return Instant.now();
  }

  private String hello() {
    try { Thread.sleep(1000); }
    catch (InterruptedException e) { e.printStackTrace(); }
    return "Hello";
  }

  private String world() {
    try { Thread.sleep(600); }
    catch (InterruptedException e) { e.printStackTrace(); }
    return " World!";
  }

  private void printTimeFromBeginning(Instant beginning) {
    System.out.println(Thread.currentThread().getName() + " - Time from beginning : " +
      (Instant.now().toEpochMilli() - beginning.toEpochMilli())
    );
  }

  private void printTimeFromBeginning(Instant beginning, String msg) {
    System.out.println("[" + Thread.currentThread().getName() + "] " + msg + " - Time from beginning : " +
      (Instant.now().toEpochMilli() - beginning.toEpochMilli())
    );
  }

}
