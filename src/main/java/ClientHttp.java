import java.nio.charset.Charset;

import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.ipc.netty.resources.LoopResources;

import testing_apis.Main;
import testing_apis.WaitingApi;

public class ClientHttp {

  private static final String delayParam = "{delay_in_ms}";
  private static final String apiUrl = Main.HOST + ":" + Main.PORT + "/" + WaitingApi.ROUTE + "/" + delayParam;

  public static String getUrlForDelay(int delayMs) {
    return apiUrl.replace(delayParam, String.valueOf(delayMs));
  }

  public static Mono<String> getAfterDelay(int ms) {
    return getAfterDelay(ms, ClientHttp.DEFAULT_CLIENT);
  }

  public static Mono<String> getAfterDelay(int ms, HttpClient client) {
    return getAsString(getUrlForDelay(ms), client);
  }

  public static Mono<String> getAfter1second() {
    return getAfter1second(ClientHttp.DEFAULT_CLIENT);
  }

  public static Mono<String> getAfter1second(HttpClient client) {
    return getAfterDelay(1000, client);
  }

  // the create function without param create an HttpClient with an incorrect SSL context.
  public static final HttpClient DEFAULT_CLIENT = HttpClient.create(a -> {});

  public static HttpClient newClient(int selectThreadCount, int workerThreadCount) {
    return HttpClient.create(a -> a.
      // poolResources(PoolResources.elastic("test")).
      // channelGroup(new DefaultChannelGroup(new DefaultEventExecutor(Executors.newCachedThreadPool()))).
      loopResources(LoopResources.create("fr.cheron.antoine.ClientHttp", selectThreadCount, workerThreadCount, true)));
  }

  public static Mono<String> getAsString(String url) {
    return getAsString(url, ClientHttp.DEFAULT_CLIENT);
  }

  public static Mono<String> getAsString(String url, HttpClient client) {
    return client.get(url).
      // map(a -> {System.out.println(Thread.currentThread().getName() + " received response"); return a;}).
      flatMap(response -> {
        if (!response.status().equals(HttpResponseStatus.OK))
          return Mono.error(new RuntimeException("Receive a response with a status code different from 200."));

        return response.receiveContent().
          reduce("", (a, httpContent) -> a.concat(httpContent.content().toString(Charset.forName("UTF-8"))));
    }).doOnError(t -> System.out.println(t.getMessage()));
  }

}
