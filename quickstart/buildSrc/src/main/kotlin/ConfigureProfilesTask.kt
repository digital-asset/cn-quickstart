// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class ConfigureProfilesTask : DefaultTask() {

    enum class OptionType { BOOLEAN, PARTY_HINT, AUTH_MODE, TEST_MODE }

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
            Option("Enable Observability", "OBSERVABILITY_ENABLED", OptionType.BOOLEAN),
            Option("Enable OAUTH2", "AUTH_MODE", OptionType.AUTH_MODE),
            Option(
                "Specify a party hint (this will identify the participant in the network)",
                "PARTY_HINT",
                OptionType.PARTY_HINT
            ),
            Option("Enable TEST_MODE", "TEST_MODE", OptionType.TEST_MODE),
        )

        options.forEach { option ->
            when (option.type) {
                OptionType.BOOLEAN -> {
                    val boolValue = promptForBoolean(option.promptText, default = true)
                    option.value = boolValue.toString()
                    println("  ${option.envVarName} set to '$boolValue'.\n")
                }

                OptionType.AUTH_MODE -> {
                    val boolValue = promptForBoolean(option.promptText, default = true)
                    option.value = if (boolValue) {
                        "oauth2"
                    } else {
                        "shared-secret"
                    }
                    println("  ${option.envVarName} set to '${option.value}'.\n")
                }

                OptionType.PARTY_HINT -> {
                    val stringValue = promptForPartyHint(option.promptText)
                    option.value = stringValue
                    println("  ${option.envVarName} set to '$stringValue'.\n")
                }

                OptionType.TEST_MODE -> {
                    val boolValue = promptForBoolean(option.promptText, default = false)
                    option.value = if (boolValue) {
                        "on"
                    } else {
                        "off"
                    }
                    println("  ${option.envVarName} set to '${option.value}'.\n")
                    if (boolValue)
                        println (
                            """
                            CAUTION: Not intended for use in production environments.
                            Activates the test profile in the backend service.
                            When enabled, party ID resolution is derived from the JWT token's party_id claim, overriding the tenant registration's party ID.
                            This feature is designed for testing purposes to generate a unique AppUser party for each test run and ensure isolation.                            
                            """.trimIndent()
                        )
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
        // The user input needs to match "<organization>-<function>-<enumerator>"
        // where <organization> and <function> are alphabetical and <enumerator> is numeric.
        val validPattern = Regex("^[A-Za-z]+-[A-Za-z]+-\\d+\$")

        // Grab either $USER or $USERNAME from the environment
        val rawUser = System.getenv("USER") ?: System.getenv("USERNAME") ?: ""

        // Clean up rawUser to keep only letters
        val cleanedUser = rawUser.replace(Regex("[^A-Za-z]"), "")

        // If a cleaned user exists, use "quickstart-$cleanedUser-1" as default
        // Otherwise, no default is provided (we'll force user input).
        val defaultPartyHint = if (cleanedUser.isNotEmpty()) {
            "quickstart-$cleanedUser-1"
        } else {
            ""
        }

        // If there's a valid default, show it in brackets; otherwise, show no brackets.
        val fullPrompt = if (defaultPartyHint.isNotEmpty()) {
            "$prompt [$defaultPartyHint]"
        } else {
            prompt
        }

        while (true) {
            print("$fullPrompt: ")
            System.out.flush()

            // Read user input
            val input = readLine().orEmpty().trim()

            // If no input was provided but a default exists, use the default.
            val candidate = if (input.isEmpty() && defaultPartyHint.isNotEmpty()) {
                defaultPartyHint
            } else {
                input
            }

            // If there's no default and the user provided nothing, force them to try again.
            if (candidate.isEmpty()) {
                println("No default is available. You must enter a valid party hint.")
                continue
            }

            // Now validate "<organization>-<function>-<enumerator>"
            if (!validPattern.matches(candidate)) {
                println(
                    """
                Invalid party hint. Must match "<organization>-<function>-<enumerator>"
                where <organization> and <function> are alphabetical, and <enumerator> is numeric.
                """.trimIndent()
                )
            } else {
                return candidate
            }
        }
    }

}
