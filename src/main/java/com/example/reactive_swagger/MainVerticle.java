package com.example.reactive_swagger;

import com.datastax.oss.driver.shaded.guava.common.collect.ImmutableSet;
import com.datastax.oss.driver.shaded.guava.common.reflect.ClassPath;
import generator.OpenApiRoutePublisher;
import generator.Required;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;

public class MainVerticle extends AbstractVerticle {
  public static void main(String[] args) {
    System.out.println("Swagger1 implementation");
    io.vertx.core.Vertx vertx= Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }

  public static final String APPLICATION_JSON = "application/json";
  public static final int PORT = 8888;
  private static final String HOST = "localhost";
  private HttpServer server;
//  private EndPoints endPoints;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
//    endPoints = new EndPoints();
    server = vertx.createHttpServer(createOptions());
    server.requestHandler(configurationRouter());
    server.listen(http ->{
      if(http.succeeded()){
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      }else{
        startPromise.fail(http.cause());
      }
    });
  }

  private HttpServerOptions createOptions(){
    HttpServerOptions options=new HttpServerOptions();
    options.setHost(HOST);
    options.setPort(PORT);
    return options;
  }

  private Router configurationRouter(){
    Router router=Router.router(vertx);
    router.route().consumes(APPLICATION_JSON);
    router.route().produces(APPLICATION_JSON);
    router.route().handler(BodyHandler.create());

    Set<String> allowedHeaders=new HashSet<>();
    allowedHeaders.add("auth");
    allowedHeaders.add("Content-Type");

    Set<HttpMethod> allowedMethods=new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);
    allowedMethods.add(HttpMethod.DELETE);
    allowedMethods.add(HttpMethod.PATCH);
    allowedMethods.add(HttpMethod.PUT);

    router.route()
      .handler(CorsHandler
        .create()
        .allowedHeaders(allowedHeaders)
        .allowedMethods(allowedMethods)
      );

    router.route()
      .handler(context -> {
        context
          .response()
          .headers()
          .add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
        context.next();
      });

    router.route()
      .failureHandler(ErrorHandler.create(vertx,true));

//    router.get("/getall").handler(endPoints::allProduct);
//    router.get("/getall/:id").handler(endPoints::fetchProduct);

    //swagger ui generation-----> here generator folder is used for OpenApiRoutePublisher
    OpenAPI openAPIDoc= OpenApiRoutePublisher.publishOpenApiSpec(
      router,
      "spec",
      "Vertx Swagger Auto Generation By Ashutosh",
      "1.0.0",
      "http://"+HOST+":"+PORT+"/"
    );

    openAPIDoc.addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Product")
      .description("Item operations"));

    ImmutableSet<ClassPath.ClassInfo> modelClasses=getClassesInPackage("com.example.swagger1.Model");
    Map<String,Object> map=new HashMap<String, Object>();

    for(ClassPath.ClassInfo modelClass :modelClasses){
      Field[] fields= FieldUtils.getFieldsListWithAnnotation(modelClass.load(), Required.class)//require in generator
        .toArray(new Field[0]);
      List<String> requiredParameters = new ArrayList<String>();
      for(Field requiredField : fields)
      {
        requiredParameters.add(requiredField.getName());
      }
      fields = modelClass.load().getDeclaredFields();
      for(Field field:fields){
        mapParameters(field,map);
      }
      openAPIDoc.schema(modelClass.getSimpleName(),
        new Schema()
          .title(modelClass.getSimpleName())
          .type("object")
          .required(requiredParameters)
          .properties(map)
      );
      map=new HashMap<String,Object>();
    }

    router.get("/swagger").handler(res -> {
      res.response()
        .setStatusCode(200)
        .end(Json.pretty(openAPIDoc));
    });

    router.route("/doc/*")
      .handler(StaticHandler.create().setCachingEnabled(false)
        .setWebRoot("webroot/node_modules/swagger-ui-dist"));

    return router;
  }

  private void mapParameters(Field field,Map<String,Object> map)
  {
    Class type= field.getType();
    Class componentType= field.getType().getComponentType();

    if(isPrimitiveOrWrapper(type)){
      Schema primitiveSchema=new Schema();
      primitiveSchema.type(field.getType().getSimpleName());
      map.put(field.getName(),primitiveSchema);
    }else{
      HashMap<String, Object> subMap=new HashMap<String,Object>();
      if(isPrimitiveOrWrapper(componentType)){
        HashMap<String,Object> arrayMap =new HashMap<String,Object>();
        arrayMap.put("type",componentType.getSimpleName()+"[]");
        subMap.put("type",arrayMap);
      }else{
        subMap.put("$ref","#/components/schemas/"+componentType.getSimpleName());
      }
      map.put(field.getName(),subMap);
    }
  }

  private Boolean isPrimitiveOrWrapper(Type type){
    return type.equals(Double.class)||
      type.equals(Float.class)||
      type.equals(Long.class)||
      type.equals(Integer.class)||
      type.equals(Short.class)||
      type.equals(Character.class)||
      type.equals(Byte.class)||
      type.equals(Boolean.class)||
      type.equals(String.class);
  }

  public ImmutableSet<ClassPath.ClassInfo> getClassesInPackage(String packagename){
    try{
      ClassPath classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
      ImmutableSet<ClassPath.ClassInfo> classes = classPath.getTopLevelClasses(packagename);
      return classes;
    }
    catch(Exception e){
      return null;
    }
  }
}
