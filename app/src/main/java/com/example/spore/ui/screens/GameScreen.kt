package com.example.spore.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spore.game.engine.GameSimulation
import com.example.spore.game.engine.TrophicTier
import com.example.spore.game.engine.TrophicWebRules
import com.example.spore.game.engine.Vector2
import com.example.spore.ui.components.CellVisualRenderer
import com.example.spore.ui.components.FoodVisualRenderer
import com.example.spore.ui.viewmodel.AppScreen
import com.example.spore.ui.viewmodel.SporeViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameScreen(
    viewModel: SporeViewModel,
    modifier: Modifier = Modifier
) {
    val cellConfig by viewModel.cellEvolution.collectAsStateWithLifecycle()
    val simulation = viewModel.gameSimulation ?: return

    var inputDirection by remember { mutableStateOf(Vector2.ZERO) }
    var joystickTouchOffset by remember { mutableStateOf<Offset?>(null) }
    var isPaused by remember { mutableStateOf(false) }

    // Game loop running on choreo frame clock
    var lastNanoTime by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPaused) {
        while (!isPaused) {
            withFrameNanos { nowNanos ->
                if (lastNanoTime == 0f) {
                    lastNanoTime = nowNanos.toFloat()
                } else {
                    val dt = ((nowNanos - lastNanoTime) / 1_000_000_000f).coerceIn(0.005f, 0.04f)
                    lastNanoTime = nowNanos.toFloat()
                    simulation.update(dt, inputDirection)
                }
            }
        }
    }

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040D1A))
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        val screenCenter = Offset(screenWidth / 2f, screenHeight / 2f)

        // Dynamic camera zoom: zooms out slightly as biomass grows
        val playerRadius = simulation.calculatePlayerRadius()
        val zoom = (1.0f - (simulation.player.biomass * 0.0018f)).coerceIn(0.65f, 1.15f)

        // Interactive Primordial Soup Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("primordial_soup_canvas")
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            joystickTouchOffset = offset
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val origin = joystickTouchOffset ?: change.position
                            val delta = change.position - origin
                            val len = delta.getDistance()
                            if (len > 8f) {
                                val norm = delta / len
                                inputDirection = Vector2(norm.x, norm.y)
                            }
                        },
                        onDragEnd = {
                            inputDirection = Vector2.ZERO
                            joystickTouchOffset = null
                        },
                        onDragCancel = {
                            inputDirection = Vector2.ZERO
                            joystickTouchOffset = null
                        }
                    )
                }
        ) {
            val playerWorldPos = simulation.player.position
            val camX = screenCenter.x - (playerWorldPos.x * zoom)
            val camY = screenCenter.y - (playerWorldPos.y * zoom)

            // 1. Draw World Boundary Rim
            drawRect(
                color = Color(0xFF003049).copy(alpha = 0.35f),
                topLeft = Offset(camX, camY),
                size = Size(GameSimulation.WORLD_WIDTH * zoom, GameSimulation.WORLD_HEIGHT * zoom),
                style = Stroke(width = 8f * zoom)
            )

            // 2. Ambient Soup Particles (microscopic light floaters)
            for (p in simulation.ambientParticles) {
                val screenPos = Offset(p.position.x * zoom + camX, p.position.y * zoom + camY)
                if (screenPos.x in -50f..screenWidth + 50f && screenPos.y in -50f..screenHeight + 50f) {
                    drawCircle(
                        color = p.color.copy(alpha = p.alpha),
                        radius = p.radius * zoom,
                        center = screenPos
                    )
                }
            }

            // 3. Poison Puddles
            for (poison in simulation.poisonPuddles) {
                val screenPos = Offset(poison.position.x * zoom + camX, poison.position.y * zoom + camY)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF76FF03).copy(alpha = 0.55f),
                            Color(0xFF33691E).copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = screenPos,
                        radius = poison.currentRadius * zoom
                    ),
                    radius = poison.currentRadius * zoom,
                    center = screenPos
                )
            }

            // 4. Electric Blasts
            for (blast in simulation.electricBlasts) {
                val screenPos = Offset(blast.position.x * zoom + camX, blast.position.y * zoom + camY)
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.45f),
                    radius = blast.currentRadius * zoom,
                    center = screenPos,
                    style = Stroke(width = 4f * zoom)
                )
            }

            // 5. Food & Nutrients
            for (food in simulation.foods) {
                val screenPos = Offset(food.position.x * zoom + camX, food.position.y * zoom + camY)
                if (screenPos.x in -60f..screenWidth + 60f && screenPos.y in -60f..screenHeight + 60f) {
                    val transformedFood = food.copy(
                        position = Vector2(screenPos.x, screenPos.y),
                        radius = food.radius * zoom
                    )
                    FoodVisualRenderer.drawFood(this, transformedFood, simulation.gameTimeSeconds)
                }
            }

            // 6. AI Microbes
            for (m in simulation.microbes) {
                val screenPos = Offset(m.position.x * zoom + camX, m.position.y * zoom + camY)
                val mRadiusScaled = m.radius * zoom

                if (screenPos.x in -120f..screenWidth + 120f && screenPos.y in -120f..screenHeight + 120f) {
                    // Cell body & parts
                    CellVisualRenderer.drawCell(
                        drawScope = this,
                        center = screenPos,
                        radius = mRadiusScaled,
                        angleRad = m.angle,
                        primaryColor = m.primaryColor,
                        mouthType = m.diet,
                        flagellaCount = m.flagellaCount,
                        ciliaCount = m.ciliaCount,
                        spikesCount = m.spikesCount,
                        hasPoison = m.hasPoison,
                        hasElectric = false,
                        eyeType = if (m.trophicTier.level >= 3) "COMPOUND" else "BASIC",
                        timeSeconds = simulation.gameTimeSeconds
                    )

                    // Health & Status bar above microbe
                    val barWidth = mRadiusScaled * 1.8f
                    val barHeight = 4f * zoom
                    val barLeft = screenPos.x - barWidth / 2f
                    val barTop = screenPos.y - mRadiusScaled - 14f * zoom

                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(barLeft, barTop),
                        size = Size(barWidth, barHeight)
                    )
                    val hpRatio = (m.health / m.maxHealth).coerceIn(0f, 1f)
                    val hpColor = when (m.trophicTier) {
                        TrophicTier.APEX -> Color(0xFFD500F9)
                        TrophicTier.PREDATOR -> Color(0xFFFF3D00)
                        TrophicTier.SECONDARY_CONSUMER -> Color(0xFFFFD600)
                        else -> Color(0xFF76FF03)
                    }
                    drawRect(
                        color = hpColor,
                        topLeft = Offset(barLeft, barTop),
                        size = Size(barWidth * hpRatio, barHeight)
                    )
                }
            }

            // 7. Player Cell (always in center of viewport!)
            val playerColor = Color(cellConfig?.primaryColorHex ?: 0xFF00E5FF)
            CellVisualRenderer.drawCell(
                drawScope = this,
                center = screenCenter,
                radius = playerRadius * zoom,
                angleRad = simulation.player.angle,
                primaryColor = playerColor,
                mouthType = simulation.playerStats.dietType,
                flagellaCount = cellConfig?.flagellaCount ?: 1,
                ciliaCount = cellConfig?.ciliaCount ?: 1,
                spikesCount = cellConfig?.spikesCount ?: 0,
                hasPoison = simulation.playerStats.hasPoison,
                hasElectric = simulation.playerStats.hasElectricShock,
                eyeType = cellConfig?.eyeType ?: "BASIC",
                timeSeconds = simulation.gameTimeSeconds,
                isDashing = simulation.player.isDashing,
                damageFlash = simulation.player.damageFlashTimer > 0f
            )

            // 8. Sensory Radar / Peripheral Threat Warning
            for (m in simulation.microbes) {
                val dist = simulation.player.position.distanceTo(m.position)
                if (dist > playerRadius + 180f && dist < simulation.playerStats.sensorRadius) {
                    val angle = (m.position - simulation.player.position).angle()
                    val radarDist = (screenHeight * 0.42f).coerceAtMost(screenWidth * 0.42f)
                    val indicatorPos = screenCenter + Offset(cos(angle) * radarDist, sin(angle) * radarDist)

                    val threatColor = when {
                        m.trophicTier == TrophicTier.APEX -> Color(0xFFD500F9)
                        TrophicWebRules.canPredatorAttack(m.diet, m.trophicTier, m.radius, simulation.playerStats.trophicTier, playerRadius) -> Color(0xFFFF1744)
                        TrophicWebRules.canPredatorAttack(simulation.playerStats.dietType, simulation.playerStats.trophicTier, playerRadius, m.trophicTier, m.radius) -> Color(0xFF76FF03)
                        else -> Color(0xFFFFD600)
                    }

                    drawCircle(
                        color = threatColor.copy(alpha = 0.85f),
                        radius = (if (m.trophicTier == TrophicTier.APEX) 9f else 6f) * zoom,
                        center = indicatorPos
                    )
                }
            }

            // 9. Floating Notices (Damage, ADN, Eaten)
            for (n in simulation.notices) {
                val screenPos = Offset(n.position.x * zoom + camX, n.position.y * zoom + camY)
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(
                            (n.alpha * 255).toInt(),
                            (n.color.red * 255).toInt(),
                            (n.color.green * 255).toInt(),
                            (n.color.blue * 255).toInt()
                        )
                        textSize = 34f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        textAlign = android.graphics.Paint.Align.CENTER
                        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                    }
                    drawText(n.text, screenPos.x, screenPos.y, paint)
                }
            }

            // 10. Virtual Joystick Ring if touching
            joystickTouchOffset?.let { touchOrigin ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = 55f,
                    center = touchOrigin,
                    style = Stroke(width = 3f)
                )
                val stickHead = touchOrigin + Offset(inputDirection.x * 38f, inputDirection.y * 38f)
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.65f),
                    radius = 24f,
                    center = stickHead
                )
            }
        }

        // Top HUD Overlay
        GameTopHud(
            simulation = simulation,
            pulseAlpha = pulseAlpha,
            onOpenEditor = {
                isPaused = true
                viewModel.navigateTo(AppScreen.CELL_EDITOR)
            },
            onOpenTrophicWeb = {
                isPaused = true
                viewModel.navigateTo(AppScreen.TROPHIC_WEB)
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )

        // Bottom Action Controls (Turbo, Poison, Electric Shock)
        GameActionButtons(
            simulation = simulation,
            onTriggerDash = { simulation.triggerDash() },
            onTriggerPoison = { simulation.triggerPoison() },
            onTriggerElectric = { simulation.triggerElectricShock() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp)
        )

        // Pause / Resume Button
        IconButton(
            onClick = { isPaused = !isPaused },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 24.dp)
                .background(Color(0xFF0B192C).copy(alpha = 0.7f), CircleShape)
                .testTag("pause_button")
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = "Pausar/Reanudar",
                tint = Color.White
            )
        }

        // Game Over Dialog Overlay
        if (simulation.isGameOver) {
            GameOverOverlay(
                killerName = simulation.killerName,
                dnaCollected = simulation.player.dnaPoints,
                maxBiomass = simulation.player.biomass,
                microbesEaten = simulation.microbesEatenCount,
                onRestart = {
                    simulation.restart()
                },
                onEvolve = {
                    viewModel.navigateTo(AppScreen.CELL_EDITOR)
                }
            )
        }
    }
}

