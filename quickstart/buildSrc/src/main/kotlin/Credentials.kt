import org.gradle.api.artifacts.repositories.PasswordCredentials

// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

/**
 * Provides functionality to parse a .netrc file and retrieve credentials
 * for a specified domain. This parser handles single-line and multi-line
 * token definitions, supports quoted strings, and skips `macdef` blocks
 * in accordance with the .netrc specification.
 *
 * @see <a href="https://curl.se/docs/manpage.html#netrc">.netrc specifications</a>
 */
object Credentials {
    /**
     * Returns an [Action] that sets the username and password of [PasswordCredentials]
     * based on the provided [domain]. If no matching domain is found,
     * this method will attempt to use `default` credentials if present.
     *
     * @param domain The domain (machine) to look up in the .netrc file.
     * @throws Exception If the .netrc file cannot be found or valid credentials
     * for the requested domain (or default) cannot be located.
     */
    fun fromNetRc(domain: String): org.gradle.api.Action<org.gradle.api.artifacts.repositories.PasswordCredentials> {
        val netrcFile = locateNetrcFile()
        val netrcContent = netrcFile.readText(Charsets.UTF_8)

        val tokens = tokenize(netrcContent)
        val machines = parseNetrcTokens(tokens)

        val machineMatch = machines.firstOrNull { it.name == domain }
        val defaultMatch = machines.firstOrNull { it.isDefault }

        val chosen = when {
            machineMatch != null && machineMatch.isComplete() -> machineMatch
            defaultMatch != null && defaultMatch.isComplete() -> defaultMatch
            else -> throw Exception("Can't find complete entry for domain '$domain' in ~/.netrc")
        }

        return object : org.gradle.api.Action<PasswordCredentials> {
            override fun execute(creds: PasswordCredentials) {
                creds.setUsername(chosen.login)
                creds.setPassword(chosen.password)
            }
        }
    }

    fun execFromNetRc(domain: String): Pair<String, String> {
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

        fromNetRc(domain).execute(passwordCredentials)

        val username = passwordCredentials.username
        val password = passwordCredentials.password

        if (username == null || password == null) {
            throw Exception("Failed to obtain credentials from .netrc")
        }
        return username to password
    }

    private fun locateNetrcFile(): java.io.File {
        val home = System.getProperty("user.home")
            ?: throw Exception("Cannot determine user home directory")
        val netrcUnix = java.io.File("$home/.netrc")
        val netrcWin = java.io.File("$home/_netrc")

        return when {
            netrcUnix.exists() -> netrcUnix
            netrcWin.exists() -> netrcWin
            else -> throw Exception("~/.netrc or ~/_netrc file not found")
        }
    }

    /**
     * Splits the entire .netrc file content into tokens, taking into account
     * quoted strings and escaped characters.
     */
    private fun tokenize(content: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < content.length) {
            // Skip whitespace outside of quotes
            if (content[i].isWhitespace()) {
                i++
                continue
            }

            // If we see a quote, consume a quoted token
            if (content[i] == '"') {
                val sb = StringBuilder()
                i++
                while (i < content.length) {
                    if (content[i] == '\\') {
                        // Handle escape
                        if (i + 1 < content.length) {
                            when (content[i + 1]) {
                                '"', '\\' -> {
                                    sb.append(content[i + 1])
                                    i += 2
                                }
                                'n' -> {
                                    sb.append('\n')
                                    i += 2
                                }
                                'r' -> {
                                    sb.append('\r')
                                    i += 2
                                }
                                't' -> {
                                    sb.append('\t')
                                    i += 2
                                }
                                else -> {
                                    sb.append(content[i + 1])
                                    i += 2
                                }
                            }
                        } else {
                            i++
                        }
                    } else if (content[i] == '"') {
                        i++
                        break
                    } else {
                        sb.append(content[i])
                        i++
                    }
                }
                tokens.add(sb.toString())
            } else {
                // Read an unquoted token until whitespace
                val start = i
                while (i < content.length && !content[i].isWhitespace()) {
                    i++
                }
                tokens.add(content.substring(start, i))
            }
        }
        return tokens
    }

    /**
     * Interprets tokens according to .netrc rules, including skipping
     * macdef blocks and assembling machine login/password data.
     */
    private fun parseNetrcTokens(tokens: List<String>): List<NetrcMachine> {
        val machines = mutableListOf<NetrcMachine>()
        var current: NetrcMachine? = null
        var index = 0

        while (index < tokens.size) {
            val token = tokens[index]

            if (token.equals("macdef", ignoreCase = true)) {
                // Skip until an empty line (or end of file)
                index++
                while (index < tokens.size) {
                    // The standard .netrc approach is to skip until an empty line;
                    // we approximate this by looking for a blank token or next machine.
                    val t = tokens[index]
                    if (t.isBlank() || t.equals("machine", ignoreCase = true) || t.equals("default", ignoreCase = true)) {
                        // Step back so the next loop iteration sees machine/default or blank
                        // (except if it's truly empty, then we skip it here).
                        if (t.isBlank()) {
                            index++
                        }
                        break
                    }
                    index++
                }
                continue
            }

            if (token.equals("machine", ignoreCase = true) || token.equals("default", ignoreCase = true)) {
                // Finalize previous machine if any
                if (current != null) {
                    machines.add(current)
                }
                val isDefault = token.equals("default", ignoreCase = true)
                val name = if (!isDefault) tokens.getOrNull(++index) else null
                current = NetrcMachine(name = name, isDefault = isDefault)
            } else if (token.equals("login", ignoreCase = true)) {
                current?.login = tokens.getOrNull(++index)
            } else if (token.equals("password", ignoreCase = true)) {
                current?.password = tokens.getOrNull(++index)
            }

            index++
        }
        // Add the last machine if present
        if (current != null) {
            machines.add(current)
        }
        return machines
    }

    /**
     * Represents a machine (or default) entry in a .netrc file.
     */
    private data class NetrcMachine(
        val name: String?,
        val isDefault: Boolean = false,
        var login: String? = null,
        var password: String? = null
    ) {
        fun isComplete(): Boolean {
            return !login.isNullOrBlank() && !password.isNullOrBlank()
        }
    }
}
