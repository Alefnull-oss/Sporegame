package com.example.spore.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spore.game.engine.CellEvolutionConfig
import com.example.spore.game.engine.DietType
import com.example.spore.game.engine.TrophicTier
import com.example.spore.ui.components.CellVisualRenderer
import com.example.spore.ui.viewmodel.AppScreen
import com.example.spore.ui.viewmodel.SporeViewModel

@Composable
fun CellEditorScreen(
    viewModel: SporeViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.GAME)
    }

    val draft by viewModel.editorDraft.collectAsStateWithLifecycle()
    val stats = remember(draft) { CellEvolutionConfig.calculateStats(draft) }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Bocas (Dieta)", "Locomoción", "Ataque/Defensa", "Sentidos/Piel")

    var previewTime by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { now ->
                previewTime = (now / 1_000_000_000f)
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF040D1A),
        topBar = {
            EditorTopBar(
                dnaPoints = draft.dnaPoints,
                onBack = { viewModel.navigateTo(AppScreen.GAME) },
                onApply = { viewModel.saveAndApplyMutations() }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Live Preview Card (Upper half)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A182E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A5F))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Cell Preview Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        CellVisualRenderer.drawCell(
                            drawScope = this,
                            center = center,
                            radius = 52f,
                            angleRad = 0f,
                            primaryColor = Color(draft.primaryColorHex),
                            mouthType = stats.dietType,
                            flagellaCount = draft.flagellaCount,
                            ciliaCount = draft.ciliaCount,
                            spikesCount = draft.spikesCount,
                            hasPoison = draft.poisonGland,
                            hasElectric = draft.electricOrgan,
                            eyeType = draft.eyeType,
                            timeSeconds = previewTime
                        )
                    }

                    // Trophic Role Ribbon
                    val tierColor = when (stats.trophicTier) {
                        TrophicTier.APEX -> Color(0xFFD500F9)
                        TrophicTier.PREDATOR -> Color(0xFFFF3D00)
                        TrophicTier.SECONDARY_CONSUMER -> Color(0xFFFFD600)
                        else -> Color(0xFF76FF03)
                    }
                    Surface(
                        color = tierColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, tierColor),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "${stats.trophicTier.displayName} • Dieta ${stats.dietType.displayName.split(" ")[0]}",
                            color = tierColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Quick Species Renaming
                    Text(
                        text = "${draft.speciesName} (Gen ${draft.generation})",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )
                }
            }

            // Stat Preview Bars
            StatSummarySection(stats = stats)

            // Category Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF071526),
                contentColor = Color(0xFF00E5FF),
                edgePadding = 12.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Evolution Options List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Bocas & Dieta
                        item {
                            MouthSelectorItem(
                                title = "Filtro Herbívoro",
                                description = "Especializado en absorber fitoplancton y algas microscópicas. Rápida asimilación de nutrientes vegetales.",
                                isSelected = draft.mouthType == "HERBIVORE",
                                cost = CellEvolutionConfig.COST_MOUTH_CHANGE,
                                canAfford = draft.dnaPoints >= CellEvolutionConfig.COST_MOUTH_CHANGE || draft.mouthType == "HERBIVORE",
                                onSelect = { viewModel.setDraftMouth("HERBIVORE") }
                            )
                        }
                        item {
                            MouthSelectorItem(
                                title = "Mandíbula Carnívora",
                                description = "Dientes y bordes afilados para desgarrar otros microbios y devorar carne orgánica. Permite cazar.",
                                isSelected = draft.mouthType == "CARNIVORE",
                                cost = CellEvolutionConfig.COST_MOUTH_CHANGE,
                                canAfford = draft.dnaPoints >= CellEvolutionConfig.COST_MOUTH_CHANGE || draft.mouthType == "CARNIVORE",
                                onSelect = { viewModel.setDraftMouth("CARNIVORE") }
                            )
                        }
                        item {
                            MouthSelectorItem(
                                title = "Probóscide Omnívora",
                                description = "Tubo digestivo versátil. Puede sorber algas vegetales y restos de carne orgánica.",
                                isSelected = draft.mouthType == "OMNIVORE",
                                cost = CellEvolutionConfig.COST_MOUTH_CHANGE,
                                canAfford = draft.dnaPoints >= CellEvolutionConfig.COST_MOUTH_CHANGE || draft.mouthType == "OMNIVORE",
                                onSelect = { viewModel.setDraftMouth("OMNIVORE") }
                            )
                        }
                    }
                    1 -> {
                        // Locomoción (Flagelos, Cilios, Sifón)
                        item {
                            CounterPartItem(
                                title = "Flagelos Ondulantes",
                                description = "Cola motriz larga que propulsa la célula con fuerza hacia adelante. (+Velocidad punta)",
                                count = draft.flagellaCount,
                                maxCount = 4,
                                cost = CellEvolutionConfig.COST_FLAGELLUM,
                                canAfford = draft.dnaPoints >= CellEvolutionConfig.COST_FLAGELLUM,
                                onIncrement = { viewModel.upgradeFlagella() },
                                onDecrement = { viewModel.downgradeFlagella() }
                            )
                        }
                        item {
                            CounterPartItem(
                                title = "Cilios Periféricos",
                                description = "Finos filamentos vibrantes alrededor de la membrana. Permiten giros inmediatos y maniobrabilidad. (+Agilidad)",
                                count = draft.ciliaCount,
                                maxCount = 4,
                                cost = CellEvolutionConfig.COST_CILIA,
                                canAfford = draft.dnaPoints >= CellEvolutionConfig.COST_CILIA,
                                onIncrement = { viewModel.upgradeCilia() },
                                onDecrement = { viewModel.downgradeCilia() }
                            )
                        }
                        item {
                            CounterPartItem(
                                title = "Sifón a Chorro",
                                description = "Órgano hidrodinámico que expulsa agua a presión. Desbloquea la habilidad activa de Impulso Turbo.",
                                count = draft.jetCount,
                                maxCount = 2,
                                cost = CellEvolutionConfig.COST_JET,
                                canAfford = draft.dnaPoints >= CellEvolutionConfig.COST_JET,
                                onIncrement = { viewModel.upgradeJet() },
                                onDecrement = { viewModel.downgradeJet() }
                            )
                        }
                    }
                    2 -> {
                        // Ataque / Defensa (Púas, Veneno, Electricidad, Blindaje)
                        item {
                            CounterPartItem(
                                title = "Púas Quitinosas",
                                description = "Espinaspuntiagudas. Infligen daño por embestida a objetivos y defienden contra mordiscos frontales.",
                                count = draft.spikesCount,
                                maxCount = 4,
                                cost = CellEvolutionConfig.COST_SPIKE,
                                canAfford = draft.dnaPoints >= CellEvolutionConfig.COST_SPIKE,
                                onIncrement = { viewModel.upgradeSpikes() },
                                onDecrement = { viewModel.downgradeSpikes() }
                            )
                        }
                        item {
                            TogglePartItem(
                                title = "Glándula de Veneno",
                                description = "Segrega un rastro tóxico verde que envenena y ralentiza a los perseguidores.",
                                isEnabled = draft.poisonGland,
                                cost = CellEvolutionConfig.COST_POISON,
                                canAfford = draft.dnaPoints >= CellEvolutionConfig.COST_POISON || draft.poisonGland,
                                onToggle = { viewModel.togglePoison() }
                            )
                        }
                        item {
                            TogglePartItem(
                                title = "Órgano Eléctrico",
                                description = "Nódulo bioluminiscente que descarga pulsos electromagnéticos defensivos para aturdir y repeler.",
                                isEnabled = draft.electricOrgan,
                                cost = CellEvolutionConfig.COST_ELECTRIC,
                                canAfford = draft.dnaPoints >= CellEvolutionConfig.COST_ELECTRIC || draft.electricOrgan,
                                onToggle = { viewModel.toggleElectric() }
                            )
                        }
                        item {
                            CounterPartItem(
                                title = "Placas de Blindaje",
                                description = "Membrana celular endurecida. Aumenta la salud y reduce el daño entrante, aunque añade peso.",
                                count = draft.armorPlates,
                                maxCount = 3,
                                cost = CellEvolutionConfig.COST_ARMOR,
                                canAfford = draft.dnaPoints >= CellEvolutionConfig.COST_ARMOR,
                                onIncrement = { viewModel.upgradeArmor() },
                                onDecrement = { viewModel.downgradeArmor() }
                            )
                        }
                    }
                    3 -> {
                        // Sentidos y Bioluminiscencia
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Órgano Sensorial (Ojo/Radar)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = when (draft.eyeType) {
                                            "COMPOUND" -> "Ojo Compuesto: Radar avanzado de largo alcance (900µm) con detección de amenazas cromática."
                                            "BASIC" -> "Mancha Ocular: Detección elemental de luz y sombras (650µm)."
                                            else -> "Sin Ojos: Campo de visión restringido (450µm)."
                                        },
                                        color = Color(0xFFB0BEC5),
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { viewModel.toggleEye() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("toggle_eye_button")
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Cambiar Ojo (${draft.eyeType})", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Pigmentación y Bioluminiscencia",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val colors = listOf(
                                        0xFF00E5FF to "Cian Primordial",
                                        0xFF76FF03 to "Verde Tóxico",
                                        0xFFFF5252 to "Rojo Carnívoro",
                                        0xFFFFD600 to "Dorado Mutágeno",
                                        0xFFD500F9 to "Púrpura Abisal"
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        colors.forEach { (hex, name) ->
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .background(Color(hex), CircleShape)
                                                    .border(
                                                        width = if (draft.primaryColorHex == hex) 3.dp else 1.dp,
                                                        color = if (draft.primaryColorHex == hex) Color.White else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { viewModel.setDraftColor(hex) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (draft.primaryColorHex == hex) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = name,
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    dnaPoints: Int,
    onBack: () -> Unit,
    onApply: () -> Unit
) {
    Surface(
        color = Color(0xFF071526),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_editor")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Laboratorio de ADN",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Current DNA
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF0B192C), RoundedCornerShape(12.dp))
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
                        text = "$dnaPoints ADN",
                        color = Color(0xFFFFD600),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("apply_mutations_button")
                ) {
                    Text("Nadar", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun StatSummarySection(stats: com.example.spore.game.engine.CellStats) {
    Surface(
        color = Color(0xFF071526),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatMetricItem(label = "Velocidad", value = "${stats.baseSpeed.toInt()}")
            StatMetricItem(label = "Agilidad", value = "${stats.turnRate.toInt()} rad/s")
            StatMetricItem(label = "Salud", value = "${stats.maxHealth.toInt()} HP")
            StatMetricItem(label = "Ataque", value = "${(stats.biteDamage + stats.spikeDamage).toInt()}")
            StatMetricItem(label = "Blindaje", value = "${(stats.armorDamageReduction * 100).toInt()}%")
        }
    }
}

@Composable
private fun StatMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
        Text(text = value, color = Color(0xFF80D8FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun MouthSelectorItem(
    title: String,
    description: String,
    isSelected: Boolean,
    cost: Int,
    canAfford: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF132B4F) else Color(0xFF0F1E36)
        ),
        shape = RoundedCornerShape(14.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("mouth_${title.take(6)}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, color = Color(0xFFB0BEC5), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (isSelected) {
                Surface(
                    color = Color(0xFF00E5FF),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Activo",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Surface(
                    color = if (canAfford) Color(0xFF1E3A5F) else Color(0xFF263238),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$cost ADN",
                        color = if (canAfford) Color(0xFFFFD600) else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CounterPartItem(
    title: String,
    description: String,
    count: Int,
    maxCount: Int,
    cost: Int,
    canAfford: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, color = Color(0xFFB0BEC5), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onDecrement,
                    enabled = count > 0,
                    modifier = Modifier
                        .size(34.dp)
                        .background(if (count > 0) Color(0xFF1E3A5F) else Color(0xFF263238), CircleShape)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Quitar", tint = Color.White, modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "$count / $maxCount",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                IconButton(
                    onClick = onIncrement,
                    enabled = count < maxCount && canAfford,
                    modifier = Modifier
                        .size(34.dp)
                        .background(if (count < maxCount && canAfford) Color(0xFF00E5FF) else Color(0xFF263238), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Añadir",
                        tint = if (count < maxCount && canAfford) Color.Black else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TogglePartItem(
    title: String,
    description: String,
    isEnabled: Boolean,
    cost: Int,
    canAfford: Boolean,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) Color(0xFF132B4F) else Color(0xFF0F1E36)
        ),
        shape = RoundedCornerShape(14.dp),
        border = if (isEnabled) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, color = Color(0xFFB0BEC5), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (isEnabled) {
                Surface(
                    color = Color(0xFF00E5FF),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Instalado",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Surface(
                    color = if (canAfford) Color(0xFF1E3A5F) else Color(0xFF263238),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$cost ADN",
                        color = if (canAfford) Color(0xFFFFD600) else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
