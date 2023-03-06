package com.example.reactive_swagger;

import generator.OpenApiRoutePublisher;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.StaticHandler;

public class MainVerticle extends AbstractVerticle {
  protected io.vertx.reactivex.core.Vertx vertx;
  public Router router;
  public static final int PORT = 8888;
  private static final String HOST = "localhost";
  public static void main(String[] args) {
    System.out.println("Reactivex swagger implementation");
    Vertx vertx=Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }
  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    router = Router.router(vertx);
    OpenAPI openAPIDoc= OpenApiRoutePublisher.publishOpenApiSpec(
      router,
      "spec",
      "Vertx Swagger Auto Generation By Ashutosh",
      "1.0.0",
      "http://"+HOST+":"+PORT+"/"
    );
    openAPIDoc.addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Product")
      .description("Item operations"));

    router.get("/swagger").handler(res -> {
      res.response()
        .setStatusCode(200)
        .end(Json.pretty(openAPIDoc));
    });

    router.route("/doc/*")
      .handler(StaticHandler.create().setCachingEnabled(false)
        .setWebRoot("webroot/node_modules/swagger-ui-dist"));


    vertx.createHttpServer().requestHandler(router).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }

}
