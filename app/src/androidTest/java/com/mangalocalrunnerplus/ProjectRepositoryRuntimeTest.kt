package com.mangalocalrunnerplus

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectRepositoryRuntimeTest {
    @Test
    fun createsListsAndDeletesProjectWithoutLeavingPartialFiles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "RepositoryTest-" + System.currentTimeMillis()
        var project: File? = null
        try {
            project = ProjectRepository.createProject(context, name)
            assertTrue(project.isDirectory)
            assertTrue(File(project, "index.html").isFile)
            assertTrue(ProjectRepository.getProjects(context).any { it.name == name })
            assertTrue(ProjectRepository.getProjectSize(project) > 0L)
            assertTrue(ProjectRepository.deleteProject(context, name))
            assertNull(ProjectRepository.findProject(context, name))
        } finally {
            project?.deleteRecursively()
        }
    }

    @Test
    fun workspaceDoesNotListOrDeleteProjectSymlinksOutsideItsRoot() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val projects = File(context.filesDir, "projects").apply { mkdirs() }
        val external = File(context.cacheDir, "novarun-external-project").apply {
            deleteRecursively()
            mkdirs()
        }
        File(external, "private.txt").writeText("must remain")
        val link = File(projects, "linked-project")
        try {
            Files.createSymbolicLink(link.toPath(), external.toPath())
            assertFalse(ProjectRepository.getProjects(context).any { it.name == link.name })
            assertFalse(ProjectRepository.deleteProject(context, link.name))
            assertTrue(File(external, "private.txt").exists())
        } finally {
            link.delete()
            external.deleteRecursively()
        }
    }
}
