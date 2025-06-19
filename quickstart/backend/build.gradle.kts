// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import com.google.protobuf.gradle.*
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import java.net.URI
import java.util.Scanner
import java.nio.file.Files

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
    implementation(Deps.springBoot.oauth2ResourceServer)
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
    dependsOn("validateSpliceCompat")
    dependsOn("copyOtelAgentJar")
}

tasks.register("validateSpliceCompat") {
    doLast {
        val localVersion = localSpliceVersion()
        val devnetUrl = "https://docs.dev.global.canton.network.sync.global/versions"
        val synchoronisersInfo = fetchDevNetRows(devnetUrl)
        if (synchoronisersInfo.isEmpty())
            logger.warn("CSV at $devnetUrl was empty or malformed")

        val versionGroups = synchoronisersInfo.groupBy { it.version }
        if (versionGroups.size > 1) {
            logger.warn("\n⚠️  DevNet is in a MIXED STATE – multiple versions detected:")
            versionGroups.forEach { (ver, group) ->
                logger.warn("  • ⚠️ $ver  ← (${group.size} SVs), eg. ${group.first().name}")
            }
        }
        // Pick the foundation row as the source of truth
        val remoteSpliceVersion = synchoronisersInfo
            .firstOrNull { it.name == "Global-Synchronizer-Foundation" }
            ?.version
            ?: error("Global-Synchronizer-Foundation row not found in devnet")

        logger.lifecycle("""
            |• DevNet: Splice version: $remoteSpliceVersion
            |• Local : Splice version: $localVersion
            """.trimMargin()
        )

        if (remoteSpliceVersion == localVersion) {
            logger.lifecycle("✅ Versions match – continuing")
        } else {
            logger.warn("⚠️ DevNet and Quickstart do not match!")
            print("Do you want to proceed anyway? [y/n]: ")
            System.out.flush()
            val answer = Scanner(System.`in`).nextLine().trim().lowercase()
            if (answer != "y" && answer != "yes") {
                throw GradleException("❌ Aborted due to Splice version mismatch from DevNet: $remoteSpliceVersion and Quickstart $localVersion")
            }
        }
    }
}

data class CNSyncInfo(val name: String, val scanUrl: String, val version: String)

fun fetchDevNetRows(url: String): List<CNSyncInfo> =
    URI.create(url).toURL().readText().lineSequence()
        .drop(1)
        .filter { it.isNotEmpty() }
        .map { it.split(',').map(String::trim) }
        .filter { it.size >= 3 }
        .map { CNSyncInfo(it[0], it[1], it[2]) }
        .toList()

fun localSpliceVersion(): String {
    val envFile = project.rootProject.file(".env")
    val envVars: Map<String, String> =
        if (envFile.exists()) {
            Files.readAllLines(envFile.toPath())
                .asSequence()
                .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
                .map {
                    val (k, v) = it.split('=', limit = 2)
                    k.trim() to v.trim()
                }
                .toMap()
        } else emptyMap()
    // ---What is this QS built against?
    return (findProperty("IMAGE_TAG") as? String)
        ?: envVars["IMAGE_TAG"]
        ?: System.getenv("IMAGE_TAG")           // final fallback
        ?: error("Missing property `IMAGE_TAG` not found (Gradle property, .env, or env var)")
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
                "$projectDir/build/generated-spring/src/main/java",
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