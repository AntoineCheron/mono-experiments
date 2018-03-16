import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class SchedulersOnOneElement {

  Scheduler elastic = Schedulers.elastic();
  Scheduler parallel = Schedulers.parallel();
  Scheduler immediate = Schedulers.immediate();
  Scheduler single = Schedulers.single();

  @Test
  public void comparisonOnBasicSleepingFlux() {
    /*  --
        In this function we compare the subscription of a flux that is created from a few objects
        and only performs basic operations on the inner objects themselves.
     */
    Flux<Integer> flux = Flux.range(1, 100).
      map(Object::toString).
      map(Integer::valueOf).
      map(i -> {
        try { Thread.sleep(12); }
        catch(InterruptedException e) { e.printStackTrace(); }
        return i;
      }).
      map(i -> i % 2 == 0 ? i : i + 10);

    TestUtils.getSchedulers().keySet().stream().forEach(scheduler -> TestUtils.mesureFluxOnScheduler(flux, scheduler));

    /*  ---
        If you execute this method, you will see that no matter the Scheduler, the operations always last the
        same time.

        This is normal, due to the fact that a Flux is a stream of data and that mapping data is done
        element by element.

        RESULT : this execution took about 1350ms on my machine for each Scheduler.
     */
  }

  @Test
  public void runOnMethodCallOnParallelFluxMatters() {
    /*  --
        In this function we demonstrate that the place where you put .runOn(Scheduler s) on a ParallelFlux matters.

        Also, it shows that one parallel flux can be run faster than one basic flux.
     */

    // FIRST TEST : .runOn(parallel) after the sleeping map
    ParallelFlux<Integer> fluxAfter = Flux.range(1, 100).parallel().
      map(Object::toString).
      map(Integer::valueOf).
      map(i -> {
        try { Thread.sleep(12); }
        catch(InterruptedException e) { e.printStackTrace(); }
        System.out.println(Thread.currentThread().getName());
        return i;
      }).
      runOn(elastic).
      map(i -> i % 2 == 0 ? i : i + 10);

    long afterSleepingResult = TestUtils.mesureParallelFluxOnScheduler(fluxAfter, elastic);

    // SECOND TEST : .runOn(parallel) before the sleeping map
    ParallelFlux<Integer> fluxBefore = Flux.range(1, 100).parallel().
      map(Object::toString).
      map(Integer::valueOf).
      runOn(elastic).
      map(i -> {
        try { Thread.sleep(12); }
        catch(InterruptedException e) { e.printStackTrace(); }
        System.out.println(Thread.currentThread().getName());
        return i;
      }).
      map(i -> i % 2 == 0 ? i : i + 10);

    long beforeSleepingResult = TestUtils.mesureParallelFluxOnScheduler(fluxBefore, elastic);

    System.out.println("After sleeping result in an execution of " + afterSleepingResult + "ms");
    System.out.println("Before sleeping result in an execution of " + beforeSleepingResult + "ms");

    /*  ---
        Running this method gave me the following results :
        after = 1369ms
        before = 156ms
     */
  }
}
