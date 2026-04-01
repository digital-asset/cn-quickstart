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
    id("de.undercouch.download") version "5.6.0"
}

tasks.register<Exec>("compileDaml") {
    val requiredVersion = VersionFiles.damlYamlSdk
    commandLine("dpm", "build", "--all")
    environment("DPM_SDK_VERSION", requiredVersion)
}

tasks.register<Exec>("testDaml") {
    val requiredVersion = VersionFiles.damlYamlSdk
    commandLine("dpm", "test", "--package-root", "licensing-tests")
    environment("DPM_SDK_VERSION", requiredVersion)
}

tasks.register<com.digitalasset.transcode.codegen.java.gradle.JavaCodegenTask>("codeGen") {
    dar.from("$projectDir/licensing/.daml/dist/quickstart-licensing-0.0.1.dar")
    destination = file("$rootDir/backend/build/generated-daml-bindings")
    dependsOn("compileDaml")
}

tasks.named("build") {
    dependsOn("codeGen")
}
