package com.mangalocalrunnerplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NodeLaunchPlanTest {
    private fun project(block: (File) -> Unit): File {
        val root = createTempDir(prefix = "novarun-node-test-")
        block(root)
        return root
    }

    @Test
    fun acceptsZeroDependencyServerAndUsesCanonicalEntry() {
        val root = project { File(it, "server.js").writeText("require('http').createServer().listen(3000)") }
        try {
            val result = NodeLaunchPlan.create(root, 3000)
            assertTrue(result.isSuccess)
            assertEquals(File(root, "server.js").canonicalFile, result.getOrThrow().entry)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun discoversNestedServerEntry() {
        val root = project {
            File(it, "src").mkdirs()
            File(it, "src/server.js").writeText("console.log('ready')")
        }
        try {
            val result = NodeLaunchPlan.create(root, 8080)
            assertTrue(result.isSuccess)
            assertEquals(File(root, "src/server.js").canonicalFile, result.getOrThrow().entry)
        } finally {
            root.deleteRecursively()
        }
    }


    @Test
    fun detectsNestedZipWrapperProjectRoot() {
        val root = project {
            val nested = File(it, "clothing_store_secure")
            assertTrue(nested.mkdirs())
            File(nested, "server.js").writeText("console.log('ready')")
            File(nested, "package.json").writeText("""{"engines":{"node":">=22.13.0"}}""")
        }
        try {
            val result = NodeLaunchPlan.create(root, 3000)
            assertTrue(result.exceptionOrNull()?.message.orEmpty(), result.isSuccess)
            assertEquals(File(root, "clothing_store_secure").canonicalFile, result.getOrThrow().root)
            assertEquals(File(root, "clothing_store_secure/server.js").canonicalFile, result.getOrThrow().entry)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun acceptsTargetZipNodeEngineRequirement() {
        val root = project {
            File(it, "server.js").writeText("console.log('ready')")
            File(it, "package.json").writeText("""{"engines":{"node":">=22.13.0"}}""")
        }
        try {
            assertTrue(NodeLaunchPlan.create(root, 3000).isSuccess)
            assertEquals("24.22.1", NodeLaunchPlan.BUNDLED_NODE_VERSION)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsNodeEngineNewerThanBundledRuntime() {
        val root = project {
            File(it, "server.js").writeText("console.log('ready')")
            File(it, "package.json").writeText("""{"engines":{"node":">=25.0.0"}}""")
        }
        try {
            val result = NodeLaunchPlan.create(root, 3000)
            assertFalse(result.isSuccess)
            assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("requires Node.js"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsMissingEntryPoint() {
        val root = project { File(it, "README.md").writeText("not a server") }
        try {
            val result = NodeLaunchPlan.create(root, 3000)
            assertFalse(result.isSuccess)
            assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("No Node server entry"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsInvalidPort() {
        val root = project { File(it, "server.js").writeText("console.log('ready')") }
        try {
            val result = NodeLaunchPlan.create(root, 80)
            assertFalse(result.isSuccess)
            assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Port"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun refusesDeclaredDependenciesWithoutNodeModules() {
        val root = project {
            File(it, "server.js").writeText("console.log('ready')")
            File(it, "package.json").writeText("""{"dependencies":{"express":"^4.0.0"}}""")
        }
        try {
            val result = NodeLaunchPlan.create(root, 3000)
            assertFalse(result.isSuccess)
            assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("node_modules is missing"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun acceptsDependenciesWhenNodeModulesExists() {
        val root = project {
            File(it, "server.js").writeText("console.log('ready')")
            File(it, "package.json").writeText("""{"dependencies":{"express":"^4.0.0"}}""")
            File(it, "node_modules").mkdirs()
        }
        try {
            assertTrue(NodeLaunchPlan.create(root, 3000).isSuccess)
        } finally {
            root.deleteRecursively()
        }
    }
}
