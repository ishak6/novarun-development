package com.mangalocalrunnerplus

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectNamePolicyTest {
    @Test
    fun acceptsSafeProjectNamesAndTrimsWhitespace() {
        assertEquals("My Website-1.0", ProjectNamePolicy.validate("  My Website-1.0  "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsHiddenProjectNamesThatWouldDisappearFromWorkspace() {
        ProjectNamePolicy.validate(".hidden")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDotAndDotDotNames() {
        ProjectNamePolicy.validate("..")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPathSeparatorsAndTraversal() {
        ProjectNamePolicy.validate("../outside")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNamesLongerThanFiftyCharacters() {
        ProjectNamePolicy.validate("a".repeat(51))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankNames() {
        ProjectNamePolicy.validate("   ")
    }
}
