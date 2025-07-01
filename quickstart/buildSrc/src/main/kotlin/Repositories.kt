// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

object Repositories {
    val sonatype: Action<MavenArtifactRepository> = Action {
        url = java.net.URI("https://digitalasset.jfrog.io/artifactory/libs-release-local/")
        credentials(Credentials.fromNetRc("digitalasset.jfrog.io"))
    }
}