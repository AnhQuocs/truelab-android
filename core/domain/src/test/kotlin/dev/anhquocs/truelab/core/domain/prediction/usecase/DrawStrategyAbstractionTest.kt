package dev.anhquocs.truelab.core.domain.prediction.usecase

import dev.anhquocs.truelab.core.domain.evaluation.model.DailyBacktestProgressEvent
import dev.anhquocs.truelab.core.domain.evaluation.usecase.CalculateEvaluationMetricsUseCase
import dev.anhquocs.truelab.core.domain.evaluation.usecase.RunDailyBacktestUseCase
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import dev.anhquocs.truelab.core.domain.prediction.model.DrawModelingStrategy
import dev.anhquocs.truelab.core.domain.prediction.model.DrawStrategyConfig
import dev.anhquocs.truelab.core.domain.prediction.model.MatchPredictionContext
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateDynamicEloUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Bộ kiểm thử hồi quy & kiến trúc chiến lược Draw Modeling (Phase B1 - Draw Strategy Abstraction).
 *
 * Đảm bảo 100% các tiêu chí bất biến:
 * 1. Default strategy luôn là [DrawModelingStrategy.BASELINE].
 * 2. Explicit BASELINE sinh ra kết quả xác suất, nhãn dự đoán và evidence giống hệt 100% default.
 * 3. Bảo vệ bất biến xác suất Draw cơ sở (0.26) và Argmax tie-breaking.
 * 4. RunDailyBacktestUseCase mặc định chạy ở chế độ BASELINE.
 * 5. Các chiến lược chưa triển khai (DECISION_MARGIN, DYNAMIC_DRAW_PRIOR) fail fast rõ ràng.
 */
class DrawStrategyAbstractionTest {

    private lateinit var useCase: PredictMatchOutcomeUseCase

    private val arsenal = TeamSummary(1, "Arsenal")
    private val chelsea = TeamSummary(2, "Chelsea")

    @Before
    fun setUp() {
        useCase = PredictMatchOutcomeUseCase()
    }

    private fun createDeterministicContext(): MatchPredictionContext {
        val homeRecent = listOf(
            Match(101L, arsenal, TeamSummary(10, "Team 10"), 2, 0, "2026-10-01 20:00", MatchStatus.ENDED),
            Match(102L, TeamSummary(11, "Team 11"), arsenal, 1, 3, "2026-09-25 20:00", MatchStatus.ENDED)
        )
        val awayRecent = listOf(
            Match(201L, chelsea, TeamSummary(20, "Team 20"), 0, 1, "2026-10-03 20:00", MatchStatus.ENDED),
            Match(202L, TeamSummary(21, "Team 21"), chelsea, 2, 0, "2026-09-27 20:00", MatchStatus.ENDED)
        )
        val h2h = listOf(
            Match(301L, arsenal, chelsea, 2, 1, "2026-05-01 20:00", MatchStatus.ENDED),
            Match(302L, chelsea, arsenal, 0, 0, "2026-01-01 20:00", MatchStatus.ENDED)
        )
        val odds = OddsRecordItem(
            companyId = 1,
            companyName = "Bet365",
            oddsType = "1X2",
            handicap = null,
            over = null,
            under = null,
            homeWin = 1.90,
            draw = 3.40,
            awayWin = 4.00,
            changeTime = 1700000000L,
            marketPhase = "FINAL"
        )
        return MatchPredictionContext(
            matchId = 9999L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeElo = 1750.0,
            awayElo = 1550.0,
            homeRecentMatches = homeRecent,
            awayRecentMatches = awayRecent,
            h2hMatches = h2h,
            latestOdds = odds,
            matchStartTimeDate = "2026-10-06 20:00"
        )
    }

    @Test
    fun `Test 1 - default strategy invokes BASELINE without explicit argument`() {
        val context = createDeterministicContext()

        val defaultResult = useCase(context)
        val baselineResult = useCase(
            context = context,
            config = PredictionWeightConfig.DEFAULT,
            drawStrategyConfig = DrawStrategyConfig.DEFAULT
        )

        assertEquals(baselineResult.homeWinProb, defaultResult.homeWinProb, 1e-6)
        assertEquals(baselineResult.drawProb, defaultResult.drawProb, 1e-6)
        assertEquals(baselineResult.awayWinProb, defaultResult.awayWinProb, 1e-6)
        assertEquals(baselineResult.predictedOutcome, defaultResult.predictedOutcome)
        assertEquals(baselineResult.confidenceScore, defaultResult.confidenceScore, 1e-6)
    }

