import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkCalls {

  private Logger logger;

  @Before
  public void init() {
    this.logger = LoggerFactory.getLogger("NetworkCalls");
  }

  @Test
  public void shouldFetchAData() {
    long duration = TestUtils.mesureTaskExecution(() -> {
      CompletedCounter completed = new CompletedCounter(1);
      ClientHttp.getAfterDelay(1000).subscribe(
        response -> this.logger.info(Ansi.PURPLE + response + Ansi.RESET),
        Throwable::printStackTrace,
        completed::completeOne
      );

      while(!completed.isCompleted()) {
        Utils.sleep(10);
      }
    });

    this.logger.info("shouldFetchAData() last " + Ansi.CYAN + duration + "ms" + Ansi.RESET);
  }

  @Test
  public void shouldFetchTenData() {
    long duration = TestUtils.mesureTaskExecution(() -> {
      CompletedCounter completed = new CompletedCounter(10);
      Stream.of(1000, 3000, 500, 10, 800, 1200, 600, 300, 1233, 3333).
        map(ClientHttp::getAfterDelay).
        forEach(mono -> mono.subscribe(
          response -> this.logger.info(Ansi.PURPLE + response + Ansi.RESET),
          Throwable::printStackTrace,
          completed::completeOne)
        );

      while (!completed.isCompleted()) {
        Utils.sleep(10);
      }
    });

    this.logger.info("shouldFetchAData() last " + Ansi.CYAN + duration + "ms" + Ansi.RESET);
  }
}
