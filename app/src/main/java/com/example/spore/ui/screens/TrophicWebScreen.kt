package com.example.spore.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spore.data.model.TrophicSpeciesEntity
import com.example.spore.game.engine.TrophicTier
import com.example.spore.ui.viewmodel.AppScreen
import com.example.spore.ui.viewmodel.SporeViewModel

@Composable
fun TrophicWebScreen(
    viewModel: SporeViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.GAME)
    }

    val speciesList by viewModel.allSpecies.collectAsStateWithLifecycle()
    var selectedTierIndex by remember { mutableIntStateOf(0) } // 0: All, 1..5: Tiers

    val filteredSpecies = remember(speciesList, selectedTierIndex) {
        if (selectedTierIndex == 0) speciesList
        else speciesList.filter { it.trophicLevel == (selectedTierIndex - 1) }
    }

    Scaffold(
        containerColor = Color(0xFF040D1A),
        topBar = {
            Surface(
                color = Color(0xFF071526),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.GAME) },
                        modifier = Modifier.testTag("back_from_trophic_web")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Cadena Trófica y Bestiario",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Red ecológica del caldo primordial",
                            color = Color(0xFF80D8FF),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                TrophicPyramidExplainerCard()
            }

            item {
                // Tier filter tabs
                val filterTabs = listOf(
                    "Todos",
                    "0: Algas",
                    "1: Herbívoros",
                    "2: Omnívoros",
                    "3: Depredadores",
                    "4: Ápex"
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTierIndex,
                    containerColor = Color(0xFF071526),
                    contentColor = Color(0xFF00E5FF),
                    edgePadding = 0.dp
                ) {
                    filterTabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = selectedTierIndex == idx,
                            onClick = { selectedTierIndex = idx },
                            text = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTierIndex == idx) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            items(filteredSpecies, key = { it.speciesId }) { species ->
                SpeciesCodexCard(species = species)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TrophicPyramidExplainerCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B192C)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A5F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cómo Funciona la Cadena Trófica",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "En la sopa primordial, la energía fluye desde los productores fotosintéticos hasta los gigantes ápex:",
                color = Color(0xFFCFD8DC),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Visual Tier Ladder
            TrophicTierStepRow(tier = 4, name = "Súper Depredadores Ápex", desc = "Cazan cualquier célula menor. Inmunes a agresiones directas sin púas/veneno.", color = Color(0xFFD500F9))
            TrophicTierStepRow(tier = 3, name = "Depredadores Carnívoros", desc = "Mandíbulas y púas. Cazan activamente herbívoros y omnívoros.", color = Color(0xFFFF3D00))
            TrophicTierStepRow(tier = 2, name = "Consumidores Secundarios", desc = "Omnívoros oportunistas. Se alimentan de algas y restos orgánicos.", color = Color(0xFFFFD600))
            TrophicTierStepRow(tier = 1, name = "Consumidores Primarios", desc = "Herbívoros filtradores. Comen fitoplancton. Son presa de carnívoros.", color = Color(0xFF76FF03))
            TrophicTierStepRow(tier = 0, name = "Productores Primarios", desc = "Fitoplancton que realiza fotosíntesis. Generan la biomasa basal.", color = Color(0xFF00E676))
        }
    }
}

@Composable
private fun TrophicTierStepRow(tier: Int, name: String, desc: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = color.copy(alpha = 0.2f),
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, color),
            modifier = Modifier.width(70.dp)
        ) {
            Text(
                text = "Nivel $tier",
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(text = desc, color = Color(0xFF90A4AE), fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun SpeciesCodexCard(species: TrophicSpeciesEntity) {
    val tier = TrophicTier.fromLevel(species.trophicLevel)
    val tierColor = when (tier) {
        TrophicTier.APEX -> Color(0xFFD500F9)
        TrophicTier.PREDATOR -> Color(0xFFFF3D00)
        TrophicTier.SECONDARY_CONSUMER -> Color(0xFFFFD600)
        TrophicTier.PRIMARY_CONSUMER -> Color(0xFF76FF03)
        TrophicTier.PRODUCER -> Color(0xFF00E676)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (species.discovered) tierColor.copy(alpha = 0.5f) else Color(0xFF1E3A5F)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(tierColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (species.discovered) species.name else "??? Microbio No Identificado",
                        color = if (species.discovered) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Surface(
                    color = tierColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, tierColor)
                ) {
                    Text(
                        text = "Nivel ${species.trophicLevel}",
                        color = tierColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (species.discovered) {
                Text(
                    text = species.scientificName,
                    color = Color(0xFF80D8FF),
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = species.description,
                    color = Color(0xFFCFD8DC),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Trophic interactions
                Surface(
                    color = Color(0xFF071224),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Alimentación / Presas: ", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(species.preyDescription, color = Color(0xFFB0BEC5), fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Depredadores / Amenazas: ", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(species.predatorDescription, color = Color(0xFFB0BEC5), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Records
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Devorados por ti: ${species.timesEatenByPlayer}",
                        color = Color(0xFFFFD600),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Muertes causadas a ti: ${species.timesKilledPlayer}",
                        color = Color(0xFFFF5252),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Encuentra este organismo en el caldo primordial para desbloquear su ficha biológica y posición en la red.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
