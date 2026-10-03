package dev.anhquocs.truelab.feature.prediction.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionEvidence
import dev.anhquocs.truelab.core.domain.prediction.model.SignalEvidence
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.theme.SpacingXXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s11
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s15
import dev.anhquocs.truelab.core.ui.utils.semiBold
import java.util.Locale
import kotlin.math.abs

/**
 * Evidence section — flat "analysis flow" layout.
 * No Card containers. Each signal is a clickable flat row separated by dividers.
 * Expanding a row reveals comparison data inline, not inside a nested card.
 */
@Composable
fun PredictionEvidenceSection(
    evidence: PredictionEvidence,
    selectedMatch: Match,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        Text(
            text = stringResource(R.string.prediction_sheet_evidence_title),
            style = MaterialTheme.typography.s15.bold(),
            color = MaterialTheme.colorScheme.onSurface
        )
        val subtitle = stringResource(R.string.prediction_sheet_evidence_subtitle)
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(SpacingXXS))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.s11,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(Dimen.PaddingM))

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

        // 1. Elo
        val hElo = evidence.elo.details["homeElo"] ?: "1500.0"
        val aElo = evidence.elo.details["awayElo"] ?: "1500.0"
        val hCount = evidence.elo.details["homeHistoryCount"]?.toIntOrNull() ?: 0
        val aCount = evidence.elo.details["awayHistoryCount"]?.toIntOrNull() ?: 0

        val hEloDouble = hElo.toDoubleOrNull() ?: 1500.0
        val aEloDouble = aElo.toDoubleOrNull() ?: 1500.0
        val eloDiff = hEloDouble - aEloDouble
        val absEloDiff = abs(eloDiff)

        val eloDiffFormatted = if (absEloDiff < 0.05) {
            "0.0"
        } else {
            String.format(Locale.US, "+%.1f", absEloDiff)
        }

        val eloDiffColor = when {
            eloDiff > 0.05 -> MaterialTheme.colorScheme.primary
            eloDiff < -0.05 -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        FlatSignalRow(
            title = stringResource(R.string.prediction_signal_elo_title),
            evidence = evidence.elo,
            quickSummary = eloDiffFormatted,
            initiallyExpanded = true
        ) {
            val hSub = if (hCount > 0) stringResource(R.string.prediction_replay_history_format, hCount.toString())
            else stringResource(R.string.prediction_initial_history_format, "0")
            val aSub = if (aCount > 0) stringResource(R.string.prediction_replay_history_format, aCount.toString())
            else stringResource(R.string.prediction_initial_history_format, "0")

            FlatComparisonRow(
                homeLabel = stringResource(R.string.prediction_home_label),
                awayLabel = stringResource(R.string.prediction_away_label),
                homeValue = hElo,
                awayValue = aElo,
                homeSubtext = hSub,
                awaySubtext = aSub,
                centerValue = eloDiffFormatted,
                centerValueColor = eloDiffColor
            )
        }

        // 2. Form
        val hm = evidence.form.details["homeMatches"] ?: evidence.form.details["homeHistoryCount"] ?: "0"
        val am = evidence.form.details["awayMatches"] ?: evidence.form.details["awayHistoryCount"] ?: "0"
        val hf = evidence.form.details["homeForm"] ?: evidence.form.details["homeFormScore"]
        val af = evidence.form.details["awayForm"] ?: evidence.form.details["awayFormScore"]
        val formSummary = if (hm != "0" || am != "0") "${hf ?: "—"} / ${af ?: "—"}" else null

        FlatSignalRow(
            title = stringResource(R.string.prediction_signal_form_title),
            evidence = evidence.form,
            quickSummary = formSummary,
            initiallyExpanded = true
        ) {
            val homeFormResults = evidence.form.details["homeFormResults"].orEmpty()
            val awayFormResults = evidence.form.details["awayFormResults"].orEmpty()
            if (hm == "0" && am == "0") {
                Text(
                    text = stringResource(R.string.prediction_signal_no_history),
                    style = MaterialTheme.typography.s12,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val homeVal = if (hm == "0" || hf == null || hf == "N/A")
                    stringResource(R.string.prediction_no_data_short) else hf
                val awayVal = if (am == "0" || af == null || af == "N/A")
                    stringResource(R.string.prediction_no_data_short) else af

                FlatComparisonRow(
                    homeLabel = stringResource(R.string.prediction_home_label),
                    awayLabel = stringResource(R.string.prediction_away_label),
                    homeValue = homeVal,
                    awayValue = awayVal,
                    homeSubContent = if (homeFormResults.isNotBlank()) {
                        { FormBadgeSequence(results = homeFormResults, horizontalArrangement = Arrangement.Start) }
                    } else null,
                    awaySubContent = if (awayFormResults.isNotBlank()) {
                        { FormBadgeSequence(results = awayFormResults, horizontalArrangement = Arrangement.End) }
                    } else null,
                    homeSubtext = if (homeFormResults.isBlank()) null else homeFormResults,
                    awaySubtext = if (awayFormResults.isBlank()) null else awayFormResults
                )
            }
        }

        // 3. EU Odds
        val ho = evidence.odds.details["homeOdds"] ?: "N/A"
        val dro = evidence.odds.details["drawOdds"] ?: "N/A"
        val ao = evidence.odds.details["awayOdds"] ?: "N/A"
        val oddsSummary = if (evidence.odds.isAvailable) "$ho · $dro · $ao" else null

        FlatSignalRow(
            title = stringResource(R.string.prediction_signal_odds_title),
            evidence = evidence.odds,
            quickSummary = oddsSummary,
            emptyMessage = stringResource(R.string.prediction_signal_no_odds)
        ) {
            val bm = evidence.odds.details["bookmaker"] ?: "N/A"
            Column(verticalArrangement = Arrangement.spacedBy(SpacingXXS)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "1 · $ho", style = MaterialTheme.typography.s14.bold(), color = MaterialTheme.colorScheme.primary)
                    Text(text = "X · $dro", style = MaterialTheme.typography.s14.bold(), color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "2 · $ao", style = MaterialTheme.typography.s14.bold(), color = MaterialTheme.colorScheme.secondary)
                }
                Text(
                    text = "Nhà cái: $bm",
                    style = MaterialTheme.typography.s12,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4. Goals
        val hs = evidence.goals.details["homeScored"] ?: evidence.goals.details["homeMeanScored"]
        val hc = evidence.goals.details["homeConceded"] ?: evidence.goals.details["homeMeanConceded"]
        val as_ = evidence.goals.details["awayScored"] ?: evidence.goals.details["awayMeanScored"]
        val ac = evidence.goals.details["awayConceded"] ?: evidence.goals.details["awayMeanConceded"]
        val goalsSummary = if (evidence.goals.isAvailable && hs != null && as_ != null) "${hs ?: "—"}/${hc ?: "—"} · ${as_ ?: "—"}/${ac ?: "—"}" else null

        FlatSignalRow(
            title = stringResource(R.string.prediction_signal_goals_title),
            evidence = evidence.goals,
            quickSummary = goalsSummary,
            emptyMessage = stringResource(R.string.prediction_signal_no_goals)
        ) {
            val sub = stringResource(R.string.prediction_goals_avg_subtext)
            FlatComparisonRow(
                homeLabel = stringResource(R.string.prediction_home_label),
                awayLabel = stringResource(R.string.prediction_away_label),
                homeValue = "${hs ?: "—"} / ${hc ?: "—"}",
                awayValue = "${as_ ?: "—"} / ${ac ?: "—"}",
                homeSubtext = sub,
                awaySubtext = sub
            )
        }

        // 5. H2H
        val tot = evidence.h2h.details["totalMatches"] ?: "0"
        val hw = evidence.h2h.details["homeWins"] ?: "0"
        val d = evidence.h2h.details["draws"] ?: "0"
        val aw = evidence.h2h.details["awayWins"] ?: "0"
        val h2hSummary = if (tot != "0") "${hw}W · ${d}D · ${aw}L" else null

        FlatSignalRow(
            title = stringResource(R.string.prediction_signal_h2h_title),
            evidence = evidence.h2h,
            quickSummary = h2hSummary,
            emptyMessage = stringResource(R.string.prediction_signal_no_h2h)
        ) {
            if (tot == "0") {
                Text(
                    text = stringResource(R.string.prediction_signal_no_h2h),
                    style = MaterialTheme.typography.s12,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlatComparisonRow(
                    homeLabel = stringResource(R.string.prediction_home_label),
                    awayLabel = stringResource(R.string.prediction_away_label),
                    homeValue = "$hw W",
                    awayValue = "$aw W",
                    homeSubtext = stringResource(R.string.prediction_h2h_total, tot),
                    awaySubtext = stringResource(R.string.prediction_h2h_total, tot),
                    centerNote = "$d D"
                )
            }
        }

        // 6. Rest Advantage
        val homeRest = evidence.restAdvantage.details["homeRestDays"]
        val awayRest = evidence.restAdvantage.details["awayRestDays"]
        val deltaRest = evidence.restAdvantage.details["deltaRestDays"]

        val restAdvSummary = if (evidence.restAdvantage.isAvailable && deltaRest != null) {
            "${deltaRest}d"
        } else {
            stringResource(R.string.prediction_signal_neutral_fallback)
        }

        FlatSignalRow(
            title = stringResource(R.string.prediction_signal_rest_adv_title),
            evidence = evidence.restAdvantage,
            quickSummary = restAdvSummary
        ) {
            val detailText = if (evidence.restAdvantage.isAvailable && homeRest != null && awayRest != null) {
                stringResource(R.string.prediction_signal_rest_adv_detail, homeRest, awayRest)
            } else {
                stringResource(R.string.prediction_signal_rest_adv_no_history)
            }
            Text(
                text = detailText,
                style = MaterialTheme.typography.s12,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    }
}

/**
 * Flat signal row — no Card, no border.
 * Clickable row with a title on the left, quick summary + chevron on the right.
 * Expanding reveals inline detail content with a simple top divider.
 */
@Composable
private fun FlatSignalRow(
    title: String,
    evidence: SignalEvidence,
    modifier: Modifier = Modifier,
    quickSummary: String? = null,
    emptyMessage: String? = null,
    initiallyExpanded: Boolean = false,
    expandedContent: @Composable () -> Unit
) {
    val isAvailable = evidence.isAvailable && evidence.effectiveWeight > 0.0
    var isExpanded by remember { mutableStateOf(initiallyExpanded && isAvailable) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Clickable header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isAvailable) Modifier.clickable { isExpanded = !isExpanded } else Modifier)
                .padding(vertical = Dimen.PaddingS),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.s14.semiBold(),
                color = if (isAvailable) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(SpacingS))

            if (isAvailable) {
                if (!quickSummary.isNullOrBlank() && !isExpanded) {
                    Text(
                        text = quickSummary,
                        style = MaterialTheme.typography.s12.medium(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = SpacingXS)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(Dimen.SizeS2)
                )
            } else {
                Text(
                    text = emptyMessage ?: stringResource(R.string.prediction_signal_weight_zero),
                    style = MaterialTheme.typography.s12,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1
                )
            }
        }

        // Expanded detail — no wrapping card
        AnimatedVisibility(
            visible = isExpanded && isAvailable,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimen.PaddingS)
            ) {
                expandedContent()
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
    }
}

/**
 * Colored W / D / L badge sequence for team form.
 * Arranged in chronological order: oldest match on the left -> newest match on the right.
 * W: Green (#22C55E), D: Amber (#F59E0B), L: Red (#EF4444).
 */
@Composable
fun FormBadgeSequence(
    results: String,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start
) {
    val items = results.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val upper = item.uppercase()
            val color = when (upper) {
                "W", "T" -> Color(0xFF22C55E) // Xanh lá
                "D", "H" -> Color(0xFFF59E0B) // Cam vàng
                "L", "B" -> Color(0xFFEF4444) // Đỏ
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = upper,
                style = MaterialTheme.typography.s12.bold(),
                color = color
            )
            if (index < items.lastIndex) {
                Spacer(modifier = Modifier.width(Dimen.PaddingXSPlus))
            }
        }
    }
}

