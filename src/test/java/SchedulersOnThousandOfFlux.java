import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class SchedulersOnThousandOfFlux {

  @Test
  public void compareSchedulersForAThousandFluxConsumingFewCPU() {
    int nbOfFlux = 1000;

    BiFunction<Integer, Scheduler, Long> fluxRunner = (fluxCount, scheduler) -> {
      Instant beginning = Instant.now();
      Counter counter = new Counter(fluxCount * 10);

      Flux.range(1, fluxCount).
        map(this::createSleepingFlux).
        toStream().
        forEach(flux -> flux.subscribeOn(scheduler).subscribe((i) -> counter.next()));

      while (counter.get() != fluxCount * 10) {
        try { Thread.sleep(20); }
        catch (InterruptedException e) { e.printStackTrace(); }
      }
      return Instant.now().toEpochMilli() - beginning.toEpochMilli();
    };

    long parallelDuration = fluxRunner.apply(nbOfFlux, Schedulers.parallel());
    long elasticDuration = fluxRunner.apply(nbOfFlux, Schedulers.elastic());

    System.out.println("On parallel, running the flux last : " + parallelDuration +"ms");
    System.out.println("On elastic, running the flux last : " + elasticDuration +"ms");

    /*  ---
        With this example, I obtain the following results :
        - parallel 14 605ms
        - elastic     401ms

        To conclude, this example demonstrates that even though when looking at only one Flux,
        it takes the same time to be executed, no matter its length, on any scheduler,
        when running a high number of Flux (here 1 000) the scheduler matters.

        Second thing to notice : the elastic scheduler reduce the time of the execution by
        a factor of 36 in the case of task that wait a lot a require a few computation power.
     */
  }

  @Test
  public void compareSchedulersForAThousandFluxConsumingHighCPU() {
    int nbOfFlux = 5000;

    BiFunction<Integer, Scheduler, Long> fluxRunner = (fluxCount, scheduler) -> {
      Instant beginning = Instant.now();
      Counter counter = new Counter(fluxCount);

      Flux.range(1, fluxCount).
        map(this::createFibonacciFlux).
        toStream().
        forEach(flux -> flux.subscribeOn(scheduler).subscribe(fibonacci -> {
          counter.next();
          // System.out.println(fibonacci);
        }));

      while (counter.get() != fluxCount) {
        try { Thread.sleep(100); }
        catch (InterruptedException e) { e.printStackTrace(); }
      }
      return Instant.now().toEpochMilli() - beginning.toEpochMilli();
    };

    long parallelDuration = fluxRunner.apply(nbOfFlux, Schedulers.parallel());
    long elasticDuration = fluxRunner.apply(nbOfFlux, Schedulers.elastic());

    System.out.println("On parallel, running the flux last : " + parallelDuration +"ms");
    System.out.println("On elastic, running the flux last : " + elasticDuration +"ms");

    /*  ---
        With this example, I obtain the following results :

        For a Flux.range(1, 1000)
        - parallel 1 677ms
        - elastic     435ms

        For a Flux.range(1, 2000)
        - parallel 2 935ms
        - elastic  2 135ms

        For a Flux.range(1, 3000)
        - parallel 7 405ms
        - elastic  7 413ms

        For a Flux.range(1, 4000)
        - parallel 16 833ms
        - elastic  18 542ms

        For a Flux.range(1, 5000)
        - parallel 40 954ms
        - elastic  39 952ms

        To conclude, this example demonstrates that when the flux contains operations
        that require high computing power, the parallel schedulers become more and more
        efficient as the required computing power increases.

        It is important to notice that a very high computing power is required to make
        the previous assessment true. Below Flow.range(1,3000) the elastic scheduler was faster.
     */
  }

  private Flux<Integer> createSleepingFlux(Integer id) {
    return Flux.range(1, 10).
      map(i -> {
        try { Thread.sleep(10); }
        catch(InterruptedException e) { e.printStackTrace(); }
        // System.out.println(Thread.currentThread().getName());
        return i;
      }).
      map(i -> i % 2 == 0 ? i : i + 10);
  }

  private Flux<String> createFibonacciFlux(Integer size) {
    return Flux.just(size).
      map(this::fibonacci).
      map(Object::toString);
  }

  private List<BigInteger> fibonacci(Integer i) {
    return fibonacci(new ArrayList<>(), BigInteger.ZERO, BigInteger.ONE, i);
  }

  private List<BigInteger> fibonacci(List<BigInteger> acc, BigInteger n1, BigInteger n2, Integer count) {
    if (count == 0)
      return acc;

    acc.add(n1.add(n2));
    return fibonacci(acc, n2, n1.add(n2), count - 1);
  }

  private class Counter {
    private long count;
    Counter(int remaining) { this.count = 0; }
    synchronized void next() { this.count ++; }
    long get() { return this.count; }
  }

}
