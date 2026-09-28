package com.example.spore.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.spore.data.model.CellEvolutionEntity
import com.example.spore.data.model.GameStatsEntity
import com.example.spore.data.model.TrophicSpeciesEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CellEvolutionEntity::class,
        TrophicSpeciesEntity::class,
        GameStatsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SporeDatabase : RoomDatabase() {
    abstract fun sporeDao(): SporeDao

    companion object {
        @Volatile
        private var INSTANCE: SporeDatabase? = null

        fun getInstance(context: Context): SporeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SporeDatabase::class.java,
                    "spore_primordial.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).sporeDao()
                            dao.saveCellEvolution(CellEvolutionEntity())
                            dao.saveGameStats(GameStatsEntity())
                            dao.insertDefaultSpecies(DEFAULT_TROPHIC_SPECIES)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_TROPHIC_SPECIES = listOf(
            TrophicSpeciesEntity(
                speciesId = "phyto_spirulina",
                name = "Espiri-Alga",
                scientificName = "Spirulina Chloroplastica",
                trophicLevel = 0,
                diet = "PRODUCER",
                description = "Microalga fotosintética que convierte la luz solar y minerales en clorofila y biomasa. Es la base de toda la red trófica.",
                preyDescription = "Nutrientes minerales y fotones.",
                predatorDescription = "Devorada por herbívoros y omnívoros.",
                discovered = true
            ),
            TrophicSpeciesEntity(
                speciesId = "phyto_cristal",
                name = "Diatomea Cristalina",
                scientificName = "Silicodiadema Primoris",
                trophicLevel = 0,
                diet = "PRODUCER",
                description = "Fitoplancton con pared de sílice que flota a la deriva. Al romperse libera ricos fragmentos de ADN condensado.",
                preyDescription = "Sílice disuelto y luz.",
                predatorDescription = "Células filtradoras herbívoras.",
                discovered = true
            ),
            TrophicSpeciesEntity(
                speciesId = "herbivore_ciliado",
                name = "Ciliado Pacifista",
                scientificName = "Paramecium Herba",
                trophicLevel = 1,
                diet = "HERBIVORE",
                description = "Consumidor primario que se desplaza mediante cientos de cilios rítmicos. Su boca ciliada succiona fitoplancton de manera incansable.",
                preyDescription = "Algas unicelulares y bacterias fotosintéticas.",
                predatorDescription = "Células omnívoras, cazadores y carnívoros.",
                discovered = true
            ),
            TrophicSpeciesEntity(
                speciesId = "herbivore_rotifer",
                name = "Rotífero Veloz",
                scientificName = "Rotatoria Agilis",
                trophicLevel = 1,
                diet = "HERBIVORE",
                description = "Herbívoro veloz con dos coronas de cilios que actúan como turbinas. Si detecta vibraciones de depredadores, huye a gran velocidad.",
                preyDescription = "Microalgas flotantes.",
                predatorDescription = "Carnívoros activos y emboscadores.",
                discovered = false
            ),
            TrophicSpeciesEntity(
                speciesId = "omnivore_amoeba",
                name = "Ameba Oportunista",
                scientificName = "Amoeba Omnivora",
                trophicLevel = 2,
                diet = "OMNIVORE",
                description = "Consumidor secundario plástico y versátil. Su probóscide le permite ingerir clorofila vegetal y digerir restos proteicos de células muertas.",
                preyDescription = "Fitoplancton, detritus celular y pequeños herbívoros.",
                predatorDescription = "Depredadores carnívoros armados y leviatanes ápex.",
                discovered = true
            ),
            TrophicSpeciesEntity(
                speciesId = "omnivore_versatil",
                name = "Versátilus Flagelado",
                scientificName = "Flagellata Mixta",
                trophicLevel = 2,
                diet = "OMNIVORE",
                description = "Provisto de un flagelo largo y una membrana semi-rígida. Aprovecha cualquier recurso disponible en el caldo primigenio.",
                preyDescription = "Materia orgánica flotante y presas microscópicas.",
                predatorDescription = "Depredadores carnívoros con mandíbulas.",
                discovered = false
            ),
            TrophicSpeciesEntity(
                speciesId = "predator_didinium",
                name = "Didinium Cazador",
                scientificName = "Didinium Vorax",
                trophicLevel = 3,
                diet = "CARNIVORE",
                description = "Depredador voraz dotado de una trompa cazadora y mandíbulas queratinizadas. Caza activamente ciliados y otras células en movimiento.",
                preyDescription = "Herbívoros y omnívoros más pequeños.",
                predatorDescription = "Superdepredadores Ápex y células con púas pesadas.",
                discovered = false
            ),
            TrophicSpeciesEntity(
                speciesId = "predator_spiketooth",
                name = "Mordaza Puntiaguda",
                scientificName = "Odontopustula Ferox",
                trophicLevel = 3,
                diet = "CARNIVORE",
                description = "Equipado con púas frontales rígidas para embestir a sus víctimas y desarmar su membrana celular antes del consumo.",
                preyDescription = "Células medianas, herbívoros indefensos.",
                predatorDescription = "Leviatanes del fondo marino primordial.",
                discovered = false
            ),
            TrophicSpeciesEntity(
                speciesId = "apex_megacolossus",
                name = "Leviatán Abisal",
                scientificName = "Gigantomonas Rex",
                trophicLevel = 4,
                diet = "APEX",
                description = "El pináculo de la cadena trófica microscópica. Un coloso multiciliado con glándulas tóxicas y masa descomunal que devora todo a su paso.",
                preyDescription = "Cualquier organismo menor que él.",
                predatorDescription = "Ninguno en el caldo nativo; solo una protocélula altamente evolucionada puede desafiarlo.",
                discovered = false
            )
        )
    }
}