/**
 * Flat side-by-side comparison for Home vs Away metric values.
 * Accepts short labels (HOME/AWAY) and supports a horizontally centered difference/note column.
 */
@Composable
fun FlatComparisonRow(
    homeLabel: String? = null,
    awayLabel: String? = null,
    homeTeamName: String? = null,
    awayTeamName: String? = null,
    homeValue: String,
    awayValue: String,
    modifier: Modifier = Modifier,
    homeSubtext: String? = null,
    awaySubtext: String? = null,
    centerValue: String? = null,
    centerValueColor: Color? = null,
    centerNote: String? = null,
    homeSubContent: (@Composable () -> Unit)? = null,
    awaySubContent: (@Composable () -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") homeSubtextColor: Color? = null,
    @Suppress("UNUSED_PARAMETER") awaySubtextColor: Color? = null
) {
    val leftLabel = homeLabel ?: homeTeamName ?: ""
    val rightLabel = awayLabel ?: awayTeamName ?: ""

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // HOME Column (Left 50% flex)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = leftLabel,
                style = MaterialTheme.typography.s12.semiBold(),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(SpacingXXS))
            Text(
                text = homeValue,
                style = MaterialTheme.typography.s15.bold(),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (homeSubContent != null) {
                Spacer(modifier = Modifier.height(SpacingXS))
                homeSubContent()
            } else if (!homeSubtext.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(SpacingXS))
                Text(
                    text = homeSubtext,
                    style = MaterialTheme.typography.s12,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Center Column (Difference / Note - horizontally centered between Home & Away)
        if (!centerValue.isNullOrBlank() || !centerNote.isNullOrBlank()) {
            Box(
                modifier = Modifier.padding(horizontal = SpacingS),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!centerValue.isNullOrBlank()) {
                        Text(
                            text = centerValue,
                            style = MaterialTheme.typography.s14.bold(),
                            color = centerValueColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (!centerNote.isNullOrBlank()) {
                        Text(
                            text = centerNote,
                            style = MaterialTheme.typography.s12.medium(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // AWAY Column (Right 50% flex)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = rightLabel,
                style = MaterialTheme.typography.s12.semiBold(),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(SpacingXXS))
            Text(
                text = awayValue,
                style = MaterialTheme.typography.s15.bold(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End
            )
            if (awaySubContent != null) {
                Spacer(modifier = Modifier.height(SpacingXS))
                awaySubContent()
            } else if (!awaySubtext.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(SpacingXS))
                Text(
                    text = awaySubtext,
                    style = MaterialTheme.typography.s12,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Legacy EvidenceComparisonTable — kept for backwards compatibility.
 * New code should use FlatComparisonRow.
 */
@Composable
fun EvidenceComparisonTable(
    homeTeamName: String,
    awayTeamName: String,
    homeValue: String,
    awayValue: String,
    modifier: Modifier = Modifier,
    homeSubtext: String? = null,
    awaySubtext: String? = null,
    diffBadge: String? = null
) {
    FlatComparisonRow(
        homeTeamName = homeTeamName,
        awayTeamName = awayTeamName,
        homeValue = homeValue,
        awayValue = awayValue,
        modifier = modifier,
        homeSubtext = homeSubtext,
        awaySubtext = awaySubtext,
        centerNote = diffBadge
    )
}
