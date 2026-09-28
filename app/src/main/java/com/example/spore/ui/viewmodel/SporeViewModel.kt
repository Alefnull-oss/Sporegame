package com.example.spore.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spore.data.db.SporeDatabase
import com.example.spore.data.db.SporeRepository
import com.example.spore.data.model.CellEvolutionEntity
import com.example.spore.data.model.GameStatsEntity
import com.example.spore.data.model.TrophicSpeciesEntity
import com.example.spore.game.engine.CellEvolutionConfig
import com.example.spore.game.engine.GameSimulation
import com.example.spore.game.engine.Vector2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    MAIN_MENU,
    GAME,
    CELL_EDITOR,
    TROPHIC_WEB
}

class SporeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SporeRepository = SporeRepository(
        SporeDatabase.getInstance(application).sporeDao()
    )

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    val cellEvolution: StateFlow<CellEvolutionEntity?> = repository.cellEvolution
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSpecies: StateFlow<List<TrophicSpeciesEntity>> = repository.allSpecies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gameStats: StateFlow<GameStatsEntity?> = repository.gameStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _editorDraft = MutableStateFlow(CellEvolutionEntity())
    val editorDraft: StateFlow<CellEvolutionEntity> = _editorDraft.asStateFlow()

    var gameSimulation: GameSimulation? = null
        private set

    init {
        viewModelScope.launch {
            repository.ensureDefaultSpecies()
            val initial = repository.getCellEvolutionSync()
            _editorDraft.value = initial
            initSimulation(initial)
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (screen == AppScreen.CELL_EDITOR) {
            // Sync editor draft with latest cell configuration
            cellEvolution.value?.let { _editorDraft.value = it }
        } else if (screen == AppScreen.GAME) {
            // Apply current cell config to active simulation
            cellEvolution.value?.let { gameSimulation?.updateEvolutionConfig(it) }
        }
        _currentScreen.value = screen
    }

    private fun initSimulation(evolution: CellEvolutionEntity) {
        gameSimulation = GameSimulation(
            initialEvolution = evolution,
            onDnaCollected = { amount ->
                viewModelScope.launch {
                    val current = repository.getCellEvolutionSync()
                    repository.saveCellEvolution(current.copy(dnaPoints = current.dnaPoints + amount))
                }
            },
            onSpeciesDiscovered = { speciesId ->
                viewModelScope.launch {
                    repository.markSpeciesDiscovered(speciesId)
                }
            },
            onSpeciesEaten = { speciesId ->
                viewModelScope.launch {
                    repository.recordPreyEaten(speciesId)
                }
            },
            onPlayerKilled = { speciesId ->
                viewModelScope.launch {
                    repository.recordKilledBy(speciesId)
                    val sim = gameSimulation ?: return@launch
                    repository.updateStatsOnGameEnd(
                        score = (sim.player.biomass * 10).toInt(),
                        maxBiomass = sim.player.biomass,
                        dnaEarned = 0,
                        microbesDefeated = sim.microbesEatenCount,
                        highestTierDefeated = sim.highestTierDefeated
                    )
                }
            },
            onHapticImpact = {
                triggerHaptic()
            }
        )
    }

    fun triggerHaptic(durationMs: Long = 25) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    // --- Cell Editor DNA Mutations ---

    fun setDraftSpeciesName(name: String) {
        _editorDraft.value = _editorDraft.value.copy(speciesName = name)
    }

    fun setDraftColor(colorHex: Long) {
        _editorDraft.value = _editorDraft.value.copy(primaryColorHex = colorHex)
    }

    fun setDraftMouth(mouthType: String) {
        val current = _editorDraft.value
        if (current.mouthType == mouthType) return
        val cost = CellEvolutionConfig.COST_MOUTH_CHANGE
        if (current.dnaPoints >= cost) {
            _editorDraft.value = current.copy(
                mouthType = mouthType,
                dnaPoints = current.dnaPoints - cost
            )
            triggerHaptic(40)
        }
    }

    fun upgradeFlagella() {
        val current = _editorDraft.value
        if (current.flagellaCount >= 4) return
        val cost = CellEvolutionConfig.COST_FLAGELLUM
        if (current.dnaPoints >= cost) {
            _editorDraft.value = current.copy(
                flagellaCount = current.flagellaCount + 1,
                dnaPoints = current.dnaPoints - cost
            )
            triggerHaptic(40)
        }
    }

    fun downgradeFlagella() {
        val current = _editorDraft.value
        if (current.flagellaCount <= 0) return
        _editorDraft.value = current.copy(
            flagellaCount = current.flagellaCount - 1,
            dnaPoints = current.dnaPoints + CellEvolutionConfig.COST_FLAGELLUM
        )
        triggerHaptic(30)
    }

    fun upgradeCilia() {
        val current = _editorDraft.value
        if (current.ciliaCount >= 4) return
        val cost = CellEvolutionConfig.COST_CILIA
        if (current.dnaPoints >= cost) {
            _editorDraft.value = current.copy(
                ciliaCount = current.ciliaCount + 1,
                dnaPoints = current.dnaPoints - cost
            )
            triggerHaptic(40)
        }
    }

    fun downgradeCilia() {
        val current = _editorDraft.value
        if (current.ciliaCount <= 0) return
        _editorDraft.value = current.copy(
            ciliaCount = current.ciliaCount - 1,
            dnaPoints = current.dnaPoints + CellEvolutionConfig.COST_CILIA
        )
        triggerHaptic(30)
    }

    fun upgradeJet() {
        val current = _editorDraft.value
        if (current.jetCount >= 2) return
        val cost = CellEvolutionConfig.COST_JET
        if (current.dnaPoints >= cost) {
            _editorDraft.value = current.copy(
                jetCount = current.jetCount + 1,
                dnaPoints = current.dnaPoints - cost
            )
            triggerHaptic(45)
        }
    }

    fun downgradeJet() {
        val current = _editorDraft.value
        if (current.jetCount <= 0) return
        _editorDraft.value = current.copy(
            jetCount = current.jetCount - 1,
            dnaPoints = current.dnaPoints + CellEvolutionConfig.COST_JET
        )
        triggerHaptic(30)
    }

    fun upgradeSpikes() {
        val current = _editorDraft.value
        if (current.spikesCount >= 4) return
        val cost = CellEvolutionConfig.COST_SPIKE
        if (current.dnaPoints >= cost) {
            _editorDraft.value = current.copy(
                spikesCount = current.spikesCount + 1,
                dnaPoints = current.dnaPoints - cost
            )
            triggerHaptic(45)
        }
    }

    fun downgradeSpikes() {
        val current = _editorDraft.value
        if (current.spikesCount <= 0) return
        _editorDraft.value = current.copy(
            spikesCount = current.spikesCount - 1,
            dnaPoints = current.dnaPoints + CellEvolutionConfig.COST_SPIKE
        )
        triggerHaptic(30)
    }

    fun togglePoison() {
        val current = _editorDraft.value
        if (current.poisonGland) {
            _editorDraft.value = current.copy(
                poisonGland = false,
                dnaPoints = current.dnaPoints + CellEvolutionConfig.COST_POISON
            )
        } else {
            val cost = CellEvolutionConfig.COST_POISON
            if (current.dnaPoints >= cost) {
                _editorDraft.value = current.copy(
                    poisonGland = true,
                    dnaPoints = current.dnaPoints - cost
                )
            }
        }
        triggerHaptic(40)
    }

    fun toggleElectric() {
        val current = _editorDraft.value
        if (current.electricOrgan) {
            _editorDraft.value = current.copy(
                electricOrgan = false,
                dnaPoints = current.dnaPoints + CellEvolutionConfig.COST_ELECTRIC
            )
        } else {
            val cost = CellEvolutionConfig.COST_ELECTRIC
            if (current.dnaPoints >= cost) {
                _editorDraft.value = current.copy(
                    electricOrgan = true,
                    dnaPoints = current.dnaPoints - cost
                )
            }
        }
        triggerHaptic(40)
    }

    fun upgradeArmor() {
        val current = _editorDraft.value
        if (current.armorPlates >= 3) return
        val cost = CellEvolutionConfig.COST_ARMOR
        if (current.dnaPoints >= cost) {
            _editorDraft.value = current.copy(
                armorPlates = current.armorPlates + 1,
                dnaPoints = current.dnaPoints - cost
            )
            triggerHaptic(40)
        }
    }

    fun downgradeArmor() {
        val current = _editorDraft.value
        if (current.armorPlates <= 0) return
        _editorDraft.value = current.copy(
            armorPlates = current.armorPlates - 1,
            dnaPoints = current.dnaPoints + CellEvolutionConfig.COST_ARMOR
        )
        triggerHaptic(30)
    }

    fun toggleEye() {
        val current = _editorDraft.value
        val nextEye = when (current.eyeType) {
            "NONE" -> "BASIC"
            "BASIC" -> "COMPOUND"
            else -> "NONE"
        }
        val cost = CellEvolutionConfig.COST_EYE_UPGRADE
        if (nextEye == "COMPOUND" && current.dnaPoints >= cost) {
            _editorDraft.value = current.copy(eyeType = "COMPOUND", dnaPoints = current.dnaPoints - cost)
        } else if (nextEye == "BASIC" && current.dnaPoints >= 10) {
            _editorDraft.value = current.copy(eyeType = "BASIC", dnaPoints = current.dnaPoints - 10)
        } else if (nextEye == "NONE") {
            _editorDraft.value = current.copy(eyeType = "NONE", dnaPoints = current.dnaPoints + 20)
        }
        triggerHaptic(35)
    }

    fun saveAndApplyMutations() {
        viewModelScope.launch {
            val updated = _editorDraft.value.copy(generation = _editorDraft.value.generation + 1)
            repository.saveCellEvolution(updated)
            gameSimulation?.updateEvolutionConfig(updated)
            navigateTo(AppScreen.GAME)
        }
    }

    fun restartGame() {
        gameSimulation?.restart()
    }
}
