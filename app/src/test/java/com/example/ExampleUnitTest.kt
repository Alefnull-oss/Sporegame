package com.example

import com.example.spore.cinematics.CinematicCameraStage
import com.example.spore.cinematics.PlanetCinematicDefinition
import com.example.spore.data.model.PlanetDefinition
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun allPlanetsHaveValidScientificCinematics() {
    val planetIds = PlanetDefinition.PLANETS.map { it.id }
    assertEquals(5, planetIds.size)

    for (pId in planetIds) {
      val cinematic = PlanetCinematicDefinition.getCinematicForPlanet(pId)
      assertNotNull(cinematic)
      assertEquals(pId, cinematic.planetId)
      assertTrue("Title should not be blank", cinematic.scientificTitle.isNotBlank())
      assertTrue("Theory should not be blank", cinematic.primaryTheory.isNotBlank())
      assertTrue("Citations should have peer reviewed papers", cinematic.peerReviewedCitations.isNotEmpty())
      assertEquals("Should have exactly 5 sequential phases", 5, cinematic.phases.size)

      // Verify phase ordering and stages
      val expectedStages = listOf(
        CinematicCameraStage.ATMOSPHERE_ENTRY,
        CinematicCameraStage.GEOLOGICAL_DESCENT,
        CinematicCameraStage.CHEMICAL_REACTION,
        CinematicCameraStage.MOLECULAR_ASSEMBLY,
        CinematicCameraStage.CELLULAR_AWAKENING
      )

      for (i in 0 until 5) {
        val phase = cinematic.phases[i]
        assertEquals(i, phase.phaseIndex)
        assertEquals(expectedStages[i], phase.stage)
        assertTrue(phase.title.isNotBlank())
        assertTrue(phase.narration.isNotBlank())
        assertTrue(phase.keyMolecules.isNotEmpty())
      }
    }
  }
}
