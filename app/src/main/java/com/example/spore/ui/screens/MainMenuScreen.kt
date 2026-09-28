package com.example.spore.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spore.data.model.CellEvolutionEntity
import com.example.spore.game.engine.CellEvolutionConfig
import com.example.spore.game.engine.DietType
import com.example.spore.ui.components.CellVisualRenderer
import com.example.spore.ui.viewmodel.AppScreen
import com.example.spore.ui.viewmodel.SporeViewModel
import kotlin.math.sin

@Composable
fun MainMenuScreen(
    viewModel: SporeViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.GALAXY_MAP)
    }

    val selectedPlanet by viewModel.selectedPlanet.collectAsStateWithLifecycle()
    val cellConfig by viewModel.cellEvolution.collectAsStateWithLifecycle()
    val draft = cellConfig ?: CellEvolutionEntity()
    val stats = remember(draft) { CellEvolutionConfig.calculateStats(draft) }

    var animTime by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { now ->
                animTime = (now / 1_000_000_000f)
            }
        }
    }

    val glowTransition = rememberInfiniteTransition(label = "titleGlow")
    val titlePulse by glowTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "titlePulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040D1A))
    ) {
        // Atmospheric Primordial Background Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Deep gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF061426), Color(0xFF020710)),
                    startY = 0f,
                    endY = h
                )
            )

            // Floating background cells
            val floaters = listOf(
                Offset(w * 0.18f, h * 0.22f + sin(animTime * 1.5f) * 20f) to Color(0xFF00E5FF),
                Offset(w * 0.82f, h * 0.35f + sin(animTime * 1.2f + 1f) * 25f) to Color(0xFF76FF03),
                Offset(w * 0.25f, h * 0.80f + sin(animTime * 1.8f + 2f) * 18f) to Color(0xFFFF5252),
                Offset(w * 0.78f, h * 0.75f + sin(animTime * 1.4f + 3f) * 22f) to Color(0xFFFFD600)
            )

            for ((pos, col) in floaters) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(col.copy(alpha = 0.25f), Color.Transparent),
                        center = pos,
                        radius = 80f
                    ),
                    radius = 80f,
                    center = pos
                )
            }

            // Preview player's cell swimming gently in the center
            val previewCenter = Offset(w / 2f, h * 0.44f + sin(animTime * 2f) * 12f)
            CellVisualRenderer.drawCell(
                drawScope = this,
                center = previewCenter,
                radius = 58f,
                angleRad = sin(animTime * 0.8f) * 0.25f,
                primaryColor = Color(draft.primaryColorHex),
                mouthType = stats.dietType,
                flagellaCount = draft.flagellaCount,
                ciliaCount = draft.ciliaCount,
                spikesCount = draft.spikesCount,
                hasPoison = draft.poisonGland,
                hasElectric = draft.electricOrgan,
                eyeType = draft.eyeType,
                timeSeconds = animTime
            )
        }

        // Main Menu Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header / Game Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Surface(
                    color = Color(0xFF00E5FF).copy(alpha = 0.15f * titlePulse),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = titlePulse))
                ) {
                    Text(
                        text = "ESTADIO DE CÉLULA",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "SPORE PRIMORDIAL",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Evolución y Cadena Trófica Microscópica",
                    color = Color(0xFF80D8FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Cell Identity Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC0B192C)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A5F))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = draft.speciesName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Generación ${draft.generation} • ${stats.trophicTier.displayName.split(" ")[0]}",
                            color = Color(0xFF80D8FF),
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF071224), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "ADN",
                            tint = Color(0xFFFFD600),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${draft.dnaPoints} ADN",
                            color = Color(0xFFFFD600),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Menu Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Change Planet / Galaxy Button
                OutlinedButton(
                    onClick = { viewModel.navigateTo(AppScreen.GALAXY_MAP) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(selectedPlanet.atmosphereColorHex)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("change_planet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = Color(selectedPlanet.atmosphereColorHex),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Planeta: ${selectedPlanet.name} • ${selectedPlanet.oceanName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(selectedPlanet.atmosphereColorHex)
                    )
                }

                // Play Button
                Button(
                    onClick = { viewModel.navigateTo(AppScreen.GAME) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(selectedPlanet.atmosphereColorHex)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("play_game_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nadar en ${selectedPlanet.name}",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cell Editor / DNA Lab Button
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(AppScreen.CELL_EDITOR) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("open_cell_editor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mutar ADN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Trophic Web / Bestiary Button
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(AppScreen.TROPHIC_WEB) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF76FF03)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("open_trophic_web_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFF76FF03),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cadena Trófica",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
