package com.example.spore.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.spore.data.model.PlanetDefinition
import com.example.spore.game.engine.GameSimulation
import com.example.spore.game.engine.TrophicTier
import com.example.spore.game.engine.TrophicWebRules
import com.example.spore.game.engine.Vector2
import com.example.spore.game.water.GpuOceanShader
import com.example.spore.game.water.WaterRippleSystem
import com.example.spore.ui.components.CellVisualRenderer
import com.example.spore.ui.components.FastTextPainter
import com.example.spore.ui.components.FoodVisualRenderer
import com.example.spore.ui.components.OceanVisualRenderer
import com.example.spore.ui.viewmodel.AppScreen
import com.example.spore.ui.viewmodel.SporeViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameScreen(
    viewModel: SporeViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.GALAXY_MAP)
    }

    val cellConfig by viewModel.cellEvolution.collectAsStateWithLifecycle()
    val simulation = viewModel.gameSimulation ?: return
    val planet = simulation.planetDefinition

    var inputDirection by remember { mutableStateOf(Vector2.ZERO) }
    var joystickTouchOffset by remember { mutableStateOf<Offset?>(null) }
    var isPaused by remember { mutableStateOf(false) }

    // Anti-lag smooth frame loop with capped delta time
    var frameTick by remember { mutableLongStateOf(0L) }
    var lastNanoTime = 0L
    LaunchedEffect(isPaused) {
        while (!isPaused) {
            withFrameNanos { nowNanos ->
                if (lastNanoTime == 0L) {
                    lastNanoTime = nowNanos
                } else {
                    val dt = ((nowNanos - lastNanoTime) / 1_000_000_000f).coerceIn(0.005f, 0.033f)
                    lastNanoTime = nowNanos
                    simulation.update(dt, inputDirection)
                    frameTick = nowNanos
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(planet.oceanBgColor2))
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        val screenCenter = Offset(screenWidth / 2f, screenHeight / 2f)
        val gpuOceanShader = remember { GpuOceanShader() }

        // Dynamic camera zoom: smooth expansion as biomass grows
        val playerRadius = simulation.calculatePlayerRadius()
        val zoom = (1.0f - (simulation.player.biomass * 0.0015f)).coerceIn(0.68f, 1.15f)

        // Interactive Boundless Ocean Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("primordial_soup_canvas")
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            joystickTouchOffset = offset
                            val touchWorldX = (offset.x - screenCenter.x) / zoom + simulation.player.position.x
                            val touchWorldY = (offset.y - screenCenter.y) / zoom + simulation.player.position.y
                            simulation.rippleSystem.emitTouchRipple(
                                Vector2(touchWorldX, touchWorldY),
                                Color(planet.oceanRimColor)
                            )
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
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTick // Bind to Canvas draw phase for 60fps rendering without recomposing the UI tree

            val playerWorldPos = simulation.player.position
            val camX = screenCenter.x - (playerWorldPos.x * zoom)
            val camY = screenCenter.y - (playerWorldPos.y * zoom)
            val currentBiome = simulation.oceanTerrain.activeBiome

            // 0. High-Performance GPU AGSL Surface Shader (API 33+) or Skia GPU fallback (Zero-Allocation)
            gpuOceanShader.renderOceanSurface(
                drawScope = this,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                camX = camX,
                camY = camY,
                zoom = zoom,
                timeSeconds = simulation.gameTimeSeconds,
                deepColor = simulation.oceanTerrain.smoothedDeepColor,
                shallowColor = simulation.oceanTerrain.smoothedShallowColor,
                causticColor = simulation.oceanTerrain.smoothedCausticColor,
                playerX = playerWorldPos.x,
                playerY = playerWorldPos.y,
                playerRadius = playerRadius * 1.5f,
                playerAmp = if (simulation.player.velocity.length() > 20f || simulation.player.isDashing) 0.85f else 0.25f,
                rippleSystem = simulation.rippleSystem
            )

            // Anti-Lag Frustum Culling Box in World Coordinates
            val invZoom = 1f / zoom
            val viewMargin = 160f * invZoom
            val halfW = (screenWidth / 2f) * invZoom + viewMargin
            val halfH = (screenHeight / 2f) * invZoom + viewMargin
            val viewLeft = playerWorldPos.x - halfW
            val viewRight = playerWorldPos.x + halfW
            val viewTop = playerWorldPos.y - halfH
            val viewBottom = playerWorldPos.y + halfH

            // 1. Seabed Terrain (Living corals, volcanic vents, swaying kelp, sand dunes)
            OceanVisualRenderer.drawSeabedTerrain(
                drawScope = this,
                terrain = simulation.oceanTerrain,
                camX = camX,
                camY = camY,
                zoom = zoom,
                viewLeft = viewLeft,
                viewRight = viewRight,
                viewTop = viewTop,
                viewBottom = viewBottom,
                worldWidth = GameSimulation.WORLD_WIDTH,
                worldHeight = GameSimulation.WORLD_HEIGHT,
                timeSeconds = simulation.gameTimeSeconds
            )

            // 2. Hydrodynamic Water Ripples, Swimming Wakes & Cavitation Bubbles
            OceanVisualRenderer.drawWaterRipplesAndWakes(
                drawScope = this,
                rippleSystem = simulation.rippleSystem,
                camX = camX,
                camY = camY,
                zoom = zoom,
                viewLeft = viewLeft,
                viewRight = viewRight,
                viewTop = viewTop,
                viewBottom = viewBottom,
                troughColor = simulation.oceanTerrain.theme.wakeTroughColor
            )

            // 3. Ambient Soup Particles (Seamless wrapped)
            for (p in simulation.ambientParticles) {
                val delta = playerWorldPos.wrappedDeltaTo(p.position, GameSimulation.WORLD_WIDTH, GameSimulation.WORLD_HEIGHT)
                val screenX = screenCenter.x + delta.x * zoom
                val screenY = screenCenter.y + delta.y * zoom
                if (screenX in -30f..(screenWidth + 30f) && screenY in -30f..(screenHeight + 30f)) {
                    drawCircle(
                        color = p.color.copy(alpha = p.alpha),
                        radius = p.radius * zoom,
                        center = Offset(screenX, screenY)
                    )
                }
            }

            // 4. Poison Puddles (Wrapped - Zero shader allocation)
            for (poison in simulation.poisonPuddles) {
                val delta = playerWorldPos.wrappedDeltaTo(poison.position, GameSimulation.WORLD_WIDTH, GameSimulation.WORLD_HEIGHT)
                val screenX = screenCenter.x + delta.x * zoom
                val screenY = screenCenter.y + delta.y * zoom
                val r = poison.currentRadius * zoom
                if (screenX in -r..(screenWidth + r) && screenY in -r..(screenHeight + r)) {
                    val screenPos = Offset(screenX, screenY)
                    drawCircle(
                        color = Color(0xFF33691E).copy(alpha = 0.25f),
                        radius = r,
                        center = screenPos
                    )
                    drawCircle(
                        color = Color(0xFF76FF03).copy(alpha = 0.55f),
                        radius = r * 0.65f,
                        center = screenPos
                    )
                }
            }

            // 5. Electric Blasts (Wrapped)
            for (blast in simulation.electricBlasts) {
                val delta = playerWorldPos.wrappedDeltaTo(blast.position, GameSimulation.WORLD_WIDTH, GameSimulation.WORLD_HEIGHT)
                val screenX = screenCenter.x + delta.x * zoom
                val screenY = screenCenter.y + delta.y * zoom
                val r = blast.currentRadius * zoom
                if (screenX in -r..(screenWidth + r) && screenY in -r..(screenHeight + r)) {
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.50f),
                        radius = r,
                        center = Offset(screenX, screenY),
                        style = Stroke(width = 5f * zoom)
                    )
                }
            }

            // 6. Food & Nutrients (Wrapped to player view - Zero object copy)
            val foods = simulation.foods
            for (i in 0 until foods.size) {
                val food = foods[i]
                val delta = playerWorldPos.wrappedDeltaTo(food.position, GameSimulation.WORLD_WIDTH, GameSimulation.WORLD_HEIGHT)
                val screenX = screenCenter.x + delta.x * zoom
                val screenY = screenCenter.y + delta.y * zoom
                val r = food.radius * zoom
                if (screenX in -r - 20f..(screenWidth + r + 20f) && screenY in -r - 20f..(screenHeight + r + 20f)) {
                    FoodVisualRenderer.drawFood(
                        drawScope = this,
                        screenX = screenX,
                        screenY = screenY,
                        radius = r,
                        kind = food.kind,
                        foodId = food.id,
                        timeSeconds = simulation.gameTimeSeconds
                    )
                }
            }

            // 7. AI Microbes (Wrapped to player view)
            for (m in simulation.microbes) {
                val delta = playerWorldPos.wrappedDeltaTo(m.position, GameSimulation.WORLD_WIDTH, GameSimulation.WORLD_HEIGHT)
                val screenX = screenCenter.x + delta.x * zoom
                val screenY = screenCenter.y + delta.y * zoom
                val mRadiusScaled = m.radius * zoom
                if (screenX in -mRadiusScaled - 80f..(screenWidth + mRadiusScaled + 80f) &&
                    screenY in -mRadiusScaled - 80f..(screenHeight + mRadiusScaled + 80f)
                ) {
                    val screenPos = Offset(screenX, screenY)

                    // Draw Claymation Microbe
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
                        timeSeconds = simulation.gameTimeSeconds,
                        softBody = m.softBody,
                        flagellaChains = m.flagellaChains,
                        jawAperture = m.jawAperture,
                        isBiting = m.isBiting
                    )

                    // Cartoon Health Bar above microbe
                    val barWidth = mRadiusScaled * 1.8f
                    val barHeight = 4.5f * zoom
                    val barLeft = screenPos.x - barWidth / 2f
                    val barTop = screenPos.y - mRadiusScaled - 14f * zoom

                    drawRect(
                        color = Color.Black.copy(alpha = 0.6f),
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

            // 8. Player Cell (always centered in viewport)
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
                damageFlash = simulation.player.damageFlashTimer > 0f,
                softBody = simulation.player.softBody,
                flagellaChains = simulation.player.flagellaChains,
                jawAperture = simulation.player.jawAperture,
                isBiting = simulation.player.isBiting
            )

            // 9. Sensory Radar / Peripheral Threat Warning (Wrapped delta)
            for (m in simulation.microbes) {
                val delta = playerWorldPos.wrappedDeltaTo(m.position, GameSimulation.WORLD_WIDTH, GameSimulation.WORLD_HEIGHT)
                val dist = delta.length()
                if (dist > playerRadius + 180f && dist < simulation.playerStats.sensorRadius) {
                    val angle = delta.angle()
                    val radarDist = (screenHeight * 0.42f).coerceAtMost(screenWidth * 0.42f)
                    val indicatorPos = screenCenter + Offset(cos(angle) * radarDist, sin(angle) * radarDist)

                    val threatColor = when {
                        m.trophicTier == TrophicTier.APEX -> Color(0xFFD500F9)
                        TrophicWebRules.canPredatorAttack(m.diet, m.trophicTier, m.radius, simulation.playerStats.trophicTier, playerRadius) -> Color(0xFFFF1744)
                        TrophicWebRules.canPredatorAttack(simulation.playerStats.dietType, simulation.playerStats.trophicTier, playerRadius, m.trophicTier, m.radius) -> Color(0xFF76FF03)
                        else -> Color(0xFFFFD600)
                    }

                    drawCircle(
                        color = Color.Black.copy(alpha = 0.5f),
                        radius = (if (m.trophicTier == TrophicTier.APEX) 11f else 8f) * zoom,
                        center = indicatorPos
                    )
                    drawCircle(
                        color = threatColor,
                        radius = (if (m.trophicTier == TrophicTier.APEX) 9f else 6f) * zoom,
                        center = indicatorPos
                    )
                }
            }

            // 10. Water Surface Sun Caustics & Volumetric God Rays
            OceanVisualRenderer.drawWaterCausticsAndSunbeams(
                drawScope = this,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                camX = camX,
                camY = camY,
                zoom = zoom,
                timeSeconds = simulation.gameTimeSeconds,
                biome = currentBiome,
                causticColor = simulation.oceanTerrain.smoothedCausticColor,
                sunbeamColor = simulation.oceanTerrain.theme.sunbeamColor
            )

            // 11. Floating Notices (Zero-allocation Text Paint)
            for (n in simulation.notices) {
                val delta = playerWorldPos.wrappedDeltaTo(n.position, GameSimulation.WORLD_WIDTH, GameSimulation.WORLD_HEIGHT)
                val screenX = screenCenter.x + delta.x * zoom
                val screenY = screenCenter.y + delta.y * zoom
                if (screenX in 0f..screenWidth && screenY in 0f..screenHeight) {
                    val argb = android.graphics.Color.argb(
                        (n.alpha * 255).toInt(),
                        (n.color.red * 255).toInt(),
                        (n.color.green * 255).toInt(),
                        (n.color.blue * 255).toInt()
                    )
                    val paint = FastTextPainter.prepare(argb, textSizePx = 34f)
                    drawContext.canvas.nativeCanvas.drawText(n.text, screenX, screenY, paint)
                }
            }

            // 12. Virtual Joystick Indicator (Chubby clay circle)
            joystickTouchOffset?.let { touchOrigin ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = 55f,
                    center = touchOrigin,
                    style = Stroke(width = 3.5f)
                )
                val stickHead = touchOrigin + Offset(inputDirection.x * 38f, inputDirection.y * 38f)
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.75f),
                    radius = 24f,
                    center = stickHead
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = 8f,
                    center = stickHead - Offset(6f, 6f)
                )
            }
        }

        // Top HUD Overlay
        GameTopHud(
            simulation = simulation,
            planet = planet,
            onOpenGalaxy = {
                isPaused = true
                viewModel.navigateTo(AppScreen.GALAXY_MAP)
            },
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

        // Bottom-Left Controls & Anti-lag badge
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { isPaused = !isPaused },
                modifier = Modifier
                    .background(Color(0xFF0B192C).copy(alpha = 0.8f), CircleShape)
                    .border(1.dp, Color(0xFF1E3A5F), CircleShape)
                    .testTag("pause_button")
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = "Pausar/Reanudar",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Anti-Lag active indicator badge
            Surface(
                color = Color(0xFF071526).copy(alpha = 0.75f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Anti-Lag 60fps",
                        color = Color(0xFF80D8FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
    planet: PlanetDefinition,
    onOpenGalaxy: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenTrophicWeb: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xDD071526)),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(planet.oceanRimColor).copy(alpha = 0.6f))
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
                // Planet & Trophic Tier Badge
                val tier = simulation.playerStats.trophicTier
                val tierColor = when (tier) {
                    TrophicTier.APEX -> Color(0xFFD500F9)
                    TrophicTier.PREDATOR -> Color(0xFFFF3D00)
                    TrophicTier.SECONDARY_CONSUMER -> Color(0xFFFFD600)
                    else -> Color(0xFF76FF03)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(planet.atmosphereColorHex).copy(alpha = 0.22f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(planet.atmosphereColorHex)),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = planet.name,
                            color = Color(planet.atmosphereColorHex),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = tierColor.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, tierColor)
                    ) {
                        Text(
                            text = "Nivel ${tier.level}",
                            color = tierColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }

                // DNA Bank display (scarcity emphasis)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Galaxy Button
                    IconButton(
                        onClick = onOpenGalaxy,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("open_galaxy_hud")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Galaxia",
                            tint = Color(planet.atmosphereColorHex)
                        )
                    }

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

                    // Evolution Lab Button (Isolated pulse recomposition)
                    val readyToEvolve = simulation.player.dnaPoints >= 20
                    EvolveLabButton(
                        readyToEvolve = readyToEvolve,
                        onOpenEditor = onOpenEditor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Ocean Terrain Biome & Bathymetric Depth Status (Themed & Zero-Flicker)
            val currentBiome = simulation.oceanTerrain.activeBiome
            val biomeTitle = simulation.oceanTerrain.getBiomeTitle(currentBiome)
            val biomeColor = simulation.oceanTerrain.theme.getShallowColor(currentBiome)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🌊 $biomeTitle",
                        color = biomeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${currentBiome.avgDepthMeters}m",
                        color = Color(0xFF90A4AE),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Océano Infinito Sin Muros",
                    color = simulation.oceanTerrain.theme.defaultRippleColor.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

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
                            .height(7.dp)
                            .clip(RoundedCornerShape(3.5.dp)),
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
                            .height(7.dp)
                            .clip(RoundedCornerShape(3.5.dp)),
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
private fun EvolveLabButton(
    readyToEvolve: Boolean,
    onOpenEditor: () -> Unit
) {
    val alpha = if (readyToEvolve) {
        val pulseAnim = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by pulseAnim.animateFloat(
            initialValue = 0.65f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "pulseAlpha"
        )
        pulseAlpha
    } else 1.0f

    Button(
        onClick = onOpenEditor,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (readyToEvolve) Color(0xFF00E5FF).copy(alpha = alpha) else Color(0xFF1E3A5F)
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
        // 1. Electric Shock Ability Button
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
                    .size(56.dp)
                    .border(2.dp, Color(0xFF006064), CircleShape)
                    .testTag("electric_ability_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Descarga Eléctrica",
                    tint = if (elecReady) Color.Black else Color.Gray
                )
            }
        }

        // 2. Poison Cloud Ability Button
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
                    .size(56.dp)
                    .border(2.dp, Color(0xFF1B5E20), CircleShape)
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
                .size(64.dp)
                .border(2.5.dp, Color(0xFFE65100), CircleShape)
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
                        shape = RoundedCornerShape(14.dp),
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
                        shape = RoundedCornerShape(14.dp),
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
