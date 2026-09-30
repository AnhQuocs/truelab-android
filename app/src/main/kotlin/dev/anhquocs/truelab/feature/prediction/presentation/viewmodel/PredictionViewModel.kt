package dev.anhquocs.truelab.feature.prediction.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import dev.anhquocs.truelab.core.domain.prediction.model.MatchPredictionContext
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictMatchOutcomeUseCase
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.prediction.presentation.model.PredictionUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PredictionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val matchRepository: MatchRepository,
    private val teamRepository: TeamRepository,
    private val oddsRepository: OddsRepository,
    private val predictMatchOutcomeUseCase: PredictMatchOutcomeUseCase
) : ViewModel() {

    private val navMatchId: Long? = savedStateHandle.get<String>("matchId")?.toLongOrNull()
        ?: savedStateHandle.get<Long>("matchId")

    private val _selectedMatchId = MutableStateFlow<Long?>(navMatchId)
    private val _searchQuery = MutableStateFlow("")

    private val matchesFlow = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                matchRepository.getPredictableMatches(50)
            } else {
                matchRepository.searchMatches(query.trim(), 30)
            }
        }

    val availableMatches: StateFlow<List<Match>> = matchesFlow
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<PredictionUiState> = combine(
        matchesFlow,
        _selectedMatchId
    ) { matches, userMatchId ->
        Pair(matches, userMatchId)
    }.flatMapLatest { (matches, userMatchId) ->
        if (matches.isEmpty()) {
            flowOf(PredictionUiState.Empty(UiText.StringResource(R.string.prediction_empty_no_matches)))
        } else {
            val selectedMatch = userMatchId?.let { id -> matches.find { it.id == id } }
                ?: matches.first()

            val homeTeamId = selectedMatch.homeTeam.id
            val awayTeamId = selectedMatch.awayTeam.id

            combine(
                teamRepository.getTeamDetail(homeTeamId),
                teamRepository.getTeamDetail(awayTeamId),
                matchRepository.getRecentMatchesForTeam(homeTeamId, 5),
                matchRepository.getRecentMatchesForTeam(awayTeamId, 5),
                matchRepository.getH2HMatches(homeTeamId, awayTeamId),
                oddsRepository.getMatchOdds(selectedMatch.id)
            ) { args: Array<Any?> ->
                val homeTeamDetail = args[0] as? dev.anhquocs.truelab.core.domain.team.model.TeamDetail
                val awayTeamDetail = args[1] as? dev.anhquocs.truelab.core.domain.team.model.TeamDetail
                @Suppress("UNCHECKED_CAST")
                val homeRecent = (args[2] as? List<Match>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val awayRecent = (args[3] as? List<Match>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val h2hMatches = (args[4] as? List<Match>) ?: emptyList()
                val matchOdds = args[5] as dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
                val context = MatchPredictionContext(
                    matchId = selectedMatch.id,
                    homeTeamId = homeTeamId,
                    awayTeamId = awayTeamId,
                    homeElo = homeTeamDetail?.eloRating,
                    awayElo = awayTeamDetail?.eloRating,
                    homeRecentMatches = homeRecent,
                    awayRecentMatches = awayRecent,
                    h2hMatches = h2hMatches,
                    latestOdds = matchOdds.oddsList.firstOrNull()
                )

                val result = predictMatchOutcomeUseCase(context)

                val homeWinPct = (result.homeWinProb * 100).roundToInt()
                val drawPct = (result.drawProb * 100).roundToInt()
                val awayWinPct = (result.awayWinProb * 100).roundToInt()
                val confPct = (result.confidenceScore * 100).roundToInt()

                PredictionUiState.Success(
                    selectedMatch = selectedMatch,
                    availableMatches = matches,
                    predictionResult = result,
                    homeElo = homeTeamDetail?.eloRating,
                    awayElo = awayTeamDetail?.eloRating,
                    homeWinPercent = homeWinPct,
                    drawPercent = drawPct,
                    awayWinPercent = awayWinPct,
                    confidencePercent = confPct
                )
            }
        }
    }.catch { e ->
        emit(
            PredictionUiState.Error(
                if (e.message.isNullOrBlank()) {
                    UiText.StringResource(R.string.prediction_error_default)
                } else {
                    UiText.DynamicString(e.message ?: "")
                }
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PredictionUiState.Loading
    )

    fun onSelectMatch(matchId: Long) {
        _selectedMatchId.value = matchId
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
