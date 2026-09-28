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
import com.example.spore.data.model.PlanetDefinition
import com.example.spore.data.model.PlanetSaveEntity
import com.example.spore.data.model.TrophicSpeciesEntity
import com.example.spore.game.engine.CellEvolutionConfig
import com.example.spore.game.engine.GameSimulation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    GALAXY_MAP,
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

    val allPlanetSaves: StateFlow<List<PlanetSaveEntity>> = repository.allPlanetSaves
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cellEvolution: StateFlow<CellEvolutionEntity?> = repository.cellEvolution
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSpecies: StateFlow<List<TrophicSpeciesEntity>> = repository.allSpecies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gameStats: StateFlow<GameStatsEntity?> = repository.gameStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentScreen = MutableStateFlow(AppScreen.GALAXY_MAP)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedPlanet = MutableStateFlow(PlanetDefinition.PLANETS[0])
    val selectedPlanet: StateFlow<PlanetDefinition> = _selectedPlanet.asStateFlow()

    private val _editorDraft = MutableStateFlow(CellEvolutionEntity())
    val editorDraft: StateFlow<CellEvolutionEntity> = _editorDraft.asStateFlow()

    var gameSimulation: GameSimulation? = null
        private set

    init {
        viewModelScope.launch {
            repository.ensureDefaults()
            val initial = repository.getCellEvolutionSync()
            _editorDraft.value = initial
            initSimulation(initial, _selectedPlanet.value)
        }
    }

    fun selectPlanet(planet: PlanetDefinition) {
        _selectedPlanet.value = planet
        viewModelScope.launch {
            val save = repository.getPlanetSaveSync(planet.id)
            if (save != null) {
                val evolution = save.toCellEvolutionEntity()
                _editorDraft.value = evolution
                repository.saveCellEvolution(evolution)
                gameSimulation?.setPlanet(planet)
                gameSimulation?.updateEvolutionConfig(evolution)
            }
        }
    }

    fun launchPlanet(planet: PlanetDefinition) {
        selectPlanet(planet)
        viewModelScope.launch {
            val save = repository.getPlanetSaveSync(planet.id)
            if (save != null && !save.hasPlayed) {
                repository.savePlanetSave(save.copy(hasPlayed = true))
            }
            navigateTo(AppScreen.GAME)
        }
    }

    fun resetPlanet(planetId: String) {
        viewModelScope.launch {
            repository.resetPlanet(planetId)
            val def = PlanetDefinition.getById(planetId)
            if (_selectedPlanet.value.id == planetId) {
                selectPlanet(def)
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (screen == AppScreen.CELL_EDITOR) {
            cellEvolution.value?.let { _editorDraft.value = it }
        } else if (screen == AppScreen.GAME) {
            cellEvolution.value?.let { gameSimulation?.updateEvolutionConfig(it) }
        }
        _currentScreen.value = screen
    }

    private fun initSimulation(evolution: CellEvolutionEntity, planet: PlanetDefinition) {
        gameSimulation = GameSimulation(
            initialEvolution = evolution,
            planetDefinition = planet,
            onDnaCollected = { amount ->
                viewModelScope.launch {
                    val pId = _selectedPlanet.value.id
                    val pSave = repository.getPlanetSaveSync(pId)
                    if (pSave != null) {
                        repository.savePlanetSave(pSave.copy(dnaPoints = pSave.dnaPoints + amount))
                    }
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
                        planetId = _selectedPlanet.value.id,
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
            // Also update active planet save file
            val pId = _selectedPlanet.value.id
            val pSave = repository.getPlanetSaveSync(pId)
            if (pSave != null) {
                repository.savePlanetSave(
                    pSave.copy(
                        speciesName = updated.speciesName,
                        generation = updated.generation,
                        dnaPoints = updated.dnaPoints,
                        biomass = updated.biomass,
                        mouthType = updated.mouthType,
                        flagellaCount = updated.flagellaCount,
                        ciliaCount = updated.ciliaCount,
                        jetCount = updated.jetCount,
                        spikesCount = updated.spikesCount,
                        poisonGland = updated.poisonGland,
                        electricOrgan = updated.electricOrgan,
                        armorPlates = updated.armorPlates,
                        eyeType = updated.eyeType,
                        primaryColorHex = updated.primaryColorHex,
                        hasPlayed = true
                    )
                )
            }
            gameSimulation?.updateEvolutionConfig(updated)
            navigateTo(AppScreen.GAME)
        }
    }

    fun restartGame() {
        gameSimulation?.restart()
    }
}
