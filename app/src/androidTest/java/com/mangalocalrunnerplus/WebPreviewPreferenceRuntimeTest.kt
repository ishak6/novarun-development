package com.mangalocalrunnerplus

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebPreviewPreferenceRuntimeTest {
    @Test
    fun javascriptPreviewPreferencePersistsPerProject() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = File(context.cacheDir, "preview-js-first")
        val second = File(context.cacheDir, "preview-js-second")

        WebPreviewPreferenceStore.setJavaScriptEnabled(context, first, true)
        WebPreviewPreferenceStore.setJavaScriptEnabled(context, second, false)

        assertTrue(WebPreviewPreferenceStore.isJavaScriptEnabled(context, first))
        assertFalse(WebPreviewPreferenceStore.isJavaScriptEnabled(context, second))

        WebPreviewPreferenceStore.setJavaScriptEnabled(context, first, false)
        assertFalse(WebPreviewPreferenceStore.isJavaScriptEnabled(context, first))
    }
}
