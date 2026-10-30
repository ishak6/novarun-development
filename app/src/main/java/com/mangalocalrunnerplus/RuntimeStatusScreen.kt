
package com.mangalocalrunnerplus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RuntimeStatusScreen(
    results: List<RuntimeCheckResult>,
    modifier: Modifier = Modifier
) {
    val overview = remember(results) {
        RuntimeOverviewManager.create(results)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Runtime Status",
            color = Color(0xFFF5F7FA),
            fontSize = 24.sp,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Language runtime availability on this device.",
            color = Color(0xFF9AA6B5),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SummaryCard(
                    title = "Total Languages",
                    value = overview.totalLanguages.toString(),
                    color = Color(0xFF0A84FF)
                )
            }

            item {
                SummaryCard(
                    title = "Preview Available",
                    value = overview.previewAvailable.toString(),
                    color = Color(0xFF30D158)
                )
            }

            item {
                SummaryCard(
                    title = "Adapter Planned",
                    value = overview.adapterPlanned.toString(),
                    color = Color(0xFF0A84FF)
                )
            }

            item {
                SummaryCard(
                    title = "Not Configured",
                    value = overview.notConfigured.toString(),
                    color = Color(0xFFFF9F0A)
                )
            }

            item {
                SummaryCard(
                    title = "Not Supported",
                    value = overview.notSupported.toString(),
                    color = Color(0xFFFF453A)
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "LANGUAGE DETAILS",
                    color = Color(0xFF9AA6B5),
                    fontSize = 12.sp
                )
            }

            items(
                items = results,
                key = { it.language }
            ) { result ->
                RuntimeStatusCard(result)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF171D28)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                color = Color(0xFFF5F7FA),
                fontSize = 14.sp
            )

            Text(
                value,
                color = color,
                fontSize = 22.sp,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun RuntimeStatusCard(
    result: RuntimeCheckResult
) {
    val statusColor = remember(result.status) {
        when (result.status) {
            RuntimeStatus.AVAILABLE_FOR_PREVIEW ->
                Color(0xFF30D158)

            RuntimeStatus.ADAPTER_PLANNED ->
                Color(0xFF0A84FF)

            RuntimeStatus.NOT_CONFIGURED ->
                Color(0xFFFF9F0A)

            RuntimeStatus.NOT_SUPPORTED ->
                Color(0xFFFF453A)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF171D28)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.language,
                    color = Color(0xFFF5F7FA),
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = RuntimeStatusChecker.getStatusLabel(
                        result.status
                    ),
                    color = statusColor,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = result.message,
                color = Color(0xFF9AA6B5),
                fontSize = 13.sp
            )

            result.executablePath?.let { path ->
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Runtime directory: $path",
                    color = Color(0xFF0A84FF),
                    fontSize = 11.sp
                )
            }
        }
    }
}
