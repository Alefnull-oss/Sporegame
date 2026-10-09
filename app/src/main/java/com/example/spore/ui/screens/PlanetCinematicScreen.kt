package com.example.spore.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spore.cinematics.CinematicAudioSynthesizer
import com.example.spore.cinematics.CinematicVisualRenderer
import com.example.spore.cinematics.PlanetCinematicDefinition
import com.example.spore.data.model.CellEvolutionEntity
import com.example.spore.ui.viewmodel.AppScreen
import com.example.spore.ui.viewmodel.SporeViewModel

/** Plain (non-Compose) animation clock: mutated at 60 fps, read only in draw phases. */
private class CinematicClock { var seconds = 0f }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanetCinematicScreen(
    viewModel: SporeViewModel,
    modifier: Modifier = Modifier
) {
    val selectedPlanet by viewModel.selectedPlanet.collectAsStateWithLifecycle()
    val cellConfig by viewModel.cellEvolution.collectAsStateWithLifecycle()
    val activeCell = cellConfig ?: CellEvolutionEntity()

    val cinematicDef = remember(selectedPlanet.id) {
        PlanetCinematicDefinition.getCinematicForPlanet(selectedPlanet.id)
    }

    val audioSynth = remember { CinematicAudioSynthesizer() }
    var isMuted by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        audioSynth.start()
        onDispose {
            audioSynth.stop()
        }
    }

    BackHandler {
        audioSynth.stop()
        viewModel.navigateTo(AppScreen.GALAXY_MAP)
    }

    var currentPhaseIndex by remember { mutableIntStateOf(0) }
    // Precise per-frame timer: plain holder (NOT Compose state) so updating it
    // 60 times per second never invalidates the composition - it is read only
    // inside the Canvas draw phase. The UI instead samples it at 5 Hz below.
    val preciseClock = remember { CinematicClock() }
    var frameTick by remember { mutableLongStateOf(0L) } // Canvas draw-phase invalidation
    var phaseTimerSeconds by remember { mutableFloatStateOf(0f) } // UI sample (5 Hz)
    var isPlaying by remember { mutableStateOf(true) }
    var showCodexDialog by remember { mutableStateOf(false) }

    val currentPhase = cinematicDef.phases[currentPhaseIndex.coerceIn(0, cinematicDef.phases.size - 1)]

    // Audio synth parameter sync
    LaunchedEffect(currentPhase.stage, selectedPlanet.id) {
        audioSynth.updateParametersForStage(selectedPlanet.id, currentPhase.stage)
    }

    // Playback frame loop: 60 fps animation clock + 5 Hz UI sampling channel.
    // Previously phaseTimerSeconds was Compose state mutated every frame, which
    // recomposed the ENTIRE screen (header, narration card, buttons) 60 times
    // per second during the cinematic.
    var lastNanoTime = 0L
    LaunchedEffect(isPlaying, currentPhaseIndex) {
        var uiClockMs = 0f
        while (isPlaying) {
            withFrameNanos { nowNanos ->
                if (lastNanoTime == 0L) {
                    lastNanoTime = nowNanos
                } else {
                    val dt = ((nowNanos - lastNanoTime) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                    lastNanoTime = nowNanos
                    preciseClock.seconds += dt

                    if (preciseClock.seconds >= currentPhase.durationSeconds) {
                        if (currentPhaseIndex < cinematicDef.phases.size - 1) {
                            currentPhaseIndex++
                            preciseClock.seconds = 0f
                            phaseTimerSeconds = 0f
                            viewModel.triggerHaptic(40)
                        } else {
                            // Reached the end of the cinematic: loop or pause at final awakening
                            preciseClock.seconds = currentPhase.durationSeconds
                            isPlaying = false
                        }
                    }

                    // Canvas redraw at full frame rate (draw-phase only invalidation)
                    frameTick = nowNanos

                    // Throttled UI refresh channel (5 Hz)
                    uiClockMs += dt * 1000f
                    if (uiClockMs >= 200f) {
                        uiClockMs = 0f
                        phaseTimerSeconds = preciseClock.seconds
                    }
                }
            }
        }
    }

    // UI-sampled progress (5 Hz) for texts, chips and the progress bar
    val phaseProgress = (phaseTimerSeconds / currentPhase.durationSeconds).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()

        // 1. Procedural Scientific Visual Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("cinematic_canvas")
                .clickable {
                    // Tap viewport to play/pause
                    isPlaying = !isPlaying
                    if (isPlaying) lastNanoTime = 0L
                    viewModel.triggerHaptic(20)
                }
        ) {
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTick // Bind to the 60 fps draw-phase clock
            // Full-precision progress computed at draw time (never recomposes the UI)
            val drawProgress = (preciseClock.seconds / currentPhase.durationSeconds).coerceIn(0f, 1f)
            CinematicVisualRenderer.renderCinematicFrame(
                drawScope = this,
                screenWidth = screenW,
                screenHeight = screenH,
                planet = selectedPlanet,
                cinematicDef = cinematicDef,
                currentPhaseIndex = currentPhaseIndex,
                phaseProgress = drawProgress,
                totalElapsedTime = preciseClock.seconds + (currentPhaseIndex * 10f),
                cellEvolution = activeCell
            )
        }

        // 2. Anamorphic 21:9 Letterbox Bars (Top & Bottom cinematic borders)
        val letterboxHeight = 44.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(letterboxHeight)
                .align(Alignment.TopCenter)
                .background(Color.Black)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(letterboxHeight)
                .align(Alignment.BottomCenter)
                .background(Color.Black)
        )

        // 3. Top Header Bar (Planet name, hypothesis badge, audio, codex and exit)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        audioSynth.stop()
                        viewModel.navigateTo(AppScreen.GALAXY_MAP)
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xCC071526), CircleShape)
                        .border(1.dp, Color(0xFF1E3A5F), CircleShape)
                        .testTag("exit_cinematic_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Volver a Galaxia",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(selectedPlanet.atmosphereColorHex).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(selectedPlanet.atmosphereColorHex))
                        ) {
                            Text(
                                text = selectedPlanet.name,
                                color = Color(selectedPlanet.atmosphereColorHex),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cinematicDef.scientificTitle,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = currentPhase.scientificEpoch,
                        color = Color(0xFF80D8FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Audio Mute Toggle
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        audioSynth.isMuted = isMuted
                        viewModel.triggerHaptic(25)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xCC071526), CircleShape)
                        .border(1.dp, Color(0xFF1E3A5F), CircleShape)
                        .testTag("mute_audio_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Alternar Audio",
                        tint = if (isMuted) Color.Gray else Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Scientific Codex Button
                IconButton(
                    onClick = {
                        showCodexDialog = true
                        viewModel.triggerHaptic(30)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xCC071526), CircleShape)
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), CircleShape)
                        .testTag("open_scientific_codex_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = "Ver Teoría Científica",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 4. Floating Scientific Narration Card (Subtitles & Molecules)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("cinematic_narration_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF2071526)),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(selectedPlanet.atmosphereColorHex).copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Phase Header & Epoch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Fase ${currentPhaseIndex + 1} de ${cinematicDef.phases.size}: ${currentPhase.title}",
                            color = Color(selectedPlanet.atmosphereColorHex),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "🔬 ${currentPhase.scientificHypothesis}",
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Surface(
                        color = Color(0xFF0A2540),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A5F))
                    ) {
                        Text(
                            text = "${(phaseProgress * 100).toInt()}%",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Phase Progress Bar
                LinearProgressIndicator(
                    progress = { phaseProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(selectedPlanet.atmosphereColorHex),
                    trackColor = Color(0xFF1B2A3D),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scientific Narration Text
                Text(
                    text = currentPhase.narration,
                    color = Color(0xFFECEFF1),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Key Molecules Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Reactivos:",
                        color = Color(0xFF90A4AE),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    currentPhase.keyMolecules.forEach { molecule ->
                        Surface(
                            color = Color(0xFF0C1F38),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = molecule,
                                color = Color(0xFF80D8FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Playback Controls & Action Launch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Media step controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Previous Phase
                        IconButton(
                            onClick = {
                                if (currentPhaseIndex > 0) {
                                    currentPhaseIndex--
                                    phaseTimerSeconds = 0f
                                    lastNanoTime = 0L
                                    viewModel.triggerHaptic(30)
                                }
                            },
                            enabled = currentPhaseIndex > 0,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF0B192C), CircleShape)
                                .testTag("prev_phase_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Fase Anterior",
                                tint = if (currentPhaseIndex > 0) Color.White else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Play / Pause
                        IconButton(
                            onClick = {
                                isPlaying = !isPlaying
                                if (isPlaying) lastNanoTime = 0L
                                viewModel.triggerHaptic(35)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(selectedPlanet.atmosphereColorHex), CircleShape)
                                .testTag("play_pause_cinematic_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reanudar",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Next Phase
                        IconButton(
                            onClick = {
                                if (currentPhaseIndex < cinematicDef.phases.size - 1) {
                                    currentPhaseIndex++
                                    phaseTimerSeconds = 0f
                                    lastNanoTime = 0L
                                    viewModel.triggerHaptic(30)
                                }
                            },
                            enabled = currentPhaseIndex < cinematicDef.phases.size - 1,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF0B192C), CircleShape)
                                .testTag("next_phase_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Siguiente Fase",
                                tint = if (currentPhaseIndex < cinematicDef.phases.size - 1) Color.White else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Direct "Sembrar Vida / Comenzar Evolución" Launch Button
                    Button(
                        onClick = {
                            audioSynth.stop()
                            viewModel.launchPlanet(selectedPlanet)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(selectedPlanet.atmosphereColorHex)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("launch_from_cinematic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentPhaseIndex == cinematicDef.phases.size - 1) "Comenzar Vida" else "Omitir a Juego",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 5. Scientific Theory Codex Modal Dialog
        if (showCodexDialog) {
            Dialog(onDismissRequest = { showCodexDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .testTag("scientific_codex_dialog"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF071526)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Base Científica Real",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }

                            IconButton(
                                onClick = { showCodexDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Theory & Scientist Authors
                        Surface(
                            color = Color(0xFF0F2744),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = cinematicDef.primaryTheory,
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Investigadores: ${cinematicDef.realWorldScientists}",
                                    color = Color(0xFFCFD8DC),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Resumen del Mecanismo de Abiogénesis",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cinematicDef.scientificSummary,
                            color = Color(0xFFB0BEC5),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Planetary Parameters
                        Text(
                            text = "Condiciones Fisicoquímicas",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "• Atmósfera: ${cinematicDef.atmosphericComposition}",
                            color = Color(0xFF80D8FF),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• Hidrosfera: ${cinematicDef.oceanChemistry}",
                            color = Color(0xFF80D8FF),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• Fuente Energética: ${cinematicDef.energySource}",
                            color = Color(0xFF80D8FF),
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Peer-Reviewed Literature Citations
                        Text(
                            text = "Publicaciones Científicas Relevantes",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        cinematicDef.peerReviewedCitations.forEach { citation ->
                            Text(
                                text = "📄 $citation",
                                color = Color(0xFFB0BEC5),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showCodexDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text(
                                text = "Entendido",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
