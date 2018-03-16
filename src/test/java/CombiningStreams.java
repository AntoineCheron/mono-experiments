import java.time.Instant;
import java.util.function.BiFunction;

import org.junit.Test;

import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class CombiningStreams {

  @Test
  public void waitingMap() {
    Scheduler scheduler = Schedulers.elastic();

    Mono<Instant> mapMono = Mono.fromCallable(this::now).
      map(beginning -> { this.oneSecondLongNetworkCall(); printTimeFromBeginning(beginning, "map"); return beginning; }).
      map(beginning -> { this.pointSixSecondLongNetworkCall(); printTimeFromBeginning(beginning); return beginning; }).
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
      map(beginning -> { this.oneSecondLongNetworkCall(); printTimeFromBeginning(beginning, "map"); return beginning; }).
      flatMap(beginning ->
        Mono.fromCallable(() -> { this.pointSixSecondLongNetworkCall();printTimeFromBeginning(beginning, "flatMap"); return beginning; }).subscribeOn(scheduler)
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
      map(beginning -> { this.oneSecondLongNetworkCall(); printTimeFromBeginning(beginning, "map1"); return beginning; }).
      subscribeOn(scheduler);

    Mono<Instant> mono2 = Mono.fromCallable(this::now).
      map(beginning -> { this.pointSixSecondLongNetworkCall(); printTimeFromBeginning(beginning, "map2"); return beginning; }).
      subscribeOn(scheduler);

    System.out.println("Starting executing third test");
    mono1.zipWith(mono2).map(tuple -> {
        System.out.println("Difference in the start between the two Mono: " + (tuple.getT2().toEpochMilli() - tuple.getT1().toEpochMilli()));
        return tuple.getT1();
      }).
      subscribeOn(scheduler).
      subscribe(beginning -> this.printTimeFromBeginning(beginning, "Subscribe"));

    // Result : 1000ms

    try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }
  }

  @Test
  public void combiningMonosWithFlux() {
    Scheduler scheduler = Schedulers.elastic();

    Mono<Instant> mono1 = Mono.fromCallable(this::now).
      map(beginning -> { this.oneSecondLongNetworkCall(); printTimeFromBeginning(beginning, "map1"); return beginning; }).
      subscribeOn(scheduler);

    Mono<Instant> mono2 = Mono.fromCallable(this::now).
      map(beginning -> { this.pointSixSecondLongNetworkCall(); printTimeFromBeginning(beginning, "map2"); return beginning; }).
      subscribeOn(scheduler);

    ParallelFlux.from(mono1, mono2).
      runOn(scheduler).
      reduce((beginning1, beginning2) -> {
        System.out.println("Difference in the start between the two Mono: " + (beginning2.toEpochMilli() - beginning1.toEpochMilli()));
        return beginning1;
      }).subscribe(beginning -> this.printTimeFromBeginning(beginning, "Subscribe"));

    // Result : 1000ms
    // !!!!! The best way to combine the values quickly is to use a ParallelFlux.
    // Unfortunately it requires the sources Mono to have the same type.

    try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }
  }

  @Test
  public void combiningHeterogeneousMono() {
    Scheduler scheduler = Schedulers.elastic();
    Mono<Integer> mono1 = Mono.just(1).subscribeOn(scheduler);
    Mono<String> mono2 = Mono.just("hello world").subscribeOn(scheduler);
  }

  public <T, U, R> Mono<R> combineMono(Mono<T> mono1, Mono<U> mono2, BiFunction<T, U, R> combiner, Scheduler scheduler) {
    Mono<Tuple2<T, U>> tupledMono1 =  mono1.subscribeOn(scheduler).map(value -> Tuples.of(value, null));
    Mono<Tuple2<T, U>> tupledMono2 =  mono2.subscribeOn(scheduler).map(value -> Tuples.of(null, value));

    Mono<R> result = ParallelFlux.from(tupledMono1, tupledMono2).runOn(scheduler).
      reduce((tuple1, tuple2) -> Tuples.of(tuple1.getT1(), tuple2.getT2())).
      map(tuple -> combiner.apply(tuple.getT1(), tuple.getT2()));

    return result;
  }

  private Instant now() {
    return Instant.now();
  }

  private String oneSecondLongNetworkCall() {
    try { Thread.sleep(1000); }
    catch (InterruptedException e) { e.printStackTrace(); }
    return "Hello";
  }

  private String pointSixSecondLongNetworkCall() {
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
