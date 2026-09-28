package com.example.spore.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trophic_species")
data class TrophicSpeciesEntity(
    @PrimaryKey val speciesId: String,
    val name: String,
    val scientificName: String,
    val trophicLevel: Int, // 0 to 4
    val diet: String,      // PRODUCER, HERBIVORE, OMNIVORE, CARNIVORE, APEX
    val description: String,
    val preyDescription: String,
    val predatorDescription: String,
    val discovered: Boolean = false,
    val timesEatenByPlayer: Int = 0,
    val timesKilledPlayer: Int = 0
)

@Entity(tableName = "game_stats")
data class GameStatsEntity(
    @PrimaryKey val id: Int = 1,
    val highScore: Int = 0,
    val maxBiomassReached: Float = 25f,
    val totalDnaCollected: Int = 0,
    val highestTrophicDefeated: Int = 0,
    val microbesEaten: Int = 0,
    val gamesPlayed: Int = 0
)
