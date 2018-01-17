import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class MonoCreation {

  private Logger logger;

  @Before
  public void init() {
    this.logger = LoggerFactory.getLogger("MonoCreation");
  }

  @Test
  public void monoDotCreate() {
    Mono<String> mono = Mono.create(monoSink -> {
      monoSink.success();
    });
  }

  @Test
  public void monoDefer() {
    Scheduler scheduler = Schedulers.elastic();
    Mono<String> mono = Mono.defer(() -> {
      this.logger.info("Waiting");
      Utils.sleep(2000);
      return Mono.just("Hello World -> api response example").subscribeOn(scheduler);
    }).subscribeOn(Schedulers.immediate());

    // Will be subscribed on the scheduler chosen in the supplier of Mono.defer.
    mono.subscribe(
      this.logger::info,
      Throwable::printStackTrace,
      () -> this.logger.info("Finished test."));

    Utils.sleep(3000);
    /*  ---
        Will result in :
        15:44:31.047 [main] INFO  MonoCreation - Waiting
        15:44:33.057 [elastic-2] INFO  MonoCreation - Hello World -> api response example

        It is confusing !
     */
  }

  @Test
  public void monoFromCallable() {
    Scheduler scheduler = Schedulers.elastic();
    Mono<String> mono = Mono.fromCallable(() -> {
      this.logger.info("Waiting");
      Utils.sleep(2000);
      return "Hello World -> api response example";
    }).subscribeOn(scheduler);

    mono.subscribe(
      this.logger::info,
      Throwable::printStackTrace,
      () -> this.logger.info("Finished test."));
    Utils.sleep(3000);

    /*  ---
        Will result in :
        15:44:31.047 [elastic-2] INFO  MonoCreation - Waiting
        15:44:33.057 [elastic-2] INFO  MonoCreation - Hello World -> api response example

        Not confusing as with Mono.defer but less powerful, because it is not possible to handle exception for example.
        See function "showHowDeferIsMorePowerFullThanFromCallable()"
     */
  }

  @Test
  public void fromCallableIsEasierThanDefer() {
    /*  ---
        Let's say we want to create a Mono from a random int between 0 and 100.
        If the random is less than 50, creates a Mono. Otherwise, creates a Mono.error.

        You can see here that it is easier with FromCallable than defer. Plus, it
        prevent you from making a mistake while manipulating schedulers.
     */

    int rnd = new Random().nextInt(100);

    // With fromCallable
    Mono<Integer> mono = Mono.fromCallable(() -> {
      if (rnd > 50) throw new RuntimeException("Error, the random is greater than 50");
      else return rnd;
    });

    mono.subscribe(
      i -> this.logger.info(i.toString()),
      error -> this.logger.error(error.getMessage()),
      () -> this.logger.info("Finished test")
    );

    // With defer
    Mono<Integer> monoDefer = Mono.defer(() -> {
      if (rnd > 50) return Mono.error(new RuntimeException("Error, the random is greater than 50"));
      else return Mono.just(rnd);
    });

    monoDefer.subscribe(
      i -> this.logger.info(i.toString()),
      error -> this.logger.error(error.getMessage()),
      () -> this.logger.info("Finished test")
    );

    // Result is the same
  }

}
