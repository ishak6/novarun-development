
package com.mangalocalrunnerplus

import android.content.Context
import java.io.File

object ProjectRepository {

    private fun getProjectsDirectory(context: Context): File {
        return File(context.filesDir, "projects")
    }

    fun getProjects(context: Context): List<File> {
        val directory = getProjectsDirectory(context)

        if (!directory.exists()) {
            directory.mkdirs()
        }

        return directory.listFiles()
            ?.filter { file ->
                file.isDirectory &&
                !file.name.startsWith(".")
            }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun getProjectCount(context: Context): Int {
        return getProjects(context).size
    }

    fun findProject(
        context: Context,
        projectName: String
    ): File? {
        return getProjects(context).firstOrNull {
            it.name == projectName
        }
    }

    fun deleteProject(
        context: Context,
        projectName: String
    ): Boolean {
        val project = findProject(context, projectName)
            ?: return false

        return project.deleteRecursively()
    }

    fun getProjectSize(project: File): Long {
        if (!project.exists()) return 0L

        return project.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 ->
                "${bytes / 1024} KB"
            bytes < 1024L * 1024L * 1024L ->
                "${bytes / (1024 * 1024)} MB"
            else ->
                "${bytes / (1024L * 1024L * 1024L)} GB"
        }
    }
}
