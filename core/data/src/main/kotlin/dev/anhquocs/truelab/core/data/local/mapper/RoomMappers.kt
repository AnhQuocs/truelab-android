package dev.anhquocs.truelab.core.data.local.mapper

import dev.anhquocs.truelab.core.data.league.local.entity.LeagueEntity
import dev.anhquocs.truelab.core.data.league.local.entity.SeasonEntity
import dev.anhquocs.truelab.core.data.match.local.entity.MatchEntity
import dev.anhquocs.truelab.core.data.match.local.entity.MatchWithTeams
import dev.anhquocs.truelab.core.data.match.remote.dto.MatchRecord
import dev.anhquocs.truelab.core.data.odds.local.entity.OddsEntity
import dev.anhquocs.truelab.core.data.odds.remote.dto.OddsRecord
import dev.anhquocs.truelab.core.data.prediction.local.entity.PredictionEntity
import dev.anhquocs.truelab.core.data.ranking.local.entity.SeasonRankingEntity
import dev.anhquocs.truelab.core.data.ranking.remote.dto.SeasonRankResponse
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity
import dev.anhquocs.truelab.core.domain.league.model.League
import dev.anhquocs.truelab.core.domain.league.model.Season
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionResult
import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object RoomMappers {

    // --- Domain to Entity ---
    fun PredictionResult.toEntity() = PredictionEntity(
        matchId = matchId,
        algorithmName = algorithmName,
        homeWinProb = homeWinProb,
        drawProb = drawProb,
        awayWinProb = awayWinProb,
        predictedOutcome = predictedOutcome,
        confidenceScore = confidenceScore
    )

    fun League.toEntity() = LeagueEntity(
        id = id,
        name = name,
        shortName = shortName,
        logo = logo,
        country = country,
        category = category
    )

    fun Season.toEntity() = SeasonEntity(
        id = id,
        leagueId = leagueId,
        name = name,
        year = year,
        isCurrent = isCurrent,
        startDate = startDate,
        endDate = endDate
    )

    // --- Entity to Domain ---
    fun PredictionEntity.toDomain() = PredictionResult(
        matchId = matchId,
        algorithmName = algorithmName,
        homeWinProb = homeWinProb,
        drawProb = drawProb,
        awayWinProb = awayWinProb,
        predictedOutcome = predictedOutcome,
        confidenceScore = confidenceScore
    )

    fun LeagueEntity.toDomain() = League(
        id = id,
        name = name,
        shortName = shortName,
        logo = logo,
        country = country,
        category = category
    )

    fun SeasonEntity.toDomain() = Season(
        id = id,
        leagueId = leagueId,
        name = name,
        year = year,
        isCurrent = isCurrent,
        startDate = startDate,
        endDate = endDate
    )

    fun TeamEntity.toDomain() = TeamDetail(
        id = id,
        name = name,
        logo = logo,
        leagueName = leagueName,
        eloRating = eloRating,
        formScore = formScore
    )

    fun SeasonRankingEntity.toDomain(json: Json) = SeasonRanking(
        teamId = teamId,
        position = position,
        won = won,
        draw = draw,
        loss = loss,
        goalDiff = goalDiff,
        recently = try { json.decodeFromString(recentlyStr) } catch (e: Exception) { emptyList() }
    )

    fun OddsEntity.toDomain() = OddsRecordItem(
        companyId = companyId,
        companyName = companyName,
        oddsType = oddsType,
        handicap = handicap,
        over = over,
        under = under,
        homeWin = homeWin,
        draw = draw,
        awayWin = awayWin,
        changeTime = changeTime,
        marketPhase = marketPhase
    )

    fun MatchWithTeams.toDomain() = Match(
        id = match.id,
        homeTeam = TeamSummary(id = homeTeam.id, name = homeTeam.name, logo = homeTeam.logo),
        awayTeam = TeamSummary(id = awayTeam.id, name = awayTeam.name, logo = awayTeam.logo),
        homeScore = match.homeScore,
        awayScore = match.awayScore,
        startTimeDate = match.startTimeDate,
        status = MatchStatus.fromCode(match.status),
        leagueId = match.leagueId,
        season = match.season
    )

    // --- DTO to Entity ---
    fun MatchRecord.toMatchEntity() = MatchEntity(
        id = id,
        homeTeamId = homeTeam.id,
        awayTeamId = awayTeam.id,
        homeScore = homeScore,
        awayScore = awayScore,
        startTimeDate = startTimeDate,
        status = status
    )

    fun MatchRecord.toHomeTeamEntity() = TeamEntity(
        id = homeTeam.id,
        name = homeTeam.name,
        logo = homeTeam.logo,
        leagueName = null // API doesn't provide this here
    )

    fun MatchRecord.toAwayTeamEntity() = TeamEntity(
        id = awayTeam.id,
        name = awayTeam.name,
        logo = awayTeam.logo,
        leagueName = null
    )

    fun OddsRecord.toEntity(matchId: Long) = OddsEntity(
        matchId = matchId,
        companyId = companyId,
        companyName = company?.name ?: "Unknown",
        oddsType = oddsType,
        handicap = handicap,
        over = over,
        under = under,
        homeWin = homeWin,
        draw = draw,
        awayWin = awayWin,
        changeTime = changeTime,
        marketPhase = marketPhase
    )

    fun SeasonRankResponse.toEntity(matchId: Long, json: Json) = SeasonRankingEntity(
        matchId = matchId,
        teamId = teamId,
        position = position,
        won = won,
        draw = draw,
        loss = loss,
        goalDiff = goalDiff,
        recentlyStr = json.encodeToString(recently ?: emptyList<String>())
    )
}
