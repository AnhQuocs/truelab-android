package dev.anhquocs.truelab.core.domain.odds.model

data class MatchOdds(
    val matchId: Long,
    val oddsList: List<OddsRecordItem>
)

data class OddsRecordItem(
    val companyId: Int,
    val companyName: String,
    val oddsType: String,
    val handicap: Double?,
    val over: Double?,
    val under: Double?,
    val homeWin: Double?,
    val draw: Double?,
    val awayWin: Double?,
    val changeTime: Long,
    val marketPhase: String?
) {
    fun calculateImpliedProbability(): ImpliedProbability? {
        val hw = homeWin ?: return null
        val d = draw ?: return null
        val aw = awayWin ?: return null

        if (hw <= 0 || d <= 0 || aw <= 0) return null

        val rawHome = 1.0 / hw
        val rawDraw = 1.0 / d
        val rawAway = 1.0 / aw
        val totalMargin = rawHome + rawDraw + rawAway

        return ImpliedProbability(
            homeProb = rawHome / totalMargin,
            drawProb = rawDraw / totalMargin,
            awayProb = rawAway / totalMargin
        )
    }
}

data class ImpliedProbability(
    val homeProb: Double,
    val drawProb: Double,
    val awayProb: Double
)

data class Bookmaker(
    val id: Int,
    val name: String,
    val shortName: String
)
