package com.mangalocalrunnerplus

import android.content.Context
import java.io.File

object WebPreviewPreferenceStore {
    private const val PREFERENCES = "novarun_preview"
    private const val KEY_PREFIX = "javascript_enabled_"

    fun isJavaScriptEnabled(context: Context, projectDirectory: File): Boolean =
        preferences(context).getBoolean(key(projectDirectory), true)

    fun setJavaScriptEnabled(context: Context, projectDirectory: File, enabled: Boolean) {
        preferences(context).edit().putBoolean(key(projectDirectory), enabled).apply()
    }

    fun isExternalNetworkAllowed(context: Context, projectDirectory: File): Boolean =
        preferences(context).getBoolean(externalNetworkKey(projectDirectory), false)

    fun setExternalNetworkAllowed(context: Context, projectDirectory: File, allowed: Boolean) {
        preferences(context).edit().putBoolean(externalNetworkKey(projectDirectory), allowed).apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private fun key(projectDirectory: File): String =
        KEY_PREFIX + projectDirectory.absolutePath

    private fun externalNetworkKey(projectDirectory: File): String =
        "external_network_allowed_" + projectDirectory.absolutePath
}
