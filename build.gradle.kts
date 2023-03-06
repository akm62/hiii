import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
  java
  application
  id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val vertxVersion = "4.4.0"
val junitJupiterVersion = "5.9.1"

val mainVerticleName = "com.example.reactive_swagger.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

application {
  mainClass.set(launcherClassName)
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-core")
  implementation("io.vertx:vertx-web:$vertxVersion")
  implementation("io.vertx:vertx-config:$vertxVersion")
  implementation("io.vertx:vertx-rx-java2:$vertxVersion")
  implementation("io.vertx:vertx-web-client:$vertxVersion")
  implementation("io.vertx:vertx-kafka-client:$vertxVersion")
  implementation("io.vertx:vertx-mysql-client:$vertxVersion")
  implementation("io.vertx:vertx-service-proxy:$vertxVersion")
  implementation("io.vertx:vertx-web-api-contract:$vertxVersion")
  implementation("io.vertx:vertx-codegen:$vertxVersion")
  implementation("com.aerospike:aerospike-client:4.4.7")

  //Metrics
  implementation("io.vertx:vertx-micrometer-metrics:$vertxVersion")
  implementation("io.micrometer:micrometer-registry-prometheus:1.2.0")
  implementation("ch.qos.logback:logback-classic:1.2.11")
  implementation("net.logstash.logback:logstash-logback-encoder:5.3")
  implementation("io.opentracing:opentracing-api:0.33.0")
  implementation("io.opentracing.contrib:opentracing-metrics-micrometer:0.3.0")
  implementation("io.jaegertracing:jaeger-client:1.1.0")
  implementation("com.newrelic.agent.java:newrelic-api:5.8.0")
  implementation("com.github.ua-parser:uap-java:1.4.0")
  implementation("org.quartz-scheduler:quartz:2.2.1")
  implementation("commons-codec:commons-codec:1.3")
  implementation("com.google.dagger:dagger:2.25.4")
  implementation("io.vertx:vertx-core")
  implementation("io.vertx:vertx-core")

  testImplementation("io.vertx:vertx-junit5")
  testImplementation("com.google.dagger:dagger:2.25.4")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
  testImplementation("io.vertx:vertx-junit5:$vertxVersion")
  testImplementation("org.mockito:mockito-core:3.2.0")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.2.0")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.2.0")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:5.2.0")
  testImplementation("org.assertj:assertj-core:3.8.0")


  implementation("com.google.guava:guava:31.1-jre")


  //For swagger spec generation and UI.
  implementation("org.json:json:20220924")
  implementation("io.swagger.core.v3:swagger-annotations:2.2.8")
  implementation("io.swagger.core.v3:swagger-core:2.2.8")
  implementation("io.swagger.core.v3:swagger-jaxrs2:2.2.8")
  implementation("io.swagger.core.v3:swagger-models:2.2.8")
//    implementation("org.slf4j:slf4j-api:2.0.0")       -----> used for logging facade for java api (cause error in logging)
  //for dependency management
  implementation("io.vertx:vertx-stack-depchain:4.3.7")
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf("run", mainVerticleName, "--redeploy=$watchForChange", "--launcher-class=$launcherClassName", "--on-redeploy=$doOnChange")
}
