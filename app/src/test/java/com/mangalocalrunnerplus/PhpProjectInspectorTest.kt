package com.mangalocalrunnerplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PhpProjectInspectorTest {
    @Test
    fun detectsLaravelComposerRequirementsScriptsAndPublicEntryPoint() {
        val root = Files.createTempDirectory("novarun-php").toFile()
        try {
            File(root, "composer.json").writeText(
                """
                {
                  "name": "demo/shop",
                  "require": {
                    "php": "^8.2",
                    "laravel/framework": "^11.0",
                    "ext-mbstring": "*",
                    "ext-openssl": "*"
                  },
                  "require-dev": {
                    "phpunit/phpunit": "^11.0"
                  },
                  "scripts": {
                    "post-install-cmd": "php artisan package:discover"
                  }
                }
                """.trimIndent()
            )
            File(root, "composer.lock").writeText("{}")
            val report = PhpProjectInspector.inspect(
                File(root, "composer.json"),
                setOf("composer.json", "composer.lock", "public/index.php", "artisan")
            )
            assertEquals("demo/shop", report.packageName)
            assertEquals("Laravel", report.framework)
            assertEquals("^8.2", report.phpVersionConstraint)
            assertEquals("composer.lock", report.lockfileName)
            assertEquals(1, report.dependencyCount)
            assertEquals(1, report.devDependencyCount)
            assertEquals(listOf("ext-mbstring", "ext-openssl"), report.requiredExtensions)
            assertEquals("public/index.php", report.entryPoint)
            assertTrue(report.scripts.containsKey("post-install-cmd"))
            assertTrue(report.warnings.any { it.contains("explicit approval") })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun detectsMissingComposerLockAndEntryPoint() {
        val root = Files.createTempDirectory("novarun-php-min").toFile()
        try {
            File(root, "composer.json").writeText("""{"require":{"php":">=8.1","slim/slim":"^4.0"}}""")
            val report = PhpProjectInspector.inspect(File(root, "composer.json"), setOf("composer.json"))
            assertEquals("Slim", report.framework)
            assertEquals(null, report.lockfileName)
            assertEquals(null, report.entryPoint)
            assertTrue(report.warnings.any { it.contains("composer.lock is missing") })
            assertTrue(report.warnings.any { it.contains("entry point") })
        } finally {
            root.deleteRecursively()
        }
    }
}
