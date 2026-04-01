// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(Deps.transcode.plugin)
    }
}

plugins {
    id("base")
}

tasks.register<Exec>("compileDaml") {
    dependsOn("verifyDamlSdkVersion")
    val requiredVersion = VersionFiles.damlYamlSdk
    commandLine("dpm", "build", "--all")
    environment("DAML_SDK_VERSION", requiredVersion)
}

tasks.register<Exec>("testDaml") {
    dependsOn("verifyDamlSdkVersion")
    val requiredVersion = VersionFiles.damlYamlSdk
    commandLine("dpm", "test", "--package-root", "licensing-tests")
    environment("DAML_SDK_VERSION", requiredVersion)
}

tasks.register<com.digitalasset.transcode.codegen.java.gradle.JavaCodegenTask>("codeGen") {
    dar.from("$projectDir/licensing/.daml/dist/quickstart-licensing-0.0.1.dar")
    destination = file("$rootDir/backend/build/generated-daml-bindings")
    dependsOn("compileDaml")
}

tasks.named("build") {
    dependsOn("codeGen")
}

// Task to install the Daml SDK using dpm
tasks.register<Exec>("installDamlSdk") {
    val requiredVersion = VersionFiles.damlYamlSdk
    commandLine("dpm", "install", requiredVersion)
    doLast {
        println("Installed Daml SDK version $requiredVersion via dpm")
    }
}

// Task to ensure the right Daml SDK version is installed
tasks.register("verifyDamlSdkVersion") {
    val requiredVersion = VersionFiles.damlYamlSdk

    doLast {
        val output = java.io.ByteArrayOutputStream()
        exec {
            commandLine = listOf("dpm", "version")
            standardOutput = output
            isIgnoreExitValue = true
        }

        val versionLine = output.toString()
            .lineSequence()
            .firstOrNull { it.contains(requiredVersion) }
            ?.trim()

        if (versionLine == null) {
            throw GradleException(
                """
                ❌ Could not find required DAML SDK version:
                   Required:  $requiredVersion

                💡 Please try running: make install-daml-sdk
                """.trimIndent()
            )
        } else {
            println("✅ DAML SDK version $requiredVersion is installed.")
        }

    }
}
