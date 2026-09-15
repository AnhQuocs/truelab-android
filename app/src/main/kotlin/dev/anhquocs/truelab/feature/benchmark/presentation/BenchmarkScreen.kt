package dev.anhquocs.truelab.feature.benchmark.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.ButtonHeightMedium
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingL
import dev.anhquocs.truelab.core.ui.theme.SpacingM
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXL
import dev.anhquocs.truelab.core.ui.theme.SpacingXS

@Composable
fun BenchmarkScreen(
    modifier: Modifier = Modifier
) {
    var selectedDatasetSizeIndex by remember { mutableIntStateOf(1) }
    val datasetSizes = listOf(1_000, 10_000, 50_000)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = SpacingL),
        verticalArrangement = Arrangement.spacedBy(SpacingM),
        contentPadding = PaddingValues(bottom = SpacingXL)
    ) {
        item {
            Spacer(modifier = Modifier.height(SpacingS))
            Text(
                text = stringResource(R.string.benchmark_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            DatasetSizeSelector(
                sizes = datasetSizes,
                selectedIndex = selectedDatasetSizeIndex,
                onSelectIndex = { selectedDatasetSizeIndex = it }
            )
        }

        item {
            AlgorithmBenchmarkCard(
                title = stringResource(R.string.benchmark_search_title),
                algo1Name = "Linear Search",
                algo1Time = "1.420 ms",
                algo1Complexity = "O(N)",
                algo2Name = "Binary Search",
                algo2Time = "0.015 ms",
                algo2Complexity = "O(log N)"
            )
        }

        item {
            AlgorithmBenchmarkCard(
                title = stringResource(R.string.benchmark_sort_title),
                algo1Name = "QuickSort (In-Place)",
                algo1Time = "8.350 ms",
                algo1Complexity = "O(N log N)",
                algo2Name = "MergeSort",
                algo2Time = "11.210 ms",
                algo2Complexity = "O(N log N)"
            )
        }

        item {
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ButtonHeightMedium),
                shape = RoundedCornerShape(RadiusMedium)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = SpacingXS))
                Text(
                    text = stringResource(R.string.benchmark_run_btn),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DatasetSizeSelector(
    sizes: List<Int>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Dataset Size (Matches / Entities):",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(SpacingXS))
        Row(horizontalArrangement = Arrangement.spacedBy(SpacingS)) {
            sizes.forEachIndexed { index, size ->
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = { onSelectIndex(index) },
                    label = { Text("%,d items".format(size)) }
                )
            }
        }
    }
}

@Composable
private fun AlgorithmBenchmarkCard(
    title: String,
    algo1Name: String,
    algo1Time: String,
    algo1Complexity: String,
    algo2Name: String,
    algo2Time: String,
    algo2Complexity: String
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingL),
            verticalArrangement = Arrangement.spacedBy(SpacingM)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            AlgoResultRow(name = algo1Name, time = algo1Time, complexity = algo1Complexity)
            AlgoResultRow(name = algo2Name, time = algo2Time, complexity = algo2Complexity)
        }
    }
}

@Composable
private fun AlgoResultRow(
    name: String,
    time: String,
    complexity: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingM),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = complexity,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}
