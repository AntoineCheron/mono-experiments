package testing_apis;

import java.util.concurrent.Executors;

import io.netty.channel.DefaultEventLoop;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import reactor.ipc.netty.http.server.HttpServer;
import reactor.ipc.netty.http.server.HttpServerRoutes;
import reactor.ipc.netty.resources.LoopResources;

public class Main {

  public static final int PORT = 7654;
  public static final String HOST = "127.0.0.1";

  public static void main (String[] args) throws Exception {
    HttpServerRoutes routes = HttpServerRoutes.newRoutes().
      route(WaitingApi::isQueryPath, WaitingApi::handler);

    HttpServer httpServer = HttpServer.builder().options(options ->
      options.host(HOST).port(PORT).
        // channelGroup(new DefaultChannelGroup(new DefaultEventLoop(new NioEventLoopGroup(30)))).
        // eventLoopGroup(new NioEventLoopGroup(20)).
        loopResources(LoopResources.create("test", 2, 100, true))
    ).build();
    // HttpServer httpServer = HttpServer.create(HOST, PORT);
    httpServer.startAndAwait(routes);
  }

}
