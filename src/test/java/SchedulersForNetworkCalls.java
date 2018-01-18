import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class SchedulersForNetworkCalls {

  private final Logger logger = LoggerFactory.getLogger("test.SchedulersForNetworkCalls");

  @Test
  public void compareSchedulersForTenAndFiftyCalls() {
    long onSingle = SchedulersForNetworkCallsUtils.mesureTenCallsOnScheduler(Schedulers.single());
    long onParallel = SchedulersForNetworkCallsUtils.mesureTenCallsOnScheduler(Schedulers.parallel());
    long onElastic = SchedulersForNetworkCallsUtils.mesureTenCallsOnScheduler(Schedulers.elastic());
    this.logger.info("On single : " + Ansi.CYAN + onSingle + " ms" + Ansi.RESET);
    this.logger.info("On parallel : " + Ansi.PURPLE + onParallel + " ms" + Ansi.RESET);
    this.logger.info("On elastic : " + Ansi.GREEN + onElastic + " ms" + Ansi.RESET);

    /*  ---
        Results on my machine :
        - single : 2956ms
        - parallel : 2023ms
        - elastic : 2026ms

        Best result would be 1100ms, the duration of the request.
        This can be explained by the fact that requests are made on the reactor-hhtp-nio
        pool that is a parallel scheduler, using as much as 8 threads (on my machine) to make
        the http requests.

        Let's do the same with 50 requests.
     */

    long onSingleFifty = SchedulersForNetworkCallsUtils.mesureCallsOnScheduler(50, Schedulers.single());
    long onParallelFifty = SchedulersForNetworkCallsUtils.mesureCallsOnScheduler(50, Schedulers.parallel());
    long onElasticFifty = SchedulersForNetworkCallsUtils.mesureCallsOnScheduler(50, Schedulers.elastic());

    this.logger.info("On single : " + Ansi.CYAN + onSingleFifty + " ms" + Ansi.RESET);
    this.logger.info("On parallel : " + Ansi.PURPLE + onParallelFifty + " ms" + Ansi.RESET);
    this.logger.info("On elastic : " + Ansi.GREEN + onElasticFifty + " ms" + Ansi.RESET);

    /*  ---
        Doing 50 requests give the following results :
        - single : 7094ms
        - parallel : 7071ms
        - elastic : 7058ms

        This time any scheduler leads to the same duration.
        Making 50 requests is not 5 times longer than doing 10 but it is
        significantly more than 10. We are hoping to reduce this amount of time.
     */
  }

  @Test
  public void compareSchedulersForTenAndFiftyCallsOnCustomClient() {
    reactor.ipc.netty.http.client.HttpClient client = ClientHttp.newClient(1, 30);

    Function<Scheduler, Long> mesureTenCallsOnScheduler = (scheduler) ->
      SchedulersForNetworkCallsUtils.mesureCallsOnSchedulerAndClient(10, scheduler, client);

    long onSingle = mesureTenCallsOnScheduler.apply(Schedulers.single());
    long onParallel = mesureTenCallsOnScheduler.apply(Schedulers.parallel());
    long onElastic = mesureTenCallsOnScheduler.apply(Schedulers.elastic());
    this.logger.info("On single : " + Ansi.CYAN + onSingle + " ms" + Ansi.RESET);
    this.logger.info("On parallel : " + Ansi.PURPLE + onParallel + " ms" + Ansi.RESET);
    this.logger.info("On elastic : " + Ansi.GREEN + onElastic + " ms" + Ansi.RESET);

    /*  ---
        Results on my machine :
        - single : 2956ms
        - parallel : 2023ms
        - elastic : 2026ms

        Best result would be 1100ms, the duration of the request.
        This can be explained by the fact that requests are made on the reactor-hhtp-nio
        pool that is a parallel scheduler, using as much as 8 threads (on my machine) to make
        the http requests.

        Let's do the same with 50 requests.
     */

    Function<Scheduler, Long> mesureFiftyCallsOnScheduler = (scheduler) ->
      SchedulersForNetworkCallsUtils.mesureCallsOnSchedulerAndClient(50, scheduler, client);

    long onSingleFifty = mesureFiftyCallsOnScheduler.apply(Schedulers.single());
    long onParallelFifty = mesureFiftyCallsOnScheduler.apply(Schedulers.parallel());
    long onElasticFifty = mesureFiftyCallsOnScheduler.apply(Schedulers.elastic());

    this.logger.info("On single : " + Ansi.CYAN + onSingleFifty + " ms" + Ansi.RESET);
    this.logger.info("On parallel : " + Ansi.PURPLE + onParallelFifty + " ms" + Ansi.RESET);
    this.logger.info("On elastic : " + Ansi.GREEN + onElasticFifty + " ms" + Ansi.RESET);

    /*  ---
        Doing 50 requests give the following results :
        - single : 7094ms
        - parallel : 7071ms
        - elastic : 7058ms

        This time any scheduler leads to the same duration.
        Making 50 requests is not 5 times longer than doing 10 but it is
        significantly more than 10. We are hoping to reduce this amount of time.
     */
  }

  @Test
  public void reactorNettyVsApacheJetty() {
    int amountOfRequests = 500;
    Scheduler scheduler = Schedulers.elastic();
    // REACTOR TEST
//    long reactorResult = SchedulersForNetworkCallsUtils.mesureCallsOnScheduler(amountOfRequests, scheduler);
//    this.logger.info("REACTOR : " + Ansi.YELLOW + reactorResult + " ms" + Ansi.RESET);

    // JETTY TEST
    SslContextFactory sslContextFactory = new SslContextFactory();
    org.eclipse.jetty.client.HttpClient httpClient = new org.eclipse.jetty.client.HttpClient(sslContextFactory);
    httpClient.setFollowRedirects(false);
    try {
      httpClient.start();
      CompletedCounter completedCounter = new CompletedCounter(amountOfRequests);

      String url = "http://" + ClientHttp.getUrlForDelay(1000);
      long jettyResult = TestUtils.mesureTaskExecution(() -> {
        Flux.range(1, amountOfRequests).parallel().runOn(Schedulers.parallel()).
          flatMap(i -> Mono.fromCallable(() ->
            httpClient.
              newRequest(url).
              method(HttpMethod.GET).
              timeout(10, TimeUnit.SECONDS).
              send().
              getContentAsString()
          ).publishOn(scheduler)).
          subscribe(
            i -> { completedCounter.completeOne(); this.logger.info("Remaining : " + completedCounter.getCounter()); },
            Throwable::printStackTrace
          );

        while(!completedCounter.isCompleted())
          Utils.sleep(10);
      });


      // RESULTS
      this.logger.info("JETTY : " + Ansi.PURPLE + jettyResult + " ms" + Ansi.RESET);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
