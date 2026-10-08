package com.example.spore.cinematics

enum class CinematicCameraStage {
    ATMOSPHERE_ENTRY,       // Entrada planetaria y visión macro de la hidrosfera
    GEOLOGICAL_DESCENT,     // Descenso a los respiraderos, calderas, mares o cráteres
    CHEMICAL_REACTION,      // Catálisis inorgánica y síntesis de monómeros prebióticos
    MOLECULAR_ASSEMBLY,     // Autoensamblaje del Mundo de ARN y vesículas de bicapa lipídica
    CELLULAR_AWAKENING      // Encapsulación, primer gradiente electroquímico y nado celular
}

data class CinematicPhaseInfo(
    val stage: CinematicCameraStage,
    val phaseIndex: Int,
    val title: String,
    val scientificEpoch: String,
    val scientificHypothesis: String,
    val narration: String,
    val keyMolecules: List<String>,
    val durationSeconds: Float = 10f
)

data class PlanetCinematicDefinition(
    val planetId: String,
    val planetName: String,
    val oceanName: String,
    val scientificTitle: String,
    val primaryTheory: String,
    val realWorldScientists: String,
    val scientificSummary: String,
    val peerReviewedCitations: List<String>,
    val atmosphericComposition: String,
    val oceanChemistry: String,
    val energySource: String,
    val phases: List<CinematicPhaseInfo>
) {
    companion object {
        val CINEMATICS = mapOf(
            "planet_aqualis" to PlanetCinematicDefinition(
                planetId = "planet_aqualis",
                planetName = "Aqualis",
                oceanName = "Océano Cian Primordial",
                scientificTitle = "Génesis Hidrotermal Alcalina Submarina",
                primaryTheory = "Teoría del Respiradero Hidrotermal Alcalino & Serpentinización",
                realWorldScientists = "Michael J. Russell, William Martin y Nick Lane",
                scientificSummary = "En el lecho basáltico de Aqualis, aguas alcalinas ricas en H₂ y CH₄ colisionan con aguas marinas ácidas ricas en CO₂ y Fe²⁺. La precipitación espontánea de membranas inorgánicas microporosas de greigita (Fe₃S₄) y mackinawita crea celdas catalíticas donde los gradientes naturales de protones (fuerza protón-motriz) impulsan la fijación inorgánica de carbono y la polimerización del primer ARN.",
                peerReviewedCitations = listOf(
                    "Russell, M.J. & Hall, A.J. (1997) - The emergence of life from alkaline hydrothermal mounds",
                    "Lane, N. & Martin, W. (2012) - The origin of membrane bioenergetics. Cell 151(7)",
                    "Sojo, V., Herschy, B., Whicher, A. et al. (2016) - The Origin of Life in Alkaline Hydrothermal Vents"
                ),
                atmosphericComposition = "CO₂ (78%), N₂ (18%), Vapor de H₂O (3.5%), CH₄ trazas. Sin O₂ libre.",
                oceanChemistry = "Rico en Fe²⁺, Ni²⁺, pH ~5.8 (ligeramente ácido debido al CO₂ disuelto).",
                energySource = "Gradiente quimiosmótico natural (ΔpH ~4 unidades) y reducción de H₂.",
                phases = listOf(
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.ATMOSPHERE_ENTRY,
                        phaseIndex = 0,
                        title = "Entrada en la Hidrosfera Primigenia",
                        scientificEpoch = "Hace 4.100 Ma • Eón Arqueano Temprano",
                        scientificHypothesis = "Océano Global Anóxico",
                        narration = "Descendemos sobre Aqualis. Un océano colosal y anóxico cubre la corteza recién enfriada. Sin capa de ozono ni oxígeno libre, la atmósfera densa de dióxido de carbono retiene el calor planetario mientras el agua marina se filtra a kilómetros de profundidad en grietas tectónicas.",
                        keyMolecules = listOf("H₂O", "CO₂", "N₂", "Fe²⁺ disuelto"),
                        durationSeconds = 8.5f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.GEOLOGICAL_DESCENT,
                        phaseIndex = 1,
                        title = "La Fumarola Alcalina Blanca",
                        scientificEpoch = "Dorsal Oceánica Abisal • 3.000m de profundidad",
                        scientificHypothesis = "Serpentinización y Manantiales Hidrotermales",
                        narration = "En la oscuridad del abismo, la serpentinización del olivino en la corteza calienta fluidos hidrotermales alcalinos (pH ~10) cargados de gas hidrógeno molecular (H₂). Al emerger en el agua de mar ácida rica en hierro, se precipitan imponentes torres minerales porosas de greigita y sulfuro ferroso.",
                        keyMolecules = listOf("Mg₂SiO₄ + H₂O", "H₂ alcalino", "Greigita Fe₃S₄", "Mackinawita FeS"),
                        durationSeconds = 9f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.CHEMICAL_REACTION,
                        phaseIndex = 2,
                        title = "Catálisis en Microcavernas Minerales",
                        scientificEpoch = "Interfase Inorgánica • Escala Micrométrica",
                        scientificHypothesis = "Bioenergética del Gradiente Protón-Motriz Natural",
                        narration = "Las paredes de las microcavernas minerales actúan como catalizadores inorgánicos. Un gradiente natural de protones (ΔpH) a través de las finas paredes de sulfuro de hierro emula la membrana de las mitocondrias primitivas, transfiriendo electrones desde el H₂ para reducir el CO₂ disuelto en formiato y acetato.",
                        keyMolecules = listOf("ΔpH (+/- H⁺)", "HCOO⁻ (Formiato)", "CH₃COO⁻ (Acetato)", "Pirofosfato"),
                        durationSeconds = 10f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.MOLECULAR_ASSEMBLY,
                        phaseIndex = 3,
                        title = "El Mundo de ARN y Autoensamblaje Lipídico",
                        scientificEpoch = "Polimerización Prebiótica • Nanómetros",
                        scientificHypothesis = "Encapsulación Espontánea en Bicapa Lipídica",
                        narration = "Impulsados por el flujo de energía termodinámica, los nucleótidos se condensan en cadenas lineales de ARN con capacidad autorreplicante y catalítica (ribozimas). Simultáneamente, ácidos grasos anfipáticos se autoensamblan espontáneamente en vesículas esféricas de bicapa lipídica que sellan el contenido molecular.",
                        keyMolecules = listOf("Ribonucleótidos (A, U, C, G)", "Ribozimas", "Ácidos Grasos C₁₂-C₁₆", "Bicapa Lipídica"),
                        durationSeconds = 10.5f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.CELLULAR_AWAKENING,
                        phaseIndex = 4,
                        title = "Despertar de la Primera Protocélula",
                        scientificEpoch = "Génesis del LUCA • Primera Vida Libre",
                        scientificHypothesis = "Autonomía Celular y Motilidad Ciliar",
                        narration = "La membrana lipídica se sella por completo, independizándose del laberinto mineral. La protocélula ha nacido: mantiene su propia homeostasis iónica, su ARN codifica sus primeros péptidos protectores y sus cilios comienzan a ondular en las corrientes del océano de Aqualis.",
                        keyMolecules = listOf("Membrana Homeostática", "Código Genético Primitivo", "Cilios de Tubulina", "ATP Sintasa Proto"),
                        durationSeconds = 11f
                    )
                )
            ),

            "planet_rubrum" to PlanetCinematicDefinition(
                planetId = "planet_rubrum",
                planetName = "Rubrum IV",
                oceanName = "Mares Rojos de Sangre e Hierro",
                scientificTitle = "Mundo de Hierro-Sulfuro y Síntesis Atmosférica",
                primaryTheory = "Hipótesis del Mundo de Hierro-Sulfuro & Catálisis de Pirita",
                realWorldScientists = "Günter Wächtershäuser, Stanley Miller y Harold Urey",
                scientificSummary = "Bajo una atmósfera volcánica reductora azotada por tempestades eléctricas brutales, los gases atmosféricos se ionizan formando aminoácidos y tioésteres. En la superficie del mar rojo, rico en hematita y sales férricas, la síntesis de pirita (FeS + H₂S → FeS₂ + H₂ + 2e⁻) libera electrones exergónicos que blindan las protomembranas con complejos organometálicos y seleccionan protocélulas depredadoras de alta resistencia.",
                peerReviewedCitations = listOf(
                    "Wächtershäuser, G. (1988) - Before enzymes and templates: theory of surface metabolism. Microbiol. Rev. 52(4)",
                    "Wächtershäuser, G. (1990) - Evolution of the first metabolic cycles. PNAS 87(1)",
                    "Miller, S.L. (1953) - A production of amino acids under possible primitive earth conditions. Science 117"
                ),
                atmosphericComposition = "CH₄ (35%), NH₃ (25%), CO (15%), H₂O vapor (20%), H₂S (5%). Altamente reductora.",
                oceanChemistry = "Saturada de sales férricas (Fe³⁺, Fe²⁺), óxido de hierro coloidal rojizo y sulfuros.",
                energySource = "Descargas de relámpagos masivos y la reacción exergónica de pirita (ΔG° = -38.4 kJ/mol).",
                phases = listOf(
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.ATMOSPHERE_ENTRY,
                        phaseIndex = 0,
                        title = "Atmósfera Volcánica de Nubes Férricas",
                        scientificEpoch = "Hace 4.250 Ma • Hádico Tardío Hostil",
                        scientificHypothesis = "Atmósfera Reductora Volcánica",
                        narration = "Ingresamos en la tormentosa atmósfera de Rubrum IV. Cielos bermellones cargados de metano, amoníaco y vapor de azufre cubren mares hiper-ferruginosos teñidos de rojo por el óxido de hierro y sales minerales oxidadas.",
                        keyMolecules = listOf("CH₄", "NH₃", "H₂S", "Fe³⁺ / Fe₂O₃"),
                        durationSeconds = 8.5f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.GEOLOGICAL_DESCENT,
                        phaseIndex = 1,
                        title = "Tormentas Eléctricas Miller-Urey",
                        scientificEpoch = "Superficie Marina • Tempestad de Relámpagos",
                        scientificHypothesis = "Síntesis Prebiótica por Descarga Eléctrica de Plasma",
                        narration = "Megarrayos de millones de voltios surcan los cielos y golpean violentamente las olas rojas. El plasma de las descargas disocia los enlaces covalentes del metano y el amoníaco, sintetizando cianuro de hidrógeno y aldehídos que se condensan en aminoácidos prebióticos solubles.",
                        keyMolecules = listOf("Plasma Iónico", "HCN (Cianuro de H)", "HCHO (Formaldehído)", "Glicina / Alanina"),
                        durationSeconds = 9f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.CHEMICAL_REACTION,
                        phaseIndex = 2,
                        title = "Catálisis Superficial en Cristales de Pirita",
                        scientificEpoch = "Lecho Costero Basáltico • Reacción Wächtershäuser",
                        scientificHypothesis = "Metabolismo Superficial Quimiolitotrófico",
                        narration = "En los sedimentos submarinos de sulfuro ferroso, la reacción de formación de pirita (FeS + H₂S → FeS₂ + H₂ + 2e⁻) dona electrones fuertemente reductores. Sin necesidad de luz solar, los tioésteres activan la formación de los primeros oligopéptidos en una monocapa mineral bidimensional.",
                        keyMolecules = listOf("FeS (Troilita)", "FeS₂ (Pirita)", "Tioésteres R-S-CO-R'", "Oligopéptidos"),
                        durationSeconds = 10f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.MOLECULAR_ASSEMBLY,
                        phaseIndex = 3,
                        title = "Ensamblaje de la Membrana Queratinoide Férrica",
                        scientificEpoch = "Quimiocápsulas • Escala Nanométrica",
                        scientificHypothesis = "Microesferas Proteicas Queratoides de Fox & Wächtershäuser",
                        narration = "Los péptidos hidrofóbicos ricos en cisteína coordinan iones de hierro y azufre, polimerizándose en microesferas resistentes al calor y a la acidez. Se forma una protomembrana endurecida con placas minerales férricas que confiere una armadura estructural contra el entorno violento.",
                        keyMolecules = listOf("Poli-cisteína", "Centros [4Fe-4S]", "Membrana Peptidasa", "Proto-blindaje"),
                        durationSeconds = 10.5f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.CELLULAR_AWAKENING,
                        phaseIndex = 4,
                        title = "El Despertar del Depredador de Hierro",
                        scientificEpoch = "Brote de la Protocélula Carnívora • Primera Predación",
                        scientificHypothesis = "Desarrollo de Flagelos de Tracción y Quimiorreceptores de Iones",
                        narration = "La protocélula de Rubrum se dota de receptores bioeléctricos de iones metálicos y un flagelo contráctil robusto. Con un metabolismo heterótrofo agresivo orientado al consumo de biomasa prebiótica rica en energía, se lanza a cazar en las aguas rojas.",
                        keyMolecules = listOf("Receptores Fe-S", "Flagelo Quimio-motriz", "Músculo Proto-ciliar", "Organelo Depredador"),
                        durationSeconds = 11f
                    )
                )
            ),

            "planet_toxis" to PlanetCinematicDefinition(
                planetId = "planet_toxis",
                planetName = "Toxis Acidus",
                oceanName = "Sopa Ácida Esmeralda",
                scientificTitle = "Polimerización en Charcas Geotérmicas y Arcillas",
                primaryTheory = "Hipótesis de Charcas Geotérmicas Fluctuantes & Arcillas Montmorilloníticas",
                realWorldScientists = "Bruce Damer, David Deamer y James Ferris",
                scientificSummary = "En las calderas volcánicas de Toxis, charcas hidrotermales ácidas ricas en boro y ácido sulfúrico experimentan ciclos periódicos de hidratación y secado. Los minerales laminares de arcilla (montmorillonita) actúan como sustratos de alineación donde los nucleótidos se condensan en polímeros de ARN largos durante la evaporación, para luego ser encapsulados en vesículas lipídicas durante la rehidratación.",
                peerReviewedCitations = listOf(
                    "Damer, B. & Deamer, D. (2020) - The Hot Spring Hypothesis for an Origin of Life. Astrobiology 20(4)",
                    "Ferris, J.P. (2005) - Mineral catalysis and prebiotic synthesis: montmorillonite-catalyzed formation of RNA. Elements 1(3)",
                    "Mulkidjanian, A.Y. et al. (2012) - Origin of first cells at terrestrial, anoxic geothermal fields. PNAS 109(14)"
                ),
                atmosphericComposition = "CO₂ (60%), SO₂ (22%), H₂S (10%), Vapor de H₂O (8%). Densidad viscosa.",
                oceanChemistry = "pH extremadamente bajo (2.5 - 3.8), alta concentración de sulfatos, boro y sílice coloidal.",
                energySource = "Ciclos térmicos de evaporación-condensación y gradientes químicos geotérmicos.",
                phases = listOf(
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.ATMOSPHERE_ENTRY,
                        phaseIndex = 0,
                        title = "Descenso a la Caldera Esmeralda",
                        scientificEpoch = "Hace 3.900 Ma • Arqueano Volcánico",
                        scientificHypothesis = "Campo Geotérmico Ácido",
                        narration = "Nos sumergimos en las brumas sulfurosas de Toxis Acidus. Una caldera colosal alberga lagunas de aguas esmeralda cargadas de ácido sulfúrico diluido y sales de boro, alimentadas por fumarolas volcánicas continuas.",
                        keyMolecules = listOf("SO₂", "H₂SO₄ acuoso", "Borato B(OH)₄⁻", "Sílice SiO₂"),
                        durationSeconds = 8.5f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.GEOLOGICAL_DESCENT,
                        phaseIndex = 1,
                        title = "Estratos de Arcilla Montmorillonítica",
                        scientificEpoch = "Borde Costero de la Charca • Geotermia Activa",
                        scientificHypothesis = "Superficies Minerales Silicatadas como Matrices Catalíticas",
                        narration = "En los márgenes de la charca, la descomposición de ceniza volcánica crea capas de arcilla montmorillonita. Sus láminas microscópicas cargadas positivamente retienen monómeros de ribonucleótidos, alineándolos paralelamente a distancias de enlace covalente.",
                        keyMolecules = listOf("Filosilicatos Al₂Si₄O₁₀(OH)₂", "Cationes Mg²⁺ / Zn²⁺", "Mononucleótidos activados"),
                        durationSeconds = 9f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.CHEMICAL_REACTION,
                        phaseIndex = 2,
                        title = "Ciclo de Desecación: Polimerización de ARN",
                        scientificEpoch = "Interfase Evaporativa • Termodinámica de Condensación",
                        scientificHypothesis = "Condensación No-Enzimática Forzada por Pérdida de Agua",
                        narration = "El calor geotérmico evapora el agua de la charca. Al concentrarse en la película seca de arcilla, se eliminan moléculas de agua forzando la unión fosfodiéster entre azúcares ribosa y grupos fosfato. Se sintetizan largas cadenas de ARN estabilizadas por iones de boro.",
                        keyMolecules = listOf("Enlace Fosfodiéster 3'-5'", "Ribozimas Poliméricas", "Complejo Ribosa-Borato"),
                        durationSeconds = 10f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.MOLECULAR_ASSEMBLY,
                        phaseIndex = 3,
                        title = "Rehidratación y Trampa de Vesículas Lipídicas",
                        scientificEpoch = "Oleada Geotérmica • Formación Micelar",
                        scientificHypothesis = "Ciclos Seco-Húmedo para Encapsulación Masiva de Deamer",
                        narration = "Una nueva oleada de vapor rehidrata el lecho mineral. Los lípidos dispersos colapsan en millones de vesículas microscópicas. Las cadenas de ARN polimerizadas quedan atrapadas dentro de los compartimentos acuosos interiores, seleccionando las ribozimas más funcionales.",
                        keyMolecules = listOf("Vesículas Multilamelares", "ARN Encapsulado", "Fosfolípidos Primitivos"),
                        durationSeconds = 10.5f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.CELLULAR_AWAKENING,
                        phaseIndex = 4,
                        title = "Génesis de la Célula Ácida Extremófila",
                        scientificEpoch = "Emergencia de Vida Acidófila • Océano Esmeralda",
                        scientificHypothesis = "Bombeo Activo de Protones y Glándulas de Toxinas Defensivas",
                        narration = "La protocélula esmeralda consolida una bomba de protones interna para expulsar el exceso de acidez, desarrollando vacuolas de secreción química corrosiva. Se desprende de la arcilla y comienza a flotar en la sopa ácida de Toxis Acidus.",
                        keyMolecules = listOf("Bomba H⁺ Antiporte", "Glándula Tóxica", "Cilios de Membrana Ácida", "Clorofila Precursora"),
                        durationSeconds = 11f
                    )
                )
            ),

            "planet_ametistia" to PlanetCinematicDefinition(
                planetId = "planet_ametistia",
                planetName = "Ametistia Abisal",
                oceanName = "Mares de Metano Púrpura",
                scientificTitle = "Panspermia Cometaria y Azotosomas en Criomares",
                primaryTheory = "Bioquímica No Acuosa Criogénica & Bombardeo Cometario de Hidrocarburos",
                realWorldScientists = "James Stevenson, Jonathan Lunine y Chandra Wickramasinghe",
                scientificSummary = "En las profundidades gélidas de Ametistia (-175°C), un impacto cometario quiebra la corteza helada inyectando hidrocarburos aromáticos policíclicos (PAHs), cianuro y nucleobases extraterrestres. En el criomar de metano y etano líquido, moléculas polares de acrilonitrilo se autoensamblan espontáneamente en membranas flexibles llamadas 'azotosomas', impulsadas por criovulcanismo profundo y bioluminiscencia química.",
                peerReviewedCitations = listOf(
                    "Stevenson, J., Lunine, J. & Clancy, P. (2015) - Membrane alternatives in worlds without oxygen: Titan azotosomes. Science Advances 1(1)",
                    "Chyba, C. & Sagan, C. (1992) - Endogenous production, exogenous delivery and impact-shock synthesis of organic molecules. Nature 355",
                    "Rahm, M. et al. (2016) - Polymorphism and electronic structure of polyimine: prebiotic chemistry on Titan. PNAS 113(29)"
                ),
                atmosphericComposition = "N₂ (94%), CH₄ (5%), Ar (0.8%), C₂H₆ (0.2%). Presión criogénica de 1.8 atm.",
                oceanChemistry = "Metano (CH₄) y etano (C₂H₆) líquidos a -175°C, con trazas disueltas de acrilonitrilo y PAHs.",
                energySource = "Criovulcanismo de amoníaco-agua y quimioluminiscencia por radicales libres.",
                phases = listOf(
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.ATMOSPHERE_ENTRY,
                        phaseIndex = 0,
                        title = "El Cometa de Carbono Interestelar",
                        scientificEpoch = "Hace 3.800 Ma • Gran Bombardeo Cometario",
                        scientificHypothesis = "Panspermia Exógena y Entrega de Moléculas Complejas",
                        narration = "Un bólido cometario interestelar cruza la alta atmósfera de Ametistia Abisal. El cometa viaja cargado de hielo de amoníaco, polvo de grafito, aminoácidos extraterrestres y densos hidrocarburos policíclicos aromáticos sintetizados en el vacío interestelar.",
                        keyMolecules = listOf("PAHs Interestelares", "Acrilonitrilo C₂H₃N", "HCN Polímeros", "Aminoácidos D/L"),
                        durationSeconds = 8.5f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.GEOLOGICAL_DESCENT,
                        phaseIndex = 1,
                        title = "Impacto Criogénico y Fumarola Abisal",
                        scientificEpoch = "Superficie de Hidrocarburos • -175°C",
                        scientificHypothesis = "Fractura de la Corteza y Criovulcanismo Térmico",
                        narration = "El impacto fractura la corteza helada del planeta, hundiendo fragmentos orgánicos en el océano abisal de metano violeta. Desde las fallas tectónicas emergen plumas criovolcánicas calientes de agua-amoníaco que generan corrientes ascendentes iluminadas por quimioluminiscencia fósforo-violeta.",
                        keyMolecules = listOf("CH₄ líquido", "C₂H₆ líquido", "NH₃·H₂O criogénico", "Fosforescencia Violeta"),
                        durationSeconds = 9f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.CHEMICAL_REACTION,
                        phaseIndex = 2,
                        title = "Autoensamblaje del Azotosoma de Nitrógeno",
                        scientificEpoch = "Microescala Criogénica • Termodinámica No Acuosa",
                        scientificHypothesis = "Membranas de Acrilonitrilo Flexibles a -180°C (Stevenson et al.)",
                        narration = "En un solvente apolar como el metano líquido, los fosfolípidos comunes se congelarían rígidamente. En su lugar, moléculas de acrilonitrilo (C₂H₃N) orientan sus extremos polares de nitrógeno hacia el interior, formando vesículas ultrarresistentes y elásticas llamadas azotosomas.",
                        keyMolecules = listOf("Acrilonitrilo C₂H₃N", "Bicapa Invertida de Nitrógeno", "Poliiminas (HCN)ₓ", "Azotosoma"),
                        durationSeconds = 10f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.MOLECULAR_ASSEMBLY,
                        phaseIndex = 3,
                        title = "Crio-Polímeros y Canales Fosforescentes",
                        scientificEpoch = "Nanomaquinaria Molecular Criogénica",
                        scientificHypothesis = "Polímeros de Poliimina como Conductores Electrónicos",
                        narration = "Dentro del azotosoma, cadenas poliméricas de imina absorben la luz difusa de la bioluminiscencia abisal. La energía se transporta mediante resonancia cuántica a lo largo de los enlaces conjugados, creando la primera maquinaria metabólica adaptada a temperaturas bajo cero.",
                        keyMolecules = listOf("Cadenas Conjugadas C=N", "Luciferina Abisal", "Canales Iónicos de Litio", "Crio-ATP Análogo"),
                        durationSeconds = 10.5f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.CELLULAR_AWAKENING,
                        phaseIndex = 4,
                        title = "Nacimiento del Leviatán Microscópico",
                        scientificEpoch = "Evolución Abisal • Dominio del Océano Violeta",
                        scientificHypothesis = "Bioluminiscencia Activa y Propulsión a Chorro Hidrocarburo",
                        narration = "La protocélula de Ametistia se enciende con pulsos de bioluminiscencia violeta. Equipada con hidrojets que eyectan metano líquido y sensores fotoquímicos supersensibles, nada majestuosamente en el abismo oscuro del criomundo.",
                        keyMolecules = listOf("Bioluminiscencia Luciferasa", "Hidrojet de Metano", "Membrana Azotosómica", "Célula Ápex Primitiva"),
                        durationSeconds = 11f
                    )
                )
            ),

            "planet_solaria" to PlanetCinematicDefinition(
                planetId = "planet_solaria",
                planetName = "Solaria Prime",
                oceanName = "Mares Solares de Azufre Dorado",
                scientificTitle = "Fotoquímica Ultravioleta y Síntesis Sutherland",
                primaryTheory = "Síntesis Prebiótica Fotoquímica por Radiación UV Solar en Sulfito",
                realWorldScientists = "John Sutherland, Matthew Powner y Jack Szostak",
                scientificSummary = "Bajo el bombardeo incesante de fotones ultravioleta emitidos por un sistema estelar binario, lagunas someras ricas en cianamida, gliceraldehído y sales de sulfito experimentan fotoquímica de alta energía. Los fotones UV destruyen selectivamente los subproductos químicos caóticos e inestables, mientras que catalizan la síntesis estereoespecífica de ribonucleótidos purínicos y pirimidínicos en un caldo ámbar fotoprotegido.",
                peerReviewedCitations = listOf(
                    "Powner, M.W., Gerland, B. & Sutherland, J.D. (2009) - Synthesis of activated pyrimidine ribonucleotides in prebiotically plausible conditions. Nature 459",
                    "Patel, B.H. et al. (2015) - Common origins of RNA, protein and lipid precursors in a cyanosulfidic protometabolism. Nature Chemistry 7",
                    "Ranjan, S. & Sasselov, D.D. (2016) - Photochemically driven prebiotic chemistry on early Earth. Astrobiology 16(1)"
                ),
                atmosphericComposition = "CO₂ (52%), N₂ (36%), CO (8%), SO₂ (4%). Flujo UV masivo en longitudes de onda de 200-280 nm.",
                oceanChemistry = "Aguas someras alcalinas ricas en sales de sulfito (SO₃²⁻), cianuro y fosfato inorgánico condensado.",
                energySource = "Radiación estelar ultravioleta de vacío (UV-C) y energía de fotones de estrella binaria.",
                phases = listOf(
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.ATMOSPHERE_ENTRY,
                        phaseIndex = 0,
                        title = "La Corona de la Estrella Doble",
                        scientificEpoch = "Hace 4.050 Ma • Arqueano Fotónico",
                        scientificHypothesis = "Radiación Ultravioleta Estelar Extrema",
                        narration = "Aproximación a Solaria Prime. El planeta orbita en la zona habitable de dos soles fulgurantes cuyas llamaradas y vientos estelares bañan la atmósfera en un torrente incesante de radiación ultravioleta de alta energía.",
                        keyMolecules = listOf("Fotones UV-C (λ 254 nm)", "Cianamida H₂N-CN", "Sulfito SO₃²⁻", "Fosfato Pi"),
                        durationSeconds = 8.5f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.GEOLOGICAL_DESCENT,
                        phaseIndex = 1,
                        title = "Lagunas Someras de Azufre Dorado",
                        scientificEpoch = "Costa Lacustre Evaporativa • Aguas Cálidas",
                        scientificHypothesis = "Cuencas Cianosulfídicas Someras (Patel & Sutherland)",
                        narration = "Descendemos sobre amplias lagunas litorales de aguas poco profundas teñidas de amarillo brillante por el azufre coloidal. La luz solar penetra hasta el fondo rocoso, excitando las soluciones químicas de cianuro de hidrógeno y iones sulfito.",
                        keyMolecules = listOf("Cianuro de Hidrógeno HCN", "2-Aminooxazol", "Gliceraldehído", "Azufre Coloidal S₈"),
                        durationSeconds = 9f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.CHEMICAL_REACTION,
                        phaseIndex = 2,
                        title = "Fotólisis y Selección Fotoquímica UV",
                        scientificEpoch = "Catálisis Fotónica • Reacción Sutherland",
                        scientificHypothesis = "Supervivencia de los Ribonucleótidos por Disipación No-Radiativa de UV",
                        narration = "Los fotones ultravioletas rompen los enlaces de moléculas intermedias caóticas. Sin embargo, los nucleótidos de citosina y uracilo poseen una estructura aromática única que disipa el exceso de energía lumínica casi instantáneamente como calor, sobreviviendo intactos a la radiación que destruye a otros compuestos.",
                        keyMolecules = listOf("Ribocitidina (C)", "Ribouridina (U)", "Fotones Absorbidos", "Enlace Glicosídico β"),
                        durationSeconds = 10f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.MOLECULAR_ASSEMBLY,
                        phaseIndex = 3,
                        title = "Condensación de Coacervados Fotoprotectores",
                        scientificEpoch = "Gotas Coacervadas • Nanomundo Solar",
                        scientificHypothesis = "Gotículas de Separación de Fases Líquido-Líquido con Pigmentos Carotenoides",
                        narration = "Los ácidos grasos y pigmentos carotenoides de azufre forman microgotas de coacervado mediante separación de fases. Estos coacervados actúan como pantallas solares naturales, canalizando la energía fotónica para fosforilar nucleótidos y duplicar cadenas de ARN a alta velocidad.",
                        keyMolecules = listOf("Gotículas Coacervadas", "Carotenoides Fotoactivos", "Fotofosforilación", "Polímeros de ARN Solar"),
                        durationSeconds = 10.5f
                    ),
                    CinematicPhaseInfo(
                        stage = CinematicCameraStage.CELLULAR_AWAKENING,
                        phaseIndex = 4,
                        title = "Génesis de la Protocélula Solar Fototrófica",
                        scientificEpoch = "Brote de Vida Fotosintética • Amanecer Dorado",
                        scientificHypothesis = "Fototaxis Positiva y Metabolismo Fotovoltaico de Membrana",
                        narration = "La protocélula solar despierta con una mancha ocular fotosensible primordial y cilios fototácticos. Orientándose hacia el resplandor de los dos soles, comienza a transformar la luz estelar en energía metabólica pura.",
                        keyMolecules = listOf("Mancha Ocular Fotorreceptora", "Rodopsina Primitiva", "Cilios Fototácticos", "Célula Radiante Dorada"),
                        durationSeconds = 11f
                    )
                )
            )
        )

        fun getCinematicForPlanet(planetId: String): PlanetCinematicDefinition {
            return CINEMATICS[planetId] ?: CINEMATICS["planet_aqualis"]!!
        }
    }
}
