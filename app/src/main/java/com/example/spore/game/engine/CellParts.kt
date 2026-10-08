package com.example.spore.game.engine

import com.example.spore.data.model.CellEvolutionEntity

enum class PartCategory(val title: String) {
    DIET("Bocas y Dieta"),
    LOCOMOTION("Locomoción Hidrodinámica"),
    COMBAT("Ataque y Toxinas"),
    DEFENSE("Blindaje y Masa"),
    SENSORY("Sentidos y Percepción")
}

data class ChimericTrait(
    val id: String,
    val name: String,
    val shortBonus: String,
    val description: String
)

data class GamePart(
    val id: String,
    val name: String,
    val category: PartCategory,
    val description: String,
    val discoveryClue: String,
    val dnaCost: Int,
    val defaultUnlocked: Boolean = false,
    val traitA: ChimericTrait,
    val traitB: ChimericTrait
)

data class CellStats(
    val maxHealth: Float,
    val baseSpeed: Float,
    val turnRate: Float, // rad per second
    val biteDamage: Float,
    val spikeDamage: Float,
    val armorDamageReduction: Float, // percentage
    val sensorRadius: Float,
    val dashSpeedMultiplier: Float,
    val hasPoison: Boolean,
    val hasElectricShock: Boolean,
    val trophicTier: TrophicTier,
    val dietType: DietType,
    // Epigenetic Chimeric Trait active flags
    val activeChimericTraits: Set<String> = emptySet(),
    val hasVampiricBite: Boolean = false,
    val hasCriticalBite: Boolean = false,
    val hasFoodMagnet: Boolean = false,
    val hasEchoSonar: Boolean = false,
    val hasPhotosynthesis: Boolean = false,
    val hasReflectiveSpikes: Boolean = false,
    val hasShockwaveDash: Boolean = false,
    val hasParalyzingPoison: Boolean = false,
    val hasBioMagneticShock: Boolean = false
)

object CellEvolutionConfig {
    // Costs in DNA
    const val COST_MOUTH_CHANGE = 20
    const val COST_FLAGELLUM = 15
    const val COST_CILIA = 15
    const val COST_JET = 30
    const val COST_SPIKE = 20
    const val COST_POISON = 35
    const val COST_ELECTRIC = 45
    const val COST_ARMOR = 25
    const val COST_EYE_UPGRADE = 25

    // Default Unlocked Parts
    const val DEFAULT_UNLOCKED_PARTS = "MOUTH_HERBIVORE,FLAGELLA,CILIA,EYE_BASIC"

