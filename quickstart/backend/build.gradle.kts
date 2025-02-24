// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import com.google.protobuf.gradle.*
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    application
    id("org.openapi.generator") version "7.7.0"
    id("org.springframework.boot") version "3.4.2"
    id("com.google.protobuf") version "0.9.4"
}

dependencies {
    implementation(Deps.springBoot.web)
    implementation(Deps.springBoot.jdbc)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")

    implementation(Deps.transcode.codegenJavaRuntime)
    implementation(Deps.transcode.protoJava)
    implementation(Deps.transcode.protoJson)

    protobuf(Deps.daml.proto)
    protobuf(Deps.grpc.commonsProto)
    implementation(Deps.grpc.stub)
    implementation(Deps.grpc.protobuf)
    if (JavaVersion.current().isJava9Compatible()) {
        // Workaround for @javax.annotation.Generated
        // see: https://github.com/grpc/grpc-java/issues/3633
        implementation("javax.annotation:javax.annotation-api:1.3.1")
    }

    // NB this is only here to let Gradle manage the dependency download
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent:${Deps.opentelemetry.version}")

    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:${Deps.opentelemetry.version}")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation(Deps.springBoot.actuator)
    implementation(Deps.springBoot.oauth2Client)
    implementation(Deps.springBoot.security)
    runtimeOnly("org.postgresql:postgresql:42.7.3")
    runtimeOnly(Deps.grpc.api)
    runtimeOnly(Deps.grpc.netty)

    testImplementation(Deps.springBoot.test)
}

repositories {
    mavenCentral()
    maven(Repositories.sonatype)
}

application {
    mainClass = "com.digitalasset.quickstart.App"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.digitalasset.quickstart.App"
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(" ") { file ->
            "libs/${file.name}"
        }
    }
}

tasks.register<Copy>("copyOtelAgentJar") {
    from(configurations.runtimeClasspath)
    into("$projectDir/build/otel-agent")
    include("**/*opentelemetry*javaagent*.jar")
}

tasks.named("build") {
    dependsOn("copyOtelAgentJar")
}

openApiGenerate {
    // TODO: Suppress stdout to get rid of annoying and unprofessional donation begging message from upstream
    generatorName = "spring"
    configOptions = mapOf(
        "responseWrapper" to "CompletableFuture",
        "interfaceOnly" to "true",
        "skipDefaultInterface" to "true"
    )
    additionalProperties = mapOf("useSpringBoot3" to "true")
    generateApiTests = false
    generateModelTests = false
    inputSpec = "$rootDir/common/openapi.yaml"
    outputDir = "$projectDir/build/generated-spring"
    apiPackage = "com.digitalasset.quickstart.api"
}

// task to generate client-side bindings for scan-proxy
tasks.register<GenerateTask>("openApiGenerateClient") {
    generatorName.set("java")
    inputSpec.set("$projectDir/src/main/resources/vendored/scan-proxy-openapi.yaml")
    outputDir.set("$buildDir/generated-client")
    apiPackage.set("com.digitalasset.quickstart.validatorproxy.client.api")
    modelPackage.set("com.digitalasset.quickstart.validatorproxy.client.model")
    configOptions.set(
        mapOf(
            "library" to "native",
            "dateLibrary" to "java8",
            "asyncNative" to "true",
            "jsonLibrary" to "jackson"
        )
    )
    generateApiTests.set(false)
    generateModelTests.set(false)
}


sourceSets {
    main {
        java {
            srcDirs(
                "$projectDir/build/generated-spring/src/main",
                "$projectDir/build/generated-client/src/main/java",
                "$projectDir/build/generated-daml-bindings" // TODO: remove this line once daml plugin is used
            )
        }
    }
    test {
        java {
            srcDir("$projectDir/build/generated-spring/src/test")
        }
    }
}

tasks.getByName("compileJava").dependsOn(
    ":daml:build",
    "openApiGenerate",
    "openApiGenerateClient"
)

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.24.0"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${Deps.grpc.version}"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc") { }
            }
        }
    }
}