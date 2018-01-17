package testing_apis;

import io.netty.handler.codec.http.HttpResponseStatus;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.ipc.netty.http.server.HttpServerRequest;
import reactor.ipc.netty.http.server.HttpServerResponse;

public class WaitingApi {

  public static final String ROUTE = "wait";

  public static boolean isQueryPath(HttpServerRequest request) {
    return request.path().startsWith(ROUTE);
  }

  public static Publisher<Void> handler(HttpServerRequest request, HttpServerResponse response) {
    return Mono.fromCallable(() -> {
      String delay = request.path().substring(request.path().indexOf("wait/") + "wait/".length());
      Thread.sleep(Integer.valueOf(delay));
      return "Response of the API after a delay of " + delay + "ms";
      }).
      subscribeOn(Schedulers.elastic()).
      flatMap(msg -> response.status(HttpResponseStatus.OK).sendString(Mono.just(msg)).then()).
      onErrorResume(InterruptedException.class, e -> response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR).send());
  }

}
