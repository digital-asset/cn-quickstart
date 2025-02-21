// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class ConfigureProfilesTask : DefaultTask() {

    enum class OptionType { BOOLEAN, PARTY_HINT }

    data class Option(
        val promptText: String,
        val envVarName: String,
        val type: OptionType,
        var value: String = "",
        var isFound: Boolean = false
    )

    init {
        inputs.property("standardInput", System.`in`)
    }

    @TaskAction
    fun configure() {
        val options = listOf(
            Option("Enable LocalNet", "LOCALNET_ENABLED", OptionType.BOOLEAN),
            Option("Enable Observability", "OBSERVABILITY_ENABLED", OptionType.BOOLEAN),
            Option(
                "Specify a party hint (this will identify the participant in the network)",
                "PARTY_HINT",
                OptionType.PARTY_HINT
            )
        )

        options.forEach { option ->
            when (option.type) {
                OptionType.BOOLEAN -> {
                    val boolValue = promptForBoolean(option.promptText, default = true)
                    option.value = boolValue.toString()
                    println("  ${option.envVarName} set to '$boolValue'.\n")
                }

                OptionType.PARTY_HINT -> {
                    val stringValue = promptForPartyHint(option.promptText)
                    option.value = stringValue
                    println("  ${option.envVarName} set to '$stringValue'.\n")
                }
            }
            System.out.flush()
        }

        val dotEnvFile = File(project.rootProject.projectDir, ".env.local").apply {
            if (!exists()) createNewFile()
        }
        val envLines = dotEnvFile.readLines().toMutableList()

        envLines.forEachIndexed { i, line ->
            options.forEach { option ->
                if (line.startsWith("${option.envVarName}=")) {
                    envLines[i] = "${option.envVarName}=${option.value}"
                    option.isFound = true
                }
            }
        }
        options.filterNot { it.isFound }.forEach {
            envLines.add("${it.envVarName}=${it.value}")
        }

        dotEnvFile.writeText(envLines.joinToString(System.lineSeparator()))
        println(".env.local updated successfully.")
    }

    private fun promptForBoolean(prompt: String, default: Boolean): Boolean {
        val optionsText = if (default) "Y/n" else "y/N"
        while (true) {
            print("$prompt? ($optionsText): ")
            System.out.flush()
            val input = readLine().orEmpty().trim()
            if (input.isEmpty()) return default
            when (input.lowercase()) {
                "y", "yes", "true", "t", "1" -> return true
                "n", "no", "false", "f", "0" -> return false
                else -> println("Invalid input. Please enter 'yes' or 'no'.")
            }
        }
    }

    private fun promptForPartyHint(prompt: String): String {
        // Regex for valid hint characters
        val pattern = Regex("^[A-Za-z0-9:\\-_]+\$")

        // Grab either $USER or $USERNAME from the environment
        val rawDefault = System.getenv("USER") ?: System.getenv("USERNAME") ?: ""
        // Remove any characters that don't match the valid pattern
        val defaultPartyHint = rawDefault.replace(Regex("[^A-Za-z0-9:\\-_]"), "")

        // If we found a valid default, include it in the prompt in brackets
        val fullPrompt = if (defaultPartyHint.isNotEmpty()) {
            "$prompt [$defaultPartyHint]"
        } else {
            prompt
        }

        while (true) {
            print("$fullPrompt: ")
            System.out.flush()

            // User input
            val input = readLine().orEmpty().trim()

            // If user didn't type anything but we have a default, use the default
            val candidate = if (input.isEmpty() && defaultPartyHint.isNotEmpty()) {
                defaultPartyHint
            } else {
                input
            }

            // Validate against the pattern
            if (candidate.isEmpty()) {
                println("Invalid party hint. You must enter a non-empty string.")
            } else if (pattern.matches(candidate)) {
                return candidate
            } else {
                println("Invalid party hint. Only use letters, digits, ':', '-' and '_'.")
            }
        }
    }

}
