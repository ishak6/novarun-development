package com.mangalocalrunnerplus

import android.net.Uri

object PreviewUrlPolicy {
    fun isAllowed(
        uri: Uri,
        allowedPort: Int,
        allowExternalNetwork: Boolean
    ): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme in setOf("about", "data", "blob")) return true
        if (scheme !in setOf("http", "https")) return false

        val host = uri.host?.lowercase() ?: return false
        if (host == "127.0.0.1" || host == "localhost") {
            return scheme == "http" && allowedPort in 1..65535 && uri.port == allowedPort
        }

        // External cleartext HTTP is never permitted by preview policy.
        return scheme == "https" && allowExternalNetwork
    }
}