    val ALL_PARTS: List<GamePart> = listOf(
        GamePart(
            id = "MOUTH_HERBIVORE",
            name = "Filtro Ciliado Herbívoro",
            category = PartCategory.DIET,
            description = "Aparato bucal adaptado para filtrar y succionar microalgas fotosintéticas con alta digestibilidad celular.",
            discoveryClue = "Genoma primario disponible desde el origen del caldo primordial.",
            dnaCost = COST_MOUTH_CHANGE,
            defaultUnlocked = true,
            traitA = ChimericTrait(
                id = "TRAIT_PHOTOSYNTHESIS",
                name = "Cloroplasto Simbiótico",
                shortBonus = "+ADN pasivo en blooms",
                description = "Sintetiza ADN extra continuamente al nadar dentro de zonas verdes de floración de fitoplancton."
            ),
            traitB = ChimericTrait(
                id = "TRAIT_HERBI_REGEN",
                name = "Enzima Curativa",
                shortBonus = "+50% Curación con algas",
                description = "Metaboliza las paredes de celulosa acelerando la regeneración celular al alimentarse."
            )
        ),
        GamePart(
            id = "MOUTH_CARNIVORE",
            name = "Mandíbula Serrada Carnívora",
            category = PartCategory.DIET,
            description = "Fauces queratinizadas con dentina punzante diseñadas para desgarrar la membrana de presas microscópicas.",
            discoveryClue = "Asimila cazando al Didinium Cazador o rompe un Meteorito de Diente Rojo.",
            dnaCost = COST_MOUTH_CHANGE,
            defaultUnlocked = false,
            traitA = ChimericTrait(
                id = "TRAIT_VAMPIRIC_BITE",
                name = "Canal Capilar Vampírico",
                shortBonus = "30% Robo de vida",
                description = "Absorbe directamente el 30% del daño infligido a presas como salud para tu membrana."
            ),
            traitB = ChimericTrait(
                id = "TRAIT_CRITICAL_BITE",
                name = "Dentina de Titanio",
                shortBonus = "35% Mordisco Crítico (x2 daño)",
                description = "Probabilidad de infligir un desgarre crítico que duplica el daño del mordisco."
            )
        ),
        GamePart(
            id = "MOUTH_OMNIVORE",
            name = "Probóscide Quimiosintética",
            category = PartCategory.DIET,
            description = "Trompa elástica multiuso capaz de ingerir tanto microalgas como fragmentos de carne de células caídas.",
            discoveryClue = "Asimila de la Ameba Oportunista o extrae de un Meteorito Orgánico Ámbar.",
            dnaCost = COST_MOUTH_CHANGE,
            defaultUnlocked = false,
            traitA = ChimericTrait(
                id = "TRAIT_HYBRID_METABOLISM",
                name = "Metabolismo Híbrido",
                shortBonus = "+50% Biomasa de alimentos",
                description = "Aprovecha al máximo cualquier compuesto orgánico acelerando el crecimiento celular."
            ),
            traitB = ChimericTrait(
                id = "TRAIT_VACUUM_PROBOSCIS",
                name = "Succión al Vacío",
                shortBonus = "+60% Alcance de captura",
                description = "Atrae y absorbe partículas alimenticias desde mayor distancia sin necesidad de contacto exacto."
            )
        ),
        GamePart(
            id = "FLAGELLA",
            name = "Flagelo Propulsor",
            category = PartCategory.LOCOMOTION,
            description = "Látigo proteico helicoidal que impulsa a la célula a través del fluido con empuje hidrodinámico constante.",
            discoveryClue = "Genoma locomotor primario disponible desde el origen.",
            dnaCost = COST_FLAGELLUM,
            defaultUnlocked = true,
            traitA = ChimericTrait(
                id = "TRAIT_CRUISE_PROPULSION",
                name = "Crucero Fluido",
                shortBonus = "+25% Velocidad base constante",
                description = "Optimiza la hidrodinámica reduciendo la fricción viscosa del agua."
            ),
            traitB = ChimericTrait(
                id = "TRAIT_TURBO_ACTIN",
                name = "Fibras de Actina Rápida",
                shortBonus = "-40% Tiempo de recarga de turbo",
                description = "Permite realizar aceleraciones de escape y persecución con mucha mayor frecuencia."
            )
        ),
        GamePart(
            id = "CILIA",
            name = "Corona de Cilios",
            category = PartCategory.LOCOMOTION,
            description = "Cientos de filamentos microscópicos rítmicos que otorgan giros de precisión y maniobrabilidad inmediata.",
            discoveryClue = "Genoma locomotor primario disponible desde el origen.",
            dnaCost = COST_CILIA,
            defaultUnlocked = true,
            traitA = ChimericTrait(
                id = "TRAIT_FOOD_MAGNET",
                name = "Vórtice Ciliar Magnético",
                shortBonus = "Atrae nutrientes cercanos",
                description = "Genera un micro-remolino que atrae activamente partículas de comida y hebras de ADN hacia ti."
            ),
            traitB = ChimericTrait(
                id = "TRAIT_INSTANT_TURN",
                name = "Giro Reflejo Instantáneo",
                shortBonus = "+100% Velocidad de rotación",
                description = "Permite maniobras evasivas ultra-rápidas para esquivar depredadores en el último instante."
            )
        ),
        GamePart(
            id = "JET",
            name = "Propulsor Hidráulico Jet",
            category = PartCategory.LOCOMOTION,
            description = "Sifón de eyección muscular que expulsa chorros de agua a presión para una aceleración explosiva.",
            discoveryClue = "Asimila cazando al Didinium Cazador o rompe un Meteorito Hidrostático.",
            dnaCost = COST_JET,
            defaultUnlocked = false,
            traitA = ChimericTrait(
                id = "TRAIT_SHOCKWAVE_DASH",
                name = "Pulso de Cavitación",
                shortBonus = "Onda aturdidora trasera",
                description = "Al hacer turbo, libera una onda de choque hacia atrás que daña y frena a los perseguidores."
            ),
            traitB = ChimericTrait(
                id = "TRAIT_HYDRAULIC_SURGE",
                name = "Sobrecarga Hidráulica",
                shortBonus = "+50% Distancia e impunidad",
                description = "El turbo recorre una distancia descomunal atravesando campos de veneno sin freno."
            )
        ),
        GamePart(
            id = "SPIKES",
            name = "Púas de Quitina",
            category = PartCategory.COMBAT,
            description = "Espolones endurecidos que sobresalen de la membrana. Dañan gravemente a cualquier célula al colisionar.",
            discoveryClue = "Asimila cazando a la Mordaza Puntiaguda o rompe un Meteorito Espinoso.",
            dnaCost = COST_SPIKE,
            defaultUnlocked = false,
            traitA = ChimericTrait(
                id = "TRAIT_REFLECTIVE_SPIKES",
                name = "Quitina Reflectora",
                shortBonus = "Devuelve 50% de daño recibido",
                description = "Cualquier depredador que ose embestirte recibe de vuelta la mitad del daño que te cause."
            ),
            traitB = ChimericTrait(
                id = "TRAIT_DEMOLITION_RAM",
                name = "Púa Perforante Rompe-Rocas",
                shortBonus = "Destroza meteoritos x3 rápido",
                description = "Multiplica el daño contra la coraza mineral de meteoritos y contra células con blindaje."
            )
        ),
        GamePart(
            id = "POISON",
            name = "Glándula de Toxina Abisal",
            category = PartCategory.COMBAT,
            description = "Vesícula secretora que deja una estela de ácido citotóxico capaz de disolver membranas rivales.",
            discoveryClue = "Asimila cazando al colosal Leviatán Abisal o extrae de un Meteorito de Azufre Tóxico.",
            dnaCost = COST_POISON,
            defaultUnlocked = false,
            traitA = ChimericTrait(
                id = "TRAIT_PARALYZING_POISON",
                name = "Neurotoxina Paralizante",
                shortBonus = "Ralentiza 50% a víctimas",
                description = "Las víctimas que tocan la nube ácida pierden la mitad de su velocidad y quedan a tu merced."
            ),
            traitB = ChimericTrait(
                id = "TRAIT_PERSISTENT_MIST",
                name = "Bruma Ácida Persistente",
                shortBonus = "Nube el doble de duradera y grande",
                description = "El charco de veneno permanece activo durante 8 segundos cubriendo una gran área de bloqueo."
            )
        ),
        GamePart(
            id = "ELECTRIC",
            name = "Órgano Bio-Eléctrico",
            category = PartCategory.COMBAT,
            description = "Electrocitos en serie que acumulan carga osmótica y liberan una descarga radial aturdidora.",
            discoveryClue = "Asimila en las profundidades de Ametistia o de un Meteorito de Plasma.",
            dnaCost = COST_ELECTRIC,
            defaultUnlocked = false,
            traitA = ChimericTrait(
                id = "TRAIT_BIOMAGNETIC_SHOCK",
                name = "Campo Bio-Magnético",
                shortBonus = "Descarga atrae todo el ADN",
                description = "La descarga radial imanta instantáneamente todo el ADN y nutrientes de la zona hacia ti."
            ),
            traitB = ChimericTrait(
                id = "TRAIT_CHAIN_LIGHTNING",
                name = "Sobrecarga Voltaica",
                shortBonus = "+80% Radio y aturdimiento 1.5s",
                description = "Expande un relámpago masivo que deja paralizados a los depredadores por segundo y medio."
            )
        ),
        GamePart(
            id = "ARMOR",
            name = "Placas de Blindaje de Sílice",
            category = PartCategory.DEFENSE,
            description = "Capas endurecidas de biomineral que protegen la membrana contra mordiscos y colisiones.",
            discoveryClue = "Asimila de la Mordaza Puntiaguda o de un Meteorito Silíceo.",
            dnaCost = COST_ARMOR,
            defaultUnlocked = false,
            traitA = ChimericTrait(
                id = "TRAIT_GRANITE_PLATES",
                name = "Placas de Granito Biológico",
                shortBonus = "+15% Reducción daño adicional",
                description = "Refuerza la corteza celular reduciendo drásticamente el impacto de mordiscos rivales."
            ),
            traitB = ChimericTrait(
                id = "TRAIT_EXO_MEMBRANE",
                name = "Membrana Exo-Elástica",
                shortBonus = "+75 Salud Máxima total",
                description = "Aumenta la capacidad volumétrica celular y la resistencia vital global."
            )
        ),
        GamePart(
            id = "EYE_COMPOUND",
            name = "Ojo Compuesto Panorámico",
            category = PartCategory.SENSORY,
            description = "Mosaico de omatidios fotosensibles que amplían el campo de visión y la percepción de amenazas.",
            discoveryClue = "Asimila de depredadores superiores o rompe una Geoda Cósmica de Cristal.",
            dnaCost = COST_EYE_UPGRADE,
            defaultUnlocked = false,
            traitA = ChimericTrait(
                id = "TRAIT_ECHO_SONAR",
                name = "Ocelo de Eco-Resonancia",
                shortBonus = "Radar de meteoritos y parejas",
                description = "Revela en la pantalla la dirección de meteoritos con partes y de parejas simbióticas."
            ),
            traitB = ChimericTrait(
                id = "TRAIT_PANORAMIC_LENS",
                name = "Lente Ultra-Panorámica",
                shortBonus = "Campo sensorial de 1000px",
                description = "Otorga una visión periférica omnidireccional para anticipar emboscadas."
            )
        )
    )

