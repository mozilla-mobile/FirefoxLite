package org.mozilla.rocket

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

open class CheckPermissionsTask : DefaultTask() {

    lateinit var permissionsFilePath: String
    lateinit var aapt2DirPath: String
    lateinit var apkFilePaths: List<File>

    @TaskAction
    fun checkPermissions() {
        backupPermissionsFile(permissionsFilePath)

        apkFilePaths.forEach { apkFile ->
            generatePermissionsFile(apkFile.absolutePath, permissionsFilePath, aapt2DirPath)
            val gitDiff = getGitDiff(permissionsFilePath)
            if (isNewLineAdded(gitDiff)) {
                throw Exception("new permissions added:\n$gitDiff")
            }
        }

        restorePermissionsFile(permissionsFilePath)
    }

    private fun backupPermissionsFile(permissionsFilePath: String) {
        val backupFile = File("$permissionsFilePath.bak")
        File(permissionsFilePath).copyTo(backupFile, overwrite = true)
    }

    private fun restorePermissionsFile(permissionsFilePath: String) {
        val backupFile = File("$permissionsFilePath.bak")
        backupFile.copyTo(File(permissionsFilePath), overwrite = true)
        backupFile.delete()
    }

    private fun generatePermissionsFile(apkFilePath: String, destFilePath: String, aapt2DirPath: String) {
        val permissionsText = "${aapt2DirPath}aapt2 dump permissions $apkFilePath".runCommand()
        val trimmedText = if (permissionsText.isNotEmpty()) {
            // Ignore package name in the first line
            permissionsText.substringAfter('\n')
        } else {
            permissionsText
        }
        File(destFilePath)
                .also { it.delete() }
                .also { it.createNewFile() }
                .writeText(trimmedText)
    }

    private fun isNewLineAdded(gitDiff: String): Boolean = gitDiff.split('\n')
            .any { it.startsWith("+    ") }

    private fun String.runCommand(): String = runCommand(WORKING_DIR)

    private fun getGitDiff(filePath: String): String {
        return "git diff $filePath".runCommand()
    }

    companion object {
        private const val WORKING_DIR_PATH = "."
        private val WORKING_DIR = File(WORKING_DIR_PATH)
    }
}

@Throws(IOException::class)
private fun String.runCommand(workingDir: File): String {
    println("run command: $this")
    val parts = this.split("\\s".toRegex())
    val process = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

    process.waitFor(60, TimeUnit.MINUTES)
    return process.inputStream.bufferedReader().readText()
}