
package com.mangalocalrunnerplus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun RuntimeStatusRoute() {
    val context = LocalContext.current

    val runtimeResults = remember(context) {
        RuntimeStatusChecker.checkAll(context)
    }

    RuntimeStatusScreen(
        results = runtimeResults
    )
}
