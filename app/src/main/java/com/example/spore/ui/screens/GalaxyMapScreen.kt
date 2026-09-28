package com.example.spore.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spore.data.model.PlanetDefinition
import com.example.spore.ui.viewmodel.AppScreen
import com.example.spore.ui.viewmodel.SporeViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun GalaxyMapScreen(
    viewModel: SporeViewModel,
    modifier: Modifier = Modifier
) {
    val planetSaves by viewModel.allPlanetSaves.collectAsStateWithLifecycle()
    val selectedPlanet by viewModel.selectedPlanet.collectAsStateWithLifecycle()

    var galaxyRotation by remember { mutableFloatStateOf(0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Galaxy rotation animation
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { nowNanos ->
                galaxyRotation = (nowNanos / 1_000_000_000f) * 0.12f
            }
        }
    }

    val pulseAnim = rememberInfiniteTransition(label = "corePulse")
    val coreGlow by pulseAnim.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "coreGlow"
    )

    val currentSave = planetSaves.firstOrNull { it.planetId == selectedPlanet.id }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
    ) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val galaxyCenter = Offset(screenW / 2f + panOffset.x, screenH * 0.40f + panOffset.y)

        // 1. Interactive Galaxy & Planet Orbit Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("galaxy_canvas")
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        panOffset = Offset(
                            (panOffset.x + dragAmount.x).coerceIn(-180f, 180f),
                            (panOffset.y + dragAmount.y).coerceIn(-120f, 120f)
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        // Detect tap on planets
                        for (planet in PlanetDefinition.PLANETS) {
                            val orbitAngle = planet.galaxyAngle + galaxyRotation
                            val px = galaxyCenter.x + cos(orbitAngle) * planet.galaxyDistance
                            val py = galaxyCenter.y + sin(orbitAngle) * (planet.galaxyDistance * 0.65f) // Perspective tilt
                            val dist = (tapOffset - Offset(px, py)).getDistance()
                            if (dist < 55f) {
                                viewModel.selectPlanet(planet)
                                viewModel.triggerHaptic(30)
                                break
                            }
                        }
                    }
                }
        ) {
            // Galaxy Background Deep Nebula Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A237E).copy(alpha = 0.35f),
                        Color(0xFF311B92).copy(alpha = 0.20f),
                        Color(0xFF0D47A1).copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = galaxyCenter,
                    radius = 360f
                ),
                radius = 360f,
                center = galaxyCenter
            )

            // Spiral Star Dust Arms (drawn procedurally)
            val starCount = 180
            for (i in 0 until starCount) {
                val armIndex = i % 2
                val t = i.toFloat() / starCount
                val r = t * 300f
                val armAngle = (t * 4.5f * PI.toFloat()) + (armIndex * PI.toFloat()) + galaxyRotation
                val sx = galaxyCenter.x + cos(armAngle) * r
                val sy = galaxyCenter.y + sin(armAngle) * (r * 0.65f) // Inclined galaxy plane

                val starAlpha = (1f - t * 0.6f).coerceIn(0.15f, 0.9f)
                val starColor = if (i % 3 == 0) Color(0xFF80D8FF) else if (i % 3 == 1) Color(0xFFFFD54F) else Color.White
                drawCircle(
                    color = starColor.copy(alpha = starAlpha),
                    radius = if (i % 7 == 0) 2.6f else 1.4f,
                    center = Offset(sx, sy)
                )
            }

            // Galactic Core (Blazing supermassive center)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFFFD54F).copy(alpha = 0.85f * coreGlow),
                        Color(0xFFFF6D00).copy(alpha = 0.45f * coreGlow),
                        Color.Transparent
                    ),
                    center = galaxyCenter,
                    radius = 65f * coreGlow
                ),
                radius = 65f * coreGlow,
                center = galaxyCenter
            )

            // Draw Orbit Trajectories & Planets
            for (planet in PlanetDefinition.PLANETS) {
                val isSelected = planet.id == selectedPlanet.id

                // Orbit ring (ellipse due to perspective)
                drawCircle(
                    color = if (isSelected) Color(planet.atmosphereColorHex).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                    radius = planet.galaxyDistance,
                    center = galaxyCenter,
                    style = Stroke(width = if (isSelected) 2f else 1f)
                )

                // Planet position on inclined orbit
                val currentAngle = planet.galaxyAngle + galaxyRotation
                val planetX = galaxyCenter.x + cos(currentAngle) * planet.galaxyDistance
                val planetY = galaxyCenter.y + sin(currentAngle) * (planet.galaxyDistance * 0.65f)
                val planetCenter = Offset(planetX, planetY)
                val pr = planet.sizeDp * 0.45f

                // Atmosphere halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(planet.atmosphereColorHex).copy(alpha = if (isSelected) 0.65f else 0.35f),
                            Color.Transparent
                        ),
                        center = planetCenter,
                        radius = pr * 1.55f
                    ),
                    radius = pr * 1.55f,
                    center = planetCenter
                )

                // Clay Drop Shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = pr + 1f,
                    center = planetCenter + Offset(2f, 3f)
                )

                // Planet Ocean Mass (Clay 3D ball)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(planet.atmosphereColorHex),
                            Color(planet.planetColorHex),
                            Color(planet.oceanBgColor2)
                        ),
                        center = planetCenter - Offset(pr * 0.3f, pr * 0.3f),
                        radius = pr * 1.2f
                    ),
                    radius = pr,
                    center = planetCenter
                )

                // Continents (Clay spots)
                drawCircle(
                    color = Color(planet.continentsColorHex),
                    radius = pr * 0.35f,
                    center = planetCenter + Offset(pr * 0.2f, -pr * 0.1f)
                )
                drawCircle(
                    color = Color(planet.continentsColorHex),
                    radius = pr * 0.25f,
                    center = planetCenter + Offset(-pr * 0.3f, pr * 0.2f)
                )

                // Glossy clay specular shine
                drawCircle(
                    color = Color.White.copy(alpha = 0.7f),
                    radius = pr * 0.28f,
                    center = planetCenter - Offset(pr * 0.35f, pr * 0.35f)
                )

                // Selection Ring around the active planet
                if (isSelected) {
                    drawCircle(
                        color = Color.White,
                        radius = pr + 8f,
                        center = planetCenter,
                        style = Stroke(width = 2.5f)
                    )
                    drawCircle(
                        color = Color(planet.atmosphereColorHex),
                        radius = pr + 12f,
                        center = planetCenter,
                        style = Stroke(width = 1.5f)
                    )
                }
            }
        }

        // Top Galaxy Title & Codex Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "GALAXIA PRIMORDIAL",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Selecciona un planeta para tu evolución celular",
                    color = Color(0xFF80D8FF),
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.TROPHIC_WEB) },
                modifier = Modifier
                    .background(Color(0xFF0F1E36), CircleShape)
                    .border(1.dp, Color(0xFF1E3A5F), CircleShape)
                    .testTag("open_trophic_from_galaxy")
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Cadena Trófica",
                    tint = Color(0xFF80D8FF)
                )
            }
        }

        // Bottom Selected Planet Save Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(14.dp)
                .testTag("planet_info_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xF0071526)),
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(selectedPlanet.atmosphereColorHex))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header: Planet Name & Environment Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(Color(selectedPlanet.planetColorHex), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedPlanet.name,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                        }
                        Text(
                            text = selectedPlanet.oceanName,
                            color = Color(selectedPlanet.atmosphereColorHex),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Surface(
                        color = Color(selectedPlanet.atmosphereColorHex).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(selectedPlanet.atmosphereColorHex))
                    ) {
                        Text(
                            text = selectedPlanet.difficulty,
                            color = Color(selectedPlanet.atmosphereColorHex),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ocean & Environmental Impact Description
                Text(
                    text = selectedPlanet.description,
                    color = Color(0xFFCFD8DC),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Save File Status Details
                Surface(
                    color = Color(0xFF040D1A),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentSave != null && currentSave.hasPlayed) {
                            Column {
                                Text(
                                    text = "Especie: ${currentSave.speciesName} (Gen ${currentSave.generation})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Dieta: ${currentSave.mouthType} • Biomasa: ${currentSave.biomass.toInt()} µg",
                                    color = Color(0xFF80D8FF),
                                    fontSize = 11.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD600),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${currentSave.dnaPoints} ADN",
                                    color = Color(0xFFFFD600),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = Color(0xFF69F0AE),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Mundo Virgen: Listo para sembrar vida",
                                    color = Color(0xFF69F0AE),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Launch & Editor Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play / Launch Button
                    Button(
                        onClick = { viewModel.launchPlanet(selectedPlanet) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(selectedPlanet.atmosphereColorHex)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("launch_planet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentSave?.hasPlayed == true) "Continuar en ${selectedPlanet.name}" else "Sembrar Vida",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }

                    // Lab / Editor Button
                    OutlinedButton(
                        onClick = {
                            viewModel.selectPlanet(selectedPlanet)
                            viewModel.navigateTo(AppScreen.CELL_EDITOR)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("edit_planet_cell_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Reset planet save button (if has played)
                    if (currentSave?.hasPlayed == true) {
                        IconButton(
                            onClick = { viewModel.resetPlanet(selectedPlanet.id) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF261017), RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .testTag("reset_planet_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reiniciar Planeta",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