@Composable
private fun GameTopHud(
    simulation: GameSimulation,
    pulseAlpha: Float,
    onOpenEditor: () -> Unit,
    onOpenTrophicWeb: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xCC071526)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A5F))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Trophic Tier Badge
                val tier = simulation.playerStats.trophicTier
                val tierColor = when (tier) {
                    TrophicTier.APEX -> Color(0xFFD500F9)
                    TrophicTier.PREDATOR -> Color(0xFFFF3D00)
                    TrophicTier.SECONDARY_CONSUMER -> Color(0xFFFFD600)
                    else -> Color(0xFF76FF03)
                }

                Surface(
                    color = tierColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, tierColor)
                ) {
                    Text(
                        text = "Nivel ${tier.level}: ${tier.displayName.split(" ")[0]}",
                        color = tierColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // DNA Bank display
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "ADN",
                        tint = Color(0xFFFFD600),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${simulation.player.dnaPoints} ADN",
                        color = Color(0xFFFFD600),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }

                // Quick Navigation Actions
                Row {
                    IconButton(
                        onClick = onOpenTrophicWeb,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("open_trophic_web_hud")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Cadena Trófica",
                            tint = Color(0xFF80D8FF)
                        )
                    }

                    // Evolution Lab Button (pulsates when enough DNA)
                    val readyToEvolve = simulation.player.dnaPoints >= 20
                    Button(
                        onClick = onOpenEditor,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (readyToEvolve) Color(0xFF00E5FF).copy(alpha = pulseAlpha) else Color(0xFF1E3A5F)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("open_editor_hud")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Laboratorio",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Mutar",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Vital Stats Bars (HP and Biomass)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Health Bar
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Salud: ${simulation.player.health.toInt()} / ${simulation.player.maxHealth.toInt()}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (simulation.playerStats.armorDamageReduction > 0f) {
                            Text(
                                text = "Blindaje -${(simulation.playerStats.armorDamageReduction * 100).toInt()}%",
                                color = Color(0xFF80D8FF),
                                fontSize = 10.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { (simulation.player.health / simulation.player.maxHealth).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFF5252),
                        trackColor = Color(0xFF37474F),
                        strokeCap = StrokeCap.Round
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Biomass / Growth meter
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Biomasa: ${simulation.player.biomass.toInt()} µg",
                            color = Color(0xFF69F0AE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Dieta: ${simulation.playerStats.dietType.name.take(4)}",
                            color = Color(0xFFB0BEC5),
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { (simulation.player.biomass / 120f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF00E676),
                        trackColor = Color(0xFF37474F),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun GameActionButtons(
    simulation: GameSimulation,
    onTriggerDash: () -> Unit,
    onTriggerPoison: () -> Unit,
    onTriggerElectric: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Electric Shock Ability Button (if unlocked)
        if (simulation.playerStats.hasElectricShock) {
            val elecReady = simulation.player.electricCooldownTimer <= 0f
            Button(
                onClick = onTriggerElectric,
                enabled = elecReady,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (elecReady) Color(0xFF00E5FF) else Color(0xFF37474F),
                    disabledContainerColor = Color(0xFF37474F)
                ),
                modifier = Modifier
                    .size(54.dp)
                    .testTag("electric_ability_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Descarga Eléctrica",
                    tint = if (elecReady) Color.Black else Color.Gray
                )
            }
        }

        // 2. Poison Cloud Ability Button (if unlocked)
        if (simulation.playerStats.hasPoison) {
            val poisonReady = simulation.player.poisonCooldownTimer <= 0f
            Button(
                onClick = onTriggerPoison,
                enabled = poisonReady,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (poisonReady) Color(0xFF76FF03) else Color(0xFF37474F),
                    disabledContainerColor = Color(0xFF37474F)
                ),
                modifier = Modifier
                    .size(54.dp)
                    .testTag("poison_ability_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Nube Tóxica",
                    tint = if (poisonReady) Color.Black else Color.Gray
                )
            }
        }

        // 3. Turbo / Dash Burst Button
        val dashReady = simulation.player.dashCooldownTimer <= 0f
        Button(
            onClick = onTriggerDash,
            enabled = dashReady,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (dashReady) Color(0xFFFFD600) else Color(0xFF37474F),
                disabledContainerColor = Color(0xFF37474F)
            ),
            modifier = Modifier
                .size(62.dp)
                .testTag("dash_burst_button")
        ) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "Impulso Dash",
                tint = if (dashReady) Color.Black else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun GameOverOverlay(
    killerName: String,
    dnaCollected: Int,
    maxBiomass: Float,
    microbesEaten: Int,
    onRestart: () -> Unit,
    onEvolve: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF5252))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¡CÉLULA ASIMILADA!",
                    color = Color(0xFFFF5252),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (killerName.isNotBlank()) "Fuiste devorado por: $killerName" else "Tu membrana colapsó en el caldo primordial.",
                    color = Color(0xFFECEFF1),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats summary
                Surface(
                    color = Color(0xFF071224),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ADN Almacenado:", color = Color.Gray, fontSize = 13.sp)
                            Text("$dnaCollected", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Biomasa Cumbre:", color = Color.Gray, fontSize = 13.sp)
                            Text("${maxBiomass.toInt()} µg", color = Color(0xFF69F0AE), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Microbios Devorados:", color = Color.Gray, fontSize = 13.sp)
                            Text("$microbesEaten", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRestart,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restart_game_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reaparecer", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onEvolve,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("evolve_after_death_button")
                    ) {
                        Icon(imageVector = Icons.Default.Science, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mutar ADN", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