    fun getPart(partId: String): GamePart? = ALL_PARTS.firstOrNull { it.id == partId }

    fun parseUnlockedParts(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) {
            return DEFAULT_UNLOCKED_PARTS.split(",").map { it.trim() }.toSet()
        }
        val set = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        // Always include basic parts
        DEFAULT_UNLOCKED_PARTS.split(",").forEach { set.add(it.trim()) }
        return set
    }

    fun formatUnlockedParts(parts: Set<String>): String {
        return parts.joinToString(",")
    }

    fun parseChimericTraits(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        raw.split(";").forEach { pair ->
            val tokens = pair.split(":")
            if (tokens.size == 2) {
                map[tokens[0].trim()] = tokens[1].trim()
            }
        }
        return map
    }

    fun formatChimericTraits(traits: Map<String, String>): String {
        return traits.entries.joinToString(";") { "${it.key}:${it.value}" }
    }

    fun isPartUnlocked(partId: String, unlockedPartsRaw: String?): Boolean {
        val parts = parseUnlockedParts(unlockedPartsRaw)
        return parts.contains(partId)
    }

    fun calculateStats(entity: CellEvolutionEntity): CellStats {
        val diet = DietType.fromString(entity.mouthType)
        val traitsMap = parseChimericTraits(entity.chimericTraits)
        val activeTraitIds = traitsMap.values.toSet()

        // Base HP math + Chimeric perks
        val extraHpTrait = if (activeTraitIds.contains("TRAIT_EXO_MEMBRANE")) 75f else 0f
        val baseHp = 100f + (entity.armorPlates * 35f) + (entity.biomass * 1.2f) + extraHpTrait

        // Speed math + Chimeric perks
        val speedPenalty = entity.armorPlates * 15f
        val cruiseBonus = if (activeTraitIds.contains("TRAIT_CRUISE_PROPULSION")) 45f else 0f
        val baseSpeed = (180f + (entity.flagellaCount * 65f) - speedPenalty + cruiseBonus).coerceAtLeast(120f)
        
        // Turn rate math
        val turnRateMultiplier = if (activeTraitIds.contains("TRAIT_INSTANT_TURN")) 2.0f else 1.0f
        val turnRate = ((3.0f + (entity.ciliaCount * 1.8f)) * turnRateMultiplier).coerceAtLeast(2.5f)

        // Combat math + Chimeric perks
        val baseBiteDmg = when (diet) {
            DietType.HERBIVORE -> 5f
            DietType.OMNIVORE -> 18f
            DietType.CARNIVORE -> 35f
        }
        val spikeDmg = entity.spikesCount * 25f

        // Armor reduction math
        val graniteBonus = if (activeTraitIds.contains("TRAIT_GRANITE_PLATES")) 0.15f else 0.0f
        val armorReduction = ((entity.armorPlates * 0.12f) + graniteBonus).coerceAtMost(0.60f)

        // Sensory math
        val sensorRange = when {
            activeTraitIds.contains("TRAIT_PANORAMIC_LENS") -> 1050f
            entity.eyeType == "COMPOUND" -> 900f
            entity.eyeType == "BASIC" -> 650f
            else -> 450f
        }

        // Trophic classification
        val tier = when (diet) {
            DietType.HERBIVORE -> TrophicTier.PRIMARY_CONSUMER
            DietType.OMNIVORE -> {
                if (entity.biomass >= 80f && (entity.spikesCount >= 2 || entity.poisonGland)) {
                    TrophicTier.PREDATOR
                } else {
                    TrophicTier.SECONDARY_CONSUMER
                }
            }
            DietType.CARNIVORE -> {
                if (entity.biomass >= 100f && (entity.spikesCount >= 2 || entity.electricOrgan)) {
                    TrophicTier.APEX
                } else {
                    TrophicTier.PREDATOR
                }
            }
        }

        val baseDashMult = if (entity.jetCount > 0) 2.2f + (entity.jetCount * 0.4f) else 1.6f
        val dashMultiplier = if (activeTraitIds.contains("TRAIT_HYDRAULIC_SURGE")) baseDashMult * 1.35f else baseDashMult

        return CellStats(
            maxHealth = baseHp,
            baseSpeed = baseSpeed,
            turnRate = turnRate,
            biteDamage = baseBiteDmg,
            spikeDamage = spikeDmg,
            armorDamageReduction = armorReduction,
            sensorRadius = sensorRange,
            dashSpeedMultiplier = dashMultiplier,
            hasPoison = entity.poisonGland,
            hasElectricShock = entity.electricOrgan,
            trophicTier = tier,
            dietType = diet,
            activeChimericTraits = activeTraitIds,
            hasVampiricBite = activeTraitIds.contains("TRAIT_VAMPIRIC_BITE"),
            hasCriticalBite = activeTraitIds.contains("TRAIT_CRITICAL_BITE"),
            hasFoodMagnet = activeTraitIds.contains("TRAIT_FOOD_MAGNET"),
            hasEchoSonar = activeTraitIds.contains("TRAIT_ECHO_SONAR"),
            hasPhotosynthesis = activeTraitIds.contains("TRAIT_PHOTOSYNTHESIS"),
            hasReflectiveSpikes = activeTraitIds.contains("TRAIT_REFLECTIVE_SPIKES"),
            hasShockwaveDash = activeTraitIds.contains("TRAIT_SHOCKWAVE_DASH"),
            hasParalyzingPoison = activeTraitIds.contains("TRAIT_PARALYZING_POISON"),
            hasBioMagneticShock = activeTraitIds.contains("TRAIT_BIOMAGNETIC_SHOCK")
        )
    }
}
