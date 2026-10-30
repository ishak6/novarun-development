
package com.mangalocalrunnerplus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RuntimeStatusRoute() {
    val context = LocalContext.current

    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    val runtimeResults = remember(context, refreshKey) {
        RuntimeStatusChecker.checkAll(context)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    refreshKey++
                }
            ) {
                Text("Refresh Status")
            }
        }

        RuntimeStatusScreen(
            results = runtimeResults,
            modifier = Modifier.weight(1f)
        )
    }
}
