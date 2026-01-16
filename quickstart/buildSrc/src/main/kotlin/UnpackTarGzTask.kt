// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

// We are using this instead of Gradle's Copy task or Ant's Tar task because neither support symlinks
open class UnpackTarGzTask : DefaultTask() {

    @get:InputFile
    lateinit var archiveFile: File

    @get:OutputDirectory
    lateinit var destinationDir: File

    @TaskAction
    fun unpack() {
        if (destinationDir.exists()) {
            destinationDir.deleteRecursively()
        }
        extractTarGz(archiveFile, destinationDir)
        println("Unpacked SDK archive to ${destinationDir.absolutePath}")
    }

    private fun extractTarGz(archive: File, destinationDir: File) {
        FileInputStream(archive).use { fis ->
            GzipCompressorInputStream(fis).use { gzis ->
                TarArchiveInputStream(gzis).use { tais ->
                    var entry: TarArchiveEntry? = tais.nextTarEntry
                    while (entry != null) {
                        val entryName = entry.name
                        val outputFile = File(destinationDir, entryName)
                        if (entry.isDirectory) {
                            outputFile.mkdirs()
                        } else if (entry.isSymbolicLink) {
                            val linkTarget = entry.linkName
                            val linkPath = outputFile.toPath()
                            Files.createDirectories(linkPath.parent)
                            Files.createSymbolicLink(linkPath, File(linkTarget).toPath())
                        } else {
                            // Regular file
                            outputFile.parentFile.mkdirs()
                            FileOutputStream(outputFile).use { fos ->
                                tais.copyTo(fos)
                            }
                        }

                        if (!entry.isSymbolicLink) {
                            // Set permissions
                            val mode = entry.mode
                            val permissions = mutableSetOf<PosixFilePermission>()
                            if ((mode and 0b100000000) != 0) permissions.add(PosixFilePermission.OWNER_READ)
                            if ((mode and 0b010000000) != 0) permissions.add(PosixFilePermission.OWNER_WRITE)
                            if ((mode and 0b001000000) != 0) permissions.add(PosixFilePermission.OWNER_EXECUTE)
                            if ((mode and 0b000100000) != 0) permissions.add(PosixFilePermission.GROUP_READ)
                            if ((mode and 0b000010000) != 0) permissions.add(PosixFilePermission.GROUP_WRITE)
                            if ((mode and 0b000001000) != 0) permissions.add(PosixFilePermission.GROUP_EXECUTE)
                            if ((mode and 0b000000100) != 0) permissions.add(PosixFilePermission.OTHERS_READ)
                            if ((mode and 0b000000010) != 0) permissions.add(PosixFilePermission.OTHERS_WRITE)
                            if ((mode and 0b000000001) != 0) permissions.add(PosixFilePermission.OTHERS_EXECUTE)
                            try {
                                Files.setPosixFilePermissions(outputFile.toPath(), permissions)
                            } catch (e: UnsupportedOperationException) {
                                // Ignore if the file system does not support POSIX file permissions
                            }
                        }

                        entry = tais.nextEntry
                    }
                }
            }
        }
    }
}
