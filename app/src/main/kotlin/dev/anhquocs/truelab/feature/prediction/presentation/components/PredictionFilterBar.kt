package dev.anhquocs.truelab.feature.prediction.presentation.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.league.model.League
import dev.anhquocs.truelab.core.domain.match.model.PredictionStatusFilter
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.theme.SpacingXXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s11
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.semiBold
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Filter Bar component for the Prediction Screen.
 *
 * Polished visual structure:
 * 1. Compact Date Navigation Bar: [←] Day Status / Date [→]
 * 2. Competition Filter (Opens ModalBottomSheet) + Status Filter Chips (Horizontal Scrollable)
 * 3. Modern Dark Search Bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionFilterBar(
    selectedDate: String,
    onDateChanged: (String) -> Unit,
    leagues: List<League>,
    selectedLeagueId: Int?,
    onLeagueSelected: (Int?) -> Unit,
    selectedStatusFilter: PredictionStatusFilter,
    onStatusFilterSelected: (PredictionStatusFilter) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showLeagueBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentDateObj = try {
        LocalDate.parse(selectedDate)
    } catch (_: Exception) {
        LocalDate.now()
    }
    val todayObj = LocalDate.now()

    if (showLeagueBottomSheet) {
        CompetitionBottomSheet(
            sheetState = sheetState,
            leagues = leagues,
            selectedLeagueId = selectedLeagueId,
            onLeagueSelected = onLeagueSelected,
            onDismiss = { showLeagueBottomSheet = false }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SpacingS)
    ) {
        // --- 1. Compact Date Navigation Bar ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(RadiusLarge),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimen.PaddingS, vertical = SpacingXS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Previous Day
                IconButton(
                    onClick = {
                        val prev = currentDateObj.minusDays(1)
                        onDateChanged(prev.toString())
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Day",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimen.SizeSM)
                    )
                }

                // Center: Tap to open DatePicker
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimen.PaddingS))
                        .clickable {
                            val year = currentDateObj.year
                            val month = currentDateObj.monthValue - 1
                            val day = currentDateObj.dayOfMonth
                            DatePickerDialog(
                                context,
                                { _, selectedYear, selectedMonth, selectedDay ->
                                    val pickedDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                                    onDateChanged(pickedDate.toString())
                                },
                                year,
                                month,
                                day
                            ).show()
                        }
                        .padding(horizontal = Dimen.PaddingM, vertical = SpacingXXS)
                ) {
                    val statusText = when (currentDateObj) {
                        todayObj -> stringResource(R.string.prediction_today)
                        todayObj.minusDays(1) -> stringResource(R.string.prediction_yesterday)
                        todayObj.plusDays(1) -> stringResource(R.string.prediction_tomorrow)
                        else -> currentDateObj.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.s13.bold(),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = currentDateObj.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        style = MaterialTheme.typography.s11.medium(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Next Day
                IconButton(
                    onClick = {
                        val next = currentDateObj.plusDays(1)
                        onDateChanged(next.toString())
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Day",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimen.SizeSM)
                    )
                }
            }
        }

        // --- 2. Competition Filter & Status Filter Chips ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SpacingS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Competition Selector Chip (opens BottomSheet)
            val currentLeague = leagues.find { it.id == selectedLeagueId }
            val competitionLabel = currentLeague?.name ?: stringResource(R.string.prediction_all_competitions)

            FilterChip(
                selected = selectedLeagueId != null,
                onClick = { showLeagueBottomSheet = true },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = competitionLabel,
                            style = MaterialTheme.typography.s12.medium(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(SpacingXXS))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                },
                trailingIcon = if (selectedLeagueId != null) {
                    {
                        IconButton(
                            onClick = { onLeagueSelected(null) },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear League",
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                } else null,
                shape = RoundedCornerShape(RadiusPill),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            // Status Filter Chip: LIVE & Upcoming
            FilterChip(
                selected = selectedStatusFilter == PredictionStatusFilter.LIVE_AND_UPCOMING,
                onClick = { onStatusFilterSelected(PredictionStatusFilter.LIVE_AND_UPCOMING) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                        Spacer(modifier = Modifier.width(SpacingXXS))
                        Text(
                            text = stringResource(R.string.prediction_filter_live_upcoming),
                            style = MaterialTheme.typography.s12.medium()
                        )
                    }
                },
                shape = RoundedCornerShape(RadiusPill),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Status Filter Chip: LIVE
            FilterChip(
                selected = selectedStatusFilter == PredictionStatusFilter.LIVE,
                onClick = { onStatusFilterSelected(PredictionStatusFilter.LIVE) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                        Spacer(modifier = Modifier.width(SpacingXXS))
                        Text(
                            text = stringResource(R.string.prediction_filter_live),
                            style = MaterialTheme.typography.s12.medium()
                        )
                    }
                },
                shape = RoundedCornerShape(RadiusPill),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Status Filter Chip: Upcoming
            FilterChip(
                selected = selectedStatusFilter == PredictionStatusFilter.UPCOMING,
                onClick = { onStatusFilterSelected(PredictionStatusFilter.UPCOMING) },
                label = {
                    Text(
                        text = stringResource(R.string.prediction_filter_upcoming),
                        style = MaterialTheme.typography.s12.medium()
                    )
                },
                shape = RoundedCornerShape(RadiusPill),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Status Filter Chip: All
            FilterChip(
                selected = selectedStatusFilter == PredictionStatusFilter.ALL,
                onClick = { onStatusFilterSelected(PredictionStatusFilter.ALL) },
                label = {
                    Text(
                        text = stringResource(R.string.prediction_filter_all),
                        style = MaterialTheme.typography.s12.medium()
                    )
                },
                shape = RoundedCornerShape(RadiusPill),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        // --- 3. Compact Search Bar ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = {
                Text(
                    text = stringResource(R.string.prediction_search_hint),
                    style = MaterialTheme.typography.s12,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimen.SizeSM)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(RadiusLarge),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}
