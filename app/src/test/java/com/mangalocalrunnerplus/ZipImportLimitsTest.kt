package com.mangalocalrunnerplus

import org.junit.Assert.assertEquals
import org.junit.Test

class ZipImportLimitsTest {
    @Test
    fun zeroOrNegativePreferencesRestoreRecommendedLimits() {
        val actual = ZipImportLimits(0, 0L, -1L).bounded()

        assertEquals(ZipImportLimits.DEFAULT_MAX_ENTRIES, actual.maxEntries)
        assertEquals(ZipImportLimits.DEFAULT_MAX_TOTAL_BYTES, actual.maxTotalBytes)
        assertEquals(ZipImportLimits.DEFAULT_MAX_SINGLE_FILE_BYTES, actual.maxSingleFileBytes)
    }

    @Test
    fun smallerUserLimitsArePreserved() {
        val actual = ZipImportLimits(100, 10L * 1024L * 1024L, 5L * 1024L * 1024L).bounded()

        assertEquals(100, actual.maxEntries)
        assertEquals(10L * 1024L * 1024L, actual.maxTotalBytes)
        assertEquals(5L * 1024L * 1024L, actual.maxSingleFileBytes)
    }

    @Test
    fun valuesAboveHardCapsAreClamped() {
        val actual = ZipImportLimits(
            Int.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE
        ).bounded()

        assertEquals(ZipImportLimits.HARD_MAX_ENTRIES, actual.maxEntries)
        assertEquals(ZipImportLimits.HARD_MAX_TOTAL_BYTES, actual.maxTotalBytes)
        assertEquals(ZipImportLimits.HARD_MAX_SINGLE_FILE_BYTES, actual.maxSingleFileBytes)
    }
}
