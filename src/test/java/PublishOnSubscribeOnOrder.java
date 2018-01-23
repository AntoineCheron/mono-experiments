import io.netty.channel.ThreadPerChannelEventLoop;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class PublishOnSubscribeOnOrder {

  @Test
  public void mono() {
    Mono.just("hello").
      subscribeOn(Schedulers.single()).
      map(s -> {
        System.out.println(Thread.currentThread().getName());
        return s;
      }).
      publishOn(Schedulers.parallel()).
      //subscribeOn(Schedulers.single()).
      flatMap(s -> {
        System.out.println(Thread.currentThread().getName());
        return Mono.error(new Exception("error"));
        //return s;
      }).subscribe(
        s -> {
          System.out.println("Subscribe");
          System.out.println(Thread.currentThread().getName());
          System.out.println(s);
        },
        error -> {
          System.out.println("Subscribe");
          System.err.println(Thread.currentThread().getName());
          System.err.println(error.getMessage());
        }
      );

    Utils.sleep(100);
  }

  @Test
  public void flux() {
    Flux.range(1, 10).parallel().
      map(n -> {
        System.out.println(Thread.currentThread().getName());
        return n;
      }).
      runOn(Schedulers.parallel()).
      map(n -> {
        System.out.println(Thread.currentThread().getName());
        return n;
      }).
      runOn(Schedulers.elastic()).
      subscribe(n -> {
        System.out.println(Thread.currentThread().getName());
        System.out.println(n);
      });
  }

  @Test
  public void flatMapWithoutChangingScheduler() {
    Flux.range(1, 3).
      map(n -> identityWithThreadLogging(n, "map1")).
      flatMap(n -> Mono.just(n).
        map(nn -> identityWithThreadLogging(nn, "mono")).
        subscribeOn(Schedulers.single())
      ).
      subscribeOn(Schedulers.parallel()).
      subscribe(n -> {
        this.identityWithThreadLogging(n, "subscribe");
        System.out.println(n);
      });

    Utils.sleep(1000);
  }

  @Test
  public void complexCase() {
    Flux.range(1, 4).
      subscribeOn(Schedulers.immediate()).
      map(n -> identityWithThreadLogging(n, "map1")).
      flatMap(n -> {
        if (n == 1) return createMonoOnScheduler(n, Schedulers.parallel());
        if (n == 2) return createMonoOnScheduler(n, Schedulers.elastic());
        if (n == 3) return createMonoOnScheduler(n, Schedulers.single());
        return Mono.error(new Exception("error")).subscribeOn(Schedulers.newSingle("error-thread"));
      }).
      publishOn(Schedulers.single()).
      map(n -> identityWithThreadLogging(n, "map2")).
      subscribe(
        success -> System.out.println(identityWithThreadLogging(success, "subscribe")),
        error -> System.err.println(identityWithThreadLogging(error, "subscribe, err").getMessage())
      );

    Utils.sleep(1000);
  }

  @Test
  public void combiningParallelAndSequentialFlux() {
    Flux.range(1, 4).
      subscribeOn(Schedulers.parallel()).
      map(n -> identityWithThreadLogging(n, "map1")).
      parallel().
      runOn(Schedulers.elastic()).
      map(n  -> identityWithThreadLogging(n, "parallelFlux")).
      sequential().
      map(n -> identityWithThreadLogging(n, "map2")).
      subscribe(n -> identityWithThreadLogging(n, "subscribe"));
  }

  private <T> Mono<T> createMonoOnScheduler(T el, Scheduler scheduler) {
    return Mono.just(el).
      map(ell -> identityWithThreadLogging(ell, "mono")).
      subscribeOn(scheduler);
  }

  private <T> T identityWithThreadLogging (T el, String operation) {
    Utils.sleep(110);
    System.out.println(operation + " -- " + el + " -- " + Thread.currentThread().getName());
    return el;
  }

}

/*  NOTES :

    - useless to call publishOn immediate() after any other scheduler because it will stay in this same scheduler
    it won't come back to the main thread

 */
