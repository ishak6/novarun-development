package com.mangalocalrunnerplus

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewLoadIdentityTest {
    @Test
    fun identityChangesWhenProjectUrlOrReloadKeyChanges() {
        val original = PreviewLoadIdentity("/projects/one", "http://127.0.0.1:18080/", 0)
        assertEquals(original, PreviewLoadIdentity("/projects/one", "http://127.0.0.1:18080/", 0))
        assertNotEquals(original, PreviewLoadIdentity("/projects/two", "http://127.0.0.1:18080/", 0))
        assertNotEquals(original, PreviewLoadIdentity("/projects/one", "http://127.0.0.1:18081/", 0))
        assertNotEquals(original, PreviewLoadIdentity("/projects/one", "http://127.0.0.1:18080/", 1))
    }
}
