import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.ipc.netty.http.client.HttpClient;

class SchedulersForNetworkCallsUtils {

  public static Stream<Integer> getOneSecDelays(int amount) {
    return Flux.range(1, amount).toStream();
  }

  public static Stream<Integer> getRandomDelays(int amount) {
    final int maxWait = 3000;
    final Random random = new Random();
    List<Integer> toReturn = new ArrayList<>(amount);

    Flux.range(1, amount).subscribe(unused -> toReturn.add(random.nextInt(maxWait)));

    return toReturn.stream();
  }

  private static void makeTenCallsOnScheduler(Scheduler scheduler) {
    makeCallsOnScheduler(10, scheduler);
  }

  public static long mesureTenCallsOnScheduler(Scheduler scheduler) {
    return TestUtils.mesureTaskExecution(() -> makeTenCallsOnScheduler(scheduler));
  }

  private static void makeCallsOnScheduler(int amount, Scheduler scheduler) {
    makeCallsOnSchedulerAndClient(amount, scheduler, ClientHttp.DEFAULT_CLIENT);
  }

  public static long mesureCallsOnScheduler(int amount, Scheduler scheduler) {
    return TestUtils.mesureTaskExecution(() ->
      makeCallsOnSchedulerAndClient(amount, scheduler, ClientHttp.DEFAULT_CLIENT));
  }

  private static void makeCallsOnSchedulerAndClient(int amount, Scheduler scheduler, HttpClient client) {
    CompletedCounter completedCounter = new CompletedCounter(amount);
    getOneSecDelays(amount).
      map(unused -> ClientHttp.getAfter1second(client)).
      map(mono -> mono.publishOn(Schedulers.parallel())).
      forEach(mono -> mono.subscribe(
        Object::toString, // make it useless
        Throwable::printStackTrace,
        completedCounter::completeOne
      ));

    while(!completedCounter.isCompleted())
      Utils.sleep(10);
  }

  public static long mesureCallsOnSchedulerAndClient(int amount, Scheduler scheduler, HttpClient client) {
    return TestUtils.mesureTaskExecution(() ->
      makeCallsOnSchedulerAndClient(amount, scheduler, client));
  }

  public static long mesureNCallOnSchedulerWithCustomClient(
    int calls, Scheduler scheduler, int selectThreadCount, int workerThreadCount
  ) {
    HttpClient client = ClientHttp.newClient(selectThreadCount, workerThreadCount);
    return TestUtils.mesureTaskExecution(() -> makeCallsOnSchedulerAndClient(calls, scheduler, client));
  }

}