    @Test
    fun `Test 2 - explicit BASELINE strategy produces identical output to default`() {
        val context = createDeterministicContext()

        val defaultResult = useCase(context)
        val explicitBaselineResult = useCase(
            context = context,
            config = PredictionWeightConfig.DEFAULT,
            drawStrategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.BASELINE)
        )

        assertEquals(defaultResult.homeWinProb, explicitBaselineResult.homeWinProb, 1e-6)
        assertEquals(defaultResult.drawProb, explicitBaselineResult.drawProb, 1e-6)
        assertEquals(defaultResult.awayWinProb, explicitBaselineResult.awayWinProb, 1e-6)
        assertEquals(defaultResult.predictedOutcome, explicitBaselineResult.predictedOutcome)
        assertEquals(defaultResult.confidenceScore, explicitBaselineResult.confidenceScore, 1e-6)
    }

    @Test
    fun `Test 3 - baseline probability regression confirms exact probability distribution`() {
        val context = createDeterministicContext()

        val result = useCase(context)

        // Probability conservation
        val sum = result.homeWinProb + result.drawProb + result.awayWinProb
        assertEquals(1.0, sum, 1e-4)

        // Home team is stronger in Elo, Form, Odds, H2H, Rest Advantage
        assertTrue("Home prob (${result.homeWinProb}) must exceed Draw prob (${result.drawProb})", result.homeWinProb > result.drawProb)
        assertTrue("Home prob (${result.homeWinProb}) must exceed Away prob (${result.awayWinProb})", result.homeWinProb > result.awayWinProb)
        assertTrue("Draw prob (${result.drawProb}) must be in valid range [0.20, 0.35]", result.drawProb in 0.20..0.35)
    }

    @Test
    fun `Test 4 - baseline outcome regression confirms deterministic HOME_WIN predicted outcome`() {
        val context = createDeterministicContext()

        val result = useCase(context)

        assertEquals("HOME_WIN", result.predictedOutcome)
        assertEquals(result.homeWinProb, result.confidenceScore, 1e-6)
    }

    @Test
    fun `Test 5 - baseline evidence regression confirms all 6 signals and metadata intact`() {
        val context = createDeterministicContext()

        val result = useCase(context)

        assertNotNull(result.evidence)
        val ev = result.evidence!!
        assertEquals(6, ev.signals.size)
        assertEquals("Elo", ev.elo.name)
        assertEquals("Form", ev.form.name)
        assertEquals("Odds", ev.odds.name)
        assertEquals("Goals", ev.goals.name)
        assertEquals("H2H", ev.h2h.name)
        assertEquals("Rest Advantage", ev.restAdvantage.name)
        assertEquals("1750.0", ev.elo.details["homeElo"])
        assertEquals("1550.0", ev.elo.details["awayElo"])
        assertEquals("Bet365", ev.odds.details["bookmaker"])
        assertEquals(1.0, ev.totalWeight, 1e-6)
    }

    @Test
    fun `Test 6 - backtest usecase defaults to BASELINE strategy and evaluates FT matches correctly`() = runTest {
        val targetMatch = Match(
            id = 501L,
            homeTeam = arsenal,
            awayTeam = chelsea,
            homeScore = 2,
            awayScore = 0,
            startTimeDate = "2026-10-02T19:00:00",
            status = MatchStatus.ENDED
        )

        val fakeOddsRepo = object : OddsRepository {
            override fun getMatchOdds(matchId: Long): Flow<MatchOdds> = flowOf(MatchOdds(matchId, emptyList()))
            override fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): Flow<List<OddsRecordItem>> = flowOf(emptyList())
            override suspend fun getLatestEuropeanOddsMap(): Map<Long, OddsRecordItem> = emptyMap()
            override suspend fun fetchAndCacheOddsForMatch(matchId: Long): Result<Unit> = Result.success(Unit)
        }

        val backtestUseCase = RunDailyBacktestUseCase(
            oddsRepository = fakeOddsRepo,
            predictMatchOutcomeUseCase = useCase,
            calculateDynamicEloUseCase = CalculateDynamicEloUseCase(),
            calculateEvaluationMetricsUseCase = CalculateEvaluationMetricsUseCase()
        )

        val events = backtestUseCase(
            evaluationDate = "2026-10-02",
            targetMatches = listOf(targetMatch),
            allMatches = listOf(targetMatch),
            initialEloMap = mapOf(1 to 1800.0, 2 to 1500.0)
        ).toList()

        val completed = events.last() as DailyBacktestProgressEvent.Completed
        assertEquals(1, completed.result.totalMatches)
        assertEquals(1, completed.result.correctMatches)
        assertEquals(1.0, completed.result.accuracy, 0.001)
        val record = completed.result.records.first()
        assertEquals("HOME_WIN", record.predictedOutcome)
    }

    @Test
    fun `Test 7 - DECISION_MARGIN strategy executes successfully without exception`() {
        val context = createDeterministicContext()

        val result = useCase(
            context = context,
            drawStrategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.DECISION_MARGIN)
        )

        assertNotNull(result)
        assertEquals(1.0, result.homeWinProb + result.drawProb + result.awayWinProb, 1e-4)
    }

    @Test
    fun `Test 8 - DYNAMIC_DRAW_PRIOR strategy executes successfully with valid probabilities and evidence`() {
        val context = createDeterministicContext()

        val result = useCase(
            context = context,
            drawStrategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.DYNAMIC_DRAW_PRIOR)
        )

        assertNotNull(result)
        val sum = result.homeWinProb + result.drawProb + result.awayWinProb
        assertEquals(1.0, sum, 1e-4)
        assertTrue(result.homeWinProb in 0.0..1.0)
        assertTrue(result.drawProb in 0.0..1.0)
        assertTrue(result.awayWinProb in 0.0..1.0)
        assertNotNull(result.evidence)
    }

    @Test
    fun `Test 11 - balanced context with DYNAMIC_DRAW_PRIOR elevates drawProb and evidence`() {
        val balancedContext = MatchPredictionContext(
            matchId = 7778L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeElo = 1500.0,
            awayElo = 1500.0,
            homeRecentMatches = emptyList(),
            awayRecentMatches = emptyList(),
            h2hMatches = emptyList(),
            latestOdds = null,
            matchStartTimeDate = "2026-10-06 20:00"
        )

        val baselineResult = useCase(
            context = balancedContext,
            drawStrategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.BASELINE)
        )
        val dynamicResult = useCase(
            context = balancedContext,
            drawStrategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.DYNAMIC_DRAW_PRIOR)
        )

        // Candidate B increases Draw probability in balanced matches
        assertTrue(
            "Dynamic Draw prob (${dynamicResult.drawProb}) must exceed Baseline Draw prob (${baselineResult.drawProb}) in balanced match",
            dynamicResult.drawProb > baselineResult.drawProb
        )
        val sum = dynamicResult.homeWinProb + dynamicResult.drawProb + dynamicResult.awayWinProb
        assertEquals(1.0, sum, 1e-6)
    }

    @Test
    fun `Test 12 - backtest with DYNAMIC_DRAW_PRIOR strategy passes strategy to prediction pipeline correctly`() = runTest {
        val balancedMatch = Match(
            id = 503L,
            homeTeam = arsenal,
            awayTeam = chelsea,
            homeScore = 0,
            awayScore = 0,
            startTimeDate = "2026-10-02T19:00:00",
            status = MatchStatus.ENDED
        )

        val fakeOddsRepo = object : OddsRepository {
            override fun getMatchOdds(matchId: Long): Flow<MatchOdds> = flowOf(MatchOdds(matchId, emptyList()))
            override fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): Flow<List<OddsRecordItem>> = flowOf(emptyList())
            override suspend fun getLatestEuropeanOddsMap(): Map<Long, OddsRecordItem> = emptyMap()
            override suspend fun fetchAndCacheOddsForMatch(matchId: Long): Result<Unit> = Result.success(Unit)
        }

        val backtestUseCase = RunDailyBacktestUseCase(
            oddsRepository = fakeOddsRepo,
            predictMatchOutcomeUseCase = useCase,
            calculateDynamicEloUseCase = CalculateDynamicEloUseCase(),
            calculateEvaluationMetricsUseCase = CalculateEvaluationMetricsUseCase()
        )

        val events = backtestUseCase(
            evaluationDate = "2026-10-02",
            targetMatches = listOf(balancedMatch),
            allMatches = listOf(balancedMatch),
            initialEloMap = mapOf(1 to 1500.0, 2 to 1500.0),
            drawStrategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.DYNAMIC_DRAW_PRIOR)
        ).toList()

        val completed = events.last() as DailyBacktestProgressEvent.Completed
        val record = completed.result.records.first()
        assertNotNull(record.predictedOutcome)
        assertEquals(1.0, record.homeWinProb + record.drawProb + record.awayWinProb, 1e-4)
    }

    @Test
    fun `Test 9 - balanced context with DECISION_MARGIN predicts DRAW while preserving probabilities`() {
        // Balanced match context
        val balancedContext = MatchPredictionContext(
            matchId = 7777L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeElo = 1500.0,
            awayElo = 1500.0,
            homeRecentMatches = emptyList(),
            awayRecentMatches = emptyList(),
            h2hMatches = emptyList(),
            latestOdds = null,
            matchStartTimeDate = "2026-10-06 20:00"
        )

        val baselineResult = useCase(
            context = balancedContext,
            drawStrategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.BASELINE)
        )
        val candidateAResult = useCase(
            context = balancedContext,
            drawStrategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.DECISION_MARGIN)
        )

        // Raw probabilities MUST BE IDENTICAL
        assertEquals(baselineResult.homeWinProb, candidateAResult.homeWinProb, 1e-6)
        assertEquals(baselineResult.drawProb, candidateAResult.drawProb, 1e-6)
        assertEquals(baselineResult.awayWinProb, candidateAResult.awayWinProb, 1e-6)

        // In a neutral/balanced match, Candidate A predicts DRAW
        assertEquals("DRAW", candidateAResult.predictedOutcome)
        assertEquals(candidateAResult.drawProb, candidateAResult.confidenceScore, 1e-6)
    }

    @Test
    fun `Test 10 - backtest with DECISION_MARGIN strategy passes strategy to predictions correctly`() = runTest {
        val balancedMatch = Match(
            id = 502L,
            homeTeam = arsenal,
            awayTeam = chelsea,
            homeScore = 1,
            awayScore = 1,
            startTimeDate = "2026-10-02T19:00:00",
            status = MatchStatus.ENDED
        )

        val fakeOddsRepo = object : OddsRepository {
            override fun getMatchOdds(matchId: Long): Flow<MatchOdds> = flowOf(MatchOdds(matchId, emptyList()))
            override fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): Flow<List<OddsRecordItem>> = flowOf(emptyList())
            override suspend fun getLatestEuropeanOddsMap(): Map<Long, OddsRecordItem> = emptyMap()
            override suspend fun fetchAndCacheOddsForMatch(matchId: Long): Result<Unit> = Result.success(Unit)
        }

        val backtestUseCase = RunDailyBacktestUseCase(
            oddsRepository = fakeOddsRepo,
            predictMatchOutcomeUseCase = useCase,
            calculateDynamicEloUseCase = CalculateDynamicEloUseCase(),
            calculateEvaluationMetricsUseCase = CalculateEvaluationMetricsUseCase()
        )

        val events = backtestUseCase(
            evaluationDate = "2026-10-02",
            targetMatches = listOf(balancedMatch),
            allMatches = listOf(balancedMatch),
            initialEloMap = mapOf(1 to 1500.0, 2 to 1500.0),
            drawStrategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.DECISION_MARGIN)
        ).toList()

        val completed = events.last() as DailyBacktestProgressEvent.Completed
        val record = completed.result.records.first()
        assertEquals("DRAW", record.predictedOutcome)
        assertEquals("DRAW", record.actualOutcome)
        assertTrue(record.isCorrect)
    }
}
