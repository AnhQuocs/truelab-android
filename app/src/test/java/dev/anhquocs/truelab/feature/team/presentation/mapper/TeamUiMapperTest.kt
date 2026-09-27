package dev.anhquocs.truelab.feature.team.presentation.mapper

import dev.anhquocs.truelab.core.algorithm.evaluation.FormScore
import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import org.junit.Assert.assertEquals
import org.junit.Test

class TeamUiMapperTest {

    private val sampleTeam = TeamDetail(
        id = 1,
        name = "Arsenal",
        logo = "https://example.com/arsenal.png",
        leagueName = "Premier League",
        eloRating = 1500.0
    )

    private val sampleRanking = SeasonRanking(
        teamId = 1,
        position = 1,
        won = 5,
        draw = 0,
        loss = 0,
        goalDiff = 10,
        recently = listOf("W", "W", "W", "W", "W")
    )

    @Test
    fun `toRecord with 5 Wins maps to 15 of 15 pts and 100 percent form score`() {
        val formScore5W = FormScore(
            score = 100.0,
            rawScore = 100.0,
            matchesCount = 5,
            wins = 5,
            draws = 0,
            losses = 0,
            totalPoints = 15.0,
            maxPoints = 15.0
        )

        val record = TeamUiMapper.toRecord(sampleTeam, sampleRanking, formScore = formScore5W)

        assertEquals(15, record.formPoints)
        assertEquals(15, record.maxFormPoints)
        assertEquals(100, record.formScore)
        assertEquals(listOf('W', 'W', 'W', 'W', 'W'), record.form)
    }

    @Test
    fun `toRecord with 5 Draws maps to 5 of 15 pts and 33 percent form score`() {
        val formScore5D = FormScore(
            score = 33.33,
            rawScore = 33.33,
            matchesCount = 5,
            wins = 0,
            draws = 5,
            losses = 0,
            totalPoints = 5.0,
            maxPoints = 15.0
        )

        val ranking5D = sampleRanking.copy(
            won = 0,
            draw = 5,
            loss = 0,
            goalDiff = 0,
            recently = listOf("D", "D", "D", "D", "D")
        )

        val record = TeamUiMapper.toRecord(sampleTeam, ranking5D, formScore = formScore5D)

        assertEquals(5, record.formPoints)
        assertEquals(15, record.maxFormPoints)
        assertEquals(33, record.formScore)
        assertEquals(listOf('D', 'D', 'D', 'D', 'D'), record.form)
    }

    @Test
    fun `toRecord with 5 Losses maps to 0 of 15 pts and 0 percent form score`() {
        val formScore5L = FormScore(
            score = 0.0,
            rawScore = 0.0,
            matchesCount = 5,
            wins = 0,
            draws = 0,
            losses = 5,
            totalPoints = 0.0,
            maxPoints = 15.0
        )

        val ranking5L = sampleRanking.copy(
            won = 0,
            draw = 0,
            loss = 5,
            goalDiff = -10,
            recently = listOf("L", "L", "L", "L", "L")
        )

        val record = TeamUiMapper.toRecord(sampleTeam, ranking5L, formScore = formScore5L)

        assertEquals(0, record.formPoints)
        assertEquals(15, record.maxFormPoints)
        assertEquals(0, record.formScore)
        assertEquals(listOf('L', 'L', 'L', 'L', 'L'), record.form)
    }

    @Test
    fun `toRecord with null formScore defaults safely`() {
        val record = TeamUiMapper.toRecord(sampleTeam, sampleRanking, formScore = null)

        assertEquals(0, record.formPoints)
        assertEquals(15, record.maxFormPoints)
        assertEquals(0, record.formScore)
    }

    @Test
    fun `toRecord with 2 matches maps maxFormPoints proportionally`() {
        val formScore2W = FormScore(
            score = 100.0,
            rawScore = 100.0,
            matchesCount = 2,
            wins = 2,
            draws = 0,
            losses = 0,
            totalPoints = 6.0,
            maxPoints = 6.0
        )

        val record = TeamUiMapper.toRecord(sampleTeam, sampleRanking, formScore = formScore2W)

        assertEquals(6, record.formPoints)
        assertEquals(6, record.maxFormPoints)
        assertEquals(100, record.formScore)
    }
}
