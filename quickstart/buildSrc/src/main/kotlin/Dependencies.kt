// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

object Deps {

    // Use getters to propagate .env file dependency version changes to gradle

    object daml {
        val version get() = VersionFiles.dotenv["DAML_RUNTIME_VERSION"]
        val proto get() = "com.daml:ledger-api-proto:$version"
    }

    object grpc {
        val version get() = "1.67.1"
        val commonsProto get() = "com.google.api.grpc:proto-google-common-protos:2.42.0"
        val stub get() = "io.grpc:grpc-stub:$version"
        val protobuf get() = "io.grpc:grpc-protobuf:$version"
        val api get() = "io.grpc:grpc-api:$version"
        val netty get() = "io.grpc:grpc-netty:$version"
    }

    object transcode {
        val version get() = "3.5.8"

        // The codegen plugin and its runtime have no stable 3.5.x release; only snapshots.
        // Their POMs pin Daml LF snapshots that were never published to Maven Central, so
        // :daml forces those transitives to $version. Keep both on the same snapshot.
        val codegenVersion get() = "3.5.0-snapshot.20260428.170.1007.v02f2a2a"

        val plugin get() = "com.daml.codegen-java-daml3:com.daml.codegen-java-daml3.gradle.plugin:$codegenVersion"
        val codegenJavaRuntime get() = "com.daml:transcode-codegen-java-runtime_3:$codegenVersion"
        val protoJava get() = "com.daml:transcode-codec-proto-java_3:$version"
        val protoJson get() = "com.daml:transcode-codec-json_3:$version"

        // Transitives the codegen snapshots pin to unpublished LF snapshots.
        val forcedLf get() = listOf(
            "com.daml:daml-lf-archive_2.13:$version",
            "com.daml:transcode-daml-lf_3:$version",
            "com.daml:transcode-schema_3:$version",
        )
    }

    object springBoot {
        val version get() = "3.4.2"
        val web get() = "org.springframework.boot:spring-boot-starter-web:$version"
        val jdbc get() = "org.springframework.boot:spring-boot-starter-jdbc:$version"
        val oauth2Client get() = "org.springframework.boot:spring-boot-starter-oauth2-client:$version"
        val oauth2ResourceServer get() = "org.springframework.boot:spring-boot-starter-oauth2-resource-server:$version"
        val security get() = "org.springframework.boot:spring-boot-starter-security:$version"
        val actuator get() = "org.springframework.boot:spring-boot-starter-actuator:$version"
        val test get() = "org.springframework.boot:spring-boot-starter-test:$version"
    }

    object opentelemetry {
        val version get() = VersionFiles.dotenv["OTEL_AGENT_VERSION"]
    }
}
