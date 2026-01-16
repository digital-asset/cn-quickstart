// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import java.io.File

object VersionFiles {
    val dotenv: Map<String, String>
        get() {
            val rootProjectDir = File(System.getProperty("gradle.root.dir"))
            val dotEnvFile = File(rootProjectDir, ".env")

            if (!dotEnvFile.exists()) {
                throw IllegalStateException(".env file not found in project root directory" +
                        " (expected at ${dotEnvFile.absolutePath})")
            }

            return parseDotEnvFile(dotEnvFile)
        }

    val damlYamlSdk: String
        get() {
            val rootProjectDir = File(System.getProperty("gradle.root.dir"))
            val damlYamlFile = File(rootProjectDir, "daml/licensing/daml.yaml")

            if (!damlYamlFile.exists()) {
                throw IllegalStateException("daml.yaml file not found in daml directory" +
                        " (expected at ${damlYamlFile.absolutePath})")
            }

            return parseDamlYamlFile(damlYamlFile)
        }

    private fun parseDotEnvFile(file: File): Map<String, String> {
        return file.readLines()
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val index = line.indexOf('=')
                if (index != -1) {
                    val key = line.substring(0, index).trim()
                    val value = line.substring(index + 1).trim()
                    key to value
                } else {
                    null
                }
            }
            .toMap()
    }

    private fun parseDamlYamlFile(file: File): String {
        return file.readLines()
            .asSequence()
            .map { line ->
                // Remove inline comments
                line.split("#")[0].trim()
            }
            .filter { it.isNotEmpty() }
            .firstOrNull { it.startsWith("sdk-version:") }
            ?.substringAfter(":")
            ?.trim()
            ?: throw IllegalStateException("sdk-version not found in daml.yaml")
    }
}
