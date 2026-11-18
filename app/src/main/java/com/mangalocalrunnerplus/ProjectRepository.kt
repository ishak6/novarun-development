package com.mangalocalrunnerplus

import android.content.Context
import java.io.File
import java.io.IOException
import java.nio.file.Files

object ProjectRepository {

    private fun getProjectsDirectory(context: Context): File =
        File(context.filesDir, "projects")

    private fun ensureProjectsDirectory(context: Context): File {
        val appRoot = context.filesDir.canonicalFile
        val directory = getProjectsDirectory(context)
        if (Files.isSymbolicLink(directory.toPath())) {
            throw IOException("Projects directory cannot be a symbolic link.")
        }
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Could not create projects directory.")
        }
        val canonical = directory.canonicalFile
        if (!canonical.isDirectory || canonical.parentFile != appRoot) {
            throw IOException("Projects directory is outside NovaRun app storage.")
        }
        return canonical
    }

    fun getProjects(context: Context): List<File> {
        val directory = try {
            ensureProjectsDirectory(context)
        } catch (_: Exception) {
            return emptyList()
        }

        return directory.listFiles()
            ?.filter { file ->
                file.isDirectory &&
                    !file.name.startsWith(".") &&
                    !Files.isSymbolicLink(file.toPath()) &&
                    runCatching { file.canonicalFile.parentFile == directory }.getOrDefault(false)
            }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    @Throws(IOException::class)
    fun createProject(context: Context, rawName: String): File {
        val name = try {
            ProjectNamePolicy.validate(rawName)
        } catch (error: IllegalArgumentException) {
            throw IOException(error.message ?: "Invalid project name.", error)
        }
        val root = ensureProjectsDirectory(context)
        val project = File(root, name)
        if (project.exists()) {
            throw IOException("A project with this name already exists.")
        }
        if (!project.mkdir()) {
            throw IOException("Could not create the project directory.")
        }

        try {
            File(project, "index.html").writeText(
                """<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$name</title>
</head>
<body>
    <h1>Welcome to $name</h1>
    <p>Your project is ready.</p>
</body>
</html>""",
                Charsets.UTF_8
            )
            return project
        } catch (error: Exception) {
            project.deleteRecursively()
            if (error is IOException) throw error
            throw IOException("Could not initialize the new project.", error)
        }
    }

    fun getProjectCount(context: Context): Int = getProjects(context).size

    fun findProject(context: Context, projectName: String): File? =
        getProjects(context).firstOrNull { it.name == projectName }

    fun deleteProject(context: Context, projectName: String): Boolean {
        val project = findProject(context, projectName) ?: return false
        val root = runCatching { ensureProjectsDirectory(context) }.getOrNull() ?: return false
        if (Files.isSymbolicLink(project.toPath())) return false
        if (runCatching { project.canonicalFile.parentFile != root }.getOrDefault(true)) return false
        return project.deleteRecursively()
    }

    fun getProjectSize(project: File): Long {
        if (!project.isDirectory || Files.isSymbolicLink(project.toPath())) return 0L
        return project.walkTopDown()
            .filter { it.isFile && !Files.isSymbolicLink(it.toPath()) }
            .sumOf { it.length() }
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024L * 1024L * 1024L -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024L * 1024L * 1024L)} GB"
        }
    }
}
