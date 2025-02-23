// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

buildscript {
    repositories {
        mavenCentral()
        maven(Repositories.sonatype)
        mavenLocal()
    }
    dependencies {
        classpath(Deps.transcode.javaCodegen)
        classpath(Deps.transcode.typescriptCodegen)
        classpath("org.apache.commons:commons-compress:1.27.1")
    }
}

plugins {
    id("base")
    id("de.undercouch.download") version "5.6.0"
}

tasks.register<Exec>("compileDaml") {
    commandLine("daml", "damlc", "build", "--all")
}

tasks.register<com.digitalasset.transcode.codegen.java.gradle.JavaCodegenTask>("javaCodeGen") {
    dar = file("$projectDir/licensing/.daml/dist/quickstart-licensing-0.0.1.dar")
    destination = file("$rootDir/backend/build/generated-daml-bindings")
    dependsOn("compileDaml")
}

tasks.register<com.digitalasset.transcode.codegen.typescript.gradle.TypescriptCodegenTask>("typescriptCodeGen") {
    dar = file("$projectDir/licensing/.daml/dist/quickstart-licensing-0.0.1.dar")
    destination = file("$rootDir/frontend/generated")
    dependsOn("compileDaml")
}

tasks.named("build") {
    dependsOn("javaCodeGen")
    dependsOn("typescriptCodeGen")
}

// Helper function to compute SDK variables
fun computeSdkVariables(): Map<String, Any> {
    val osName = System.getProperty("os.name").lowercase()
    val osArch = System.getProperty("os.arch").lowercase()
    val isWindows = osName.contains("win")
    val isMac = osName.contains("mac")
    val isUnix = osName.contains("nix") || osName.contains("nux") || osName.contains("aix")

    val sdkOs = when {
        isWindows -> "windows"
        isMac -> "macos"
        isUnix -> when {
            osArch.contains("arm") || osArch.contains("aarch64") -> "linux-arm"
            else -> "linux-intel"
        }

        else -> throw Exception("Unsupported OS: $osName")
    }

    val damlSdkRuntimeVersion = VersionFiles.dotenv["DAML_RUNTIME_VERSION"] as String
    val damlSdkVersion = VersionFiles.damlYamlSdk
    val sdkArchive = "daml-sdk-$damlSdkRuntimeVersion-$sdkOs.tar.gz"
    val sdkUrl = "https://digitalasset.jfrog.io/artifactory/assembly/daml/$damlSdkRuntimeVersion/$sdkArchive"
    val sdkDir = file("$projectDir/.sdk")
    val sdkArchiveFile = file("${sdkDir}/${sdkArchive}")
    val extractedDir = file("${sdkDir}/extracted")

    return mapOf(
        "damlSdkRuntimeVersion" to damlSdkRuntimeVersion,
        "damlSdkVersion" to damlSdkVersion,
        "sdkOs" to sdkOs,
        "sdkArchive" to sdkArchive,
        "sdkUrl" to sdkUrl,
        "sdkDir" to sdkDir,
        "sdkArchiveFile" to sdkArchiveFile,
        "extractedDir" to extractedDir
    )
}

// Task to download the SDK archive
tasks.register<de.undercouch.gradle.tasks.download.Download>("fetchDamlSdk") {
    val sdkVars = computeSdkVariables()
    val sdkUrl = sdkVars["sdkUrl"] as String
    val sdkArchiveFile = sdkVars["sdkArchiveFile"] as File
    val sdkDir = sdkVars["sdkDir"] as File

    src(sdkUrl)
    dest(sdkArchiveFile)
    overwrite(false)
    onlyIfModified(true)

    doFirst {
        sdkDir.mkdirs()
        // Use Credentials.fromNetRc to get username and password
        val passwordCredentials = object : PasswordCredentials {
            private var usernameField: String? = null
            private var passwordField: String? = null

            override fun getUsername(): String? = usernameField
            override fun setUsername(username: String?) {
                usernameField = username
            }

            override fun getPassword(): String? = passwordField
            override fun setPassword(password: String?) {
                passwordField = password
            }
        }

        // Apply the action to populate username and password
        Credentials.fromNetRc("digitalasset.jfrog.io").execute(passwordCredentials)

        val username = passwordCredentials.username
        val password = passwordCredentials.password

        if (username == null || password == null) {
            throw Exception("Failed to obtain credentials from .netrc")
        }

        username(username)
        password(password)
    }

    doLast {
        println("Downloaded $sdkUrl to ${sdkArchiveFile.absolutePath}")
    }
}

// Task to unpack the SDK archive
tasks.register<UnpackTarGzTask>("unpackDamlSdk") {
    dependsOn("fetchDamlSdk")
    val sdkVars = computeSdkVariables()
    archiveFile = sdkVars["sdkArchiveFile"] as File
    destinationDir = sdkVars["extractedDir"] as File
}



// Task to run the install script
tasks.register<Exec>("installDamlSdk") {
    dependsOn("unpackDamlSdk")
    val sdkVars = computeSdkVariables()
    val sdkDir = sdkVars["sdkDir"] as File
    val extractedDir = sdkVars["extractedDir"] as File
    val damlSdkRuntimeVersion = sdkVars["damlSdkRuntimeVersion"] as String
    val damlSdkVersion = sdkVars["damlSdkVersion"] as String
    val sdkOs = sdkVars["sdkOs"] as String

    doFirst {
        val topLevelDirs = extractedDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
        if (topLevelDirs.isEmpty()) {
            throw Exception("No directories found in $extractedDir")
        }
        workingDir = topLevelDirs.first()
    }
    commandLine(
        if (sdkOs == "windows") "./install.bat" else "./install.sh",
        "--install-with-custom-version",
        damlSdkVersion
    )
    doLast {
        println("Installed Daml SDK runtime $damlSdkRuntimeVersion as $damlSdkVersion")
        // Clean up extracted files
        println("Cleaning up downloaded files")
        sdkDir.deleteRecursively()
    }
}
