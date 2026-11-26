package com.mangalocalrunnerplus

import android.net.Uri
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
    fun externalNetworkAccessIsOptInAndIsolatedPerProject() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = File(context.cacheDir, "preview-network-first")
        val second = File(context.cacheDir, "preview-network-second")

        assertFalse(WebPreviewPreferenceStore.isExternalNetworkAllowed(context, first))
        assertFalse(WebPreviewPreferenceStore.isExternalNetworkAllowed(context, second))
        WebPreviewPreferenceStore.setExternalNetworkAllowed(context, first, true)
        assertTrue(WebPreviewPreferenceStore.isExternalNetworkAllowed(context, first))
        assertFalse(WebPreviewPreferenceStore.isExternalNetworkAllowed(context, second))
    }

    @Test
    fun previewUrlPolicyAllowsOnlyLoopbackByDefault() {
        val local = Uri.parse("http://127.0.0.1:18080/index.html")
        val wrongPort = Uri.parse("http://127.0.0.1:18081/index.html")
        val externalHttps = Uri.parse("https://example.com/app.js")
        val externalHttp = Uri.parse("http://example.com/app.js")
        val fileUrl = Uri.parse("file:///data/data/private.txt")

        assertTrue(PreviewUrlPolicy.isAllowed(local, 18080, false))
        assertFalse(PreviewUrlPolicy.isAllowed(wrongPort, 18080, false))
        assertFalse(PreviewUrlPolicy.isAllowed(externalHttps, 18080, false))
        assertTrue(PreviewUrlPolicy.isAllowed(externalHttps, 18080, true))
        assertFalse(PreviewUrlPolicy.isAllowed(externalHttp, 18080, true))
        assertFalse(PreviewUrlPolicy.isAllowed(fileUrl, 18080, true))
        assertTrue(PreviewUrlPolicy.isAllowed(Uri.parse("data:text/plain,local"), 18080, false))
    }

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
