package com.example.spore.game.physics

import com.example.spore.game.engine.Vector2 as SporeVector2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import org.dyn4j.dynamics.Body
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.dynamics.joint.DistanceJoint
import org.dyn4j.dynamics.joint.RevoluteJoint
import org.dyn4j.geometry.Geometry
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Vector2 as DynVector2
import org.dyn4j.world.World

/**
 * Soft-body membrane perimeter lattice.
 * Simulates elastic volume-preserving deformation when impacted by spikes,
 * predator jaws, or seabed rocks.
 */
class SoftBodyMembrane(val vertexCount: Int = 16) {
    // Current radial deformation from baseline radius for each perimeter vertex
    val deformations = FloatArray(vertexCount)
    private val velocities = FloatArray(vertexCount)

    // Spring constants for organic bouncy clay/membrane feel
    var stiffness: Float = 55.0f
    var damping: Float = 7.5f
    var lateralCoupling: Float = 0.35f

    fun applyImpact(localAngle: Float, impulse: Float, spreadAngle: Float = 0.85f) {
        val normalizedAngle = (localAngle % (2f * PI.toFloat()) + 2f * PI.toFloat()) % (2f * PI.toFloat())
        val step = (2f * PI.toFloat()) / vertexCount

        for (i in 0 until vertexCount) {
            val nodeAngle = i * step
            var diff = abs(nodeAngle - normalizedAngle)
            if (diff > PI.toFloat()) diff = (2f * PI.toFloat()) - diff

            if (diff < spreadAngle) {
                val falloff = 1.0f - (diff / spreadAngle)
                // Inward compression (indentation)
                velocities[i] -= impulse * falloff * 1.5f
            } else if (diff < spreadAngle * 1.8f) {
                // Lateral bulge for volume conservation
                val bulge = 1.0f - (diff / (spreadAngle * 1.8f))
                velocities[i] += impulse * bulge * 0.45f
            }
        }
    }

    fun update(deltaTime: Float) {
        val dt = deltaTime.coerceIn(0.005f, 0.04f)
        val step = (2f * PI.toFloat()) / vertexCount

        for (i in 0 until vertexCount) {
            val prev = if (i == 0) vertexCount - 1 else i - 1
            val next = if (i == vertexCount - 1) 0 else i + 1

            // Spring restoring force towards 0 + lateral coupling with neighbors
            val springForce = -stiffness * deformations[i]
            val neighborPull = lateralCoupling * ((deformations[prev] + deformations[next]) * 0.5f - deformations[i])
            val dampingForce = -damping * velocities[i]

            val accel = springForce + neighborPull * stiffness + dampingForce
            velocities[i] += accel * dt
            deformations[i] += velocities[i] * dt

            // Clamp max deformation to avoid inversion
            deformations[i] = deformations[i].coerceIn(-25f, 25f)
        }
    }

    fun getDeformedRadius(baseRadius: Float, angle: Float): Float {
        val normalizedAngle = (angle % (2f * PI.toFloat()) + 2f * PI.toFloat()) % (2f * PI.toFloat())
        val step = (2f * PI.toFloat()) / vertexCount
        val floatIdx = normalizedAngle / step
        val idx0 = (floatIdx.toInt()) % vertexCount
        val idx1 = (idx0 + 1) % vertexCount
        val frac = floatIdx - floatIdx.toInt()

        val deform = deformations[idx0] * (1f - frac) + deformations[idx1] * frac
        return max(baseRadius * 0.4f, baseRadius + deform)
    }
}

/**
 * Segment of an elastic appendage (flagellum or cilio) with water inertia and elastic restoring force.
 */
data class AppendageSegment(
    var position: SporeVector2,
    var velocity: SporeVector2 = SporeVector2.ZERO,
    var angle: Float = 0f,
    val length: Float,
    val radius: Float
)

/**
 * Elastic kinematic chain driven by hydrodynamic drag and spring joints.
 */
class ElasticAppendageChain(
    val segmentCount: Int = 5,
    val totalLength: Float = 80f,
    val baseRadius: Float = 6f
) {
    val segments = ArrayList<AppendageSegment>(segmentCount)

    init {
        val segLen = totalLength / segmentCount
        for (i in 0 until segmentCount) {
            val t = i.toFloat() / segmentCount
            val r = (baseRadius * (1.1f - t * 0.6f)).coerceAtLeast(2.5f)
            segments.add(AppendageSegment(position = SporeVector2.ZERO, length = segLen, radius = r))
        }
    }

    fun update(
        rootPos: SporeVector2,
        rootAngle: Float,
        cellVelocity: SporeVector2,
        deltaTime: Float,
        wavyPulse: Float = 0f
    ) {
        val dt = deltaTime.coerceIn(0.005f, 0.04f)
        var anchor = rootPos

        // Root segment aligns opposite to cell forward direction + sinusoidal swim stroke
        val rootDir = SporeVector2(cos(rootAngle + PI.toFloat() + wavyPulse), sin(rootAngle + PI.toFloat() + wavyPulse))

        for (i in 0 until segmentCount) {
            val seg = segments[i]
            val prevPos = seg.position

            // Target position based on joint length and chain propagation
            val targetPos = anchor + rootDir * (seg.length * (i + 1))
            val offset = targetPos - seg.position

            // Hydrodynamic drag: water resists movement perpendicular to segment orientation
            val waterDrag = 0.88f
            seg.velocity = (seg.velocity + offset * (40f * dt)) * waterDrag - cellVelocity * (0.15f * (i + 1))
            seg.position = seg.position + seg.velocity * dt

            // Maintain distance constraint (inverse kinematics spring)
            val toAnchor = seg.position - anchor
            val dist = toAnchor.length()
            if (dist > 0.001f) {
                seg.position = anchor + toAnchor.normalized() * seg.length
            } else {
                seg.position = anchor + rootDir * seg.length
            }

            seg.angle = atan2(seg.position.y - anchor.y, seg.position.x - anchor.x)
            anchor = seg.position
        }
    }
}

/**
 * Physical Jaws & Pinchers powered by Dyn4j.
 * Contains upper and lower mandibles mounted on revolute joints with angular motor
 * that can clamp, hold, and shove target cells.
 */
class PhysicalMandibles(
    val ownerBody: Body,
    val jawLength: Double = 25.0,
    val jawSpreadAngle: Double = 0.55
) {
    var isClamping: Boolean = false
    var clampTargetId: Long? = null
    var currentAperture: Double = jawSpreadAngle

    val upperMandible: Body = Body()
    val lowerMandible: Body = Body()

    private val upperJoint: RevoluteJoint<Body>
    private val lowerJoint: RevoluteJoint<Body>

    init {
        // Create mandible fixtures
        val upperShape = Geometry.createPolygon(
            DynVector2(0.0, 0.0),
            DynVector2(jawLength, -jawLength * 0.35),
            DynVector2(jawLength * 0.8, 0.0)
        )
        val lowerShape = Geometry.createPolygon(
            DynVector2(0.0, 0.0),
            DynVector2(jawLength * 0.8, 0.0),
            DynVector2(jawLength, jawLength * 0.35)
        )

        upperMandible.addFixture(BodyFixture(upperShape).apply {
            density = 1.0
            friction = 0.6
            restitution = 0.1
        })
        upperMandible.setMass(MassType.NORMAL)

        lowerMandible.addFixture(BodyFixture(lowerShape).apply {
            density = 1.0
            friction = 0.6
            restitution = 0.1
        })
        lowerMandible.setMass(MassType.NORMAL)

        // Revolute joints with angular limits
        val anchor = ownerBody.worldCenter
        upperJoint = RevoluteJoint(ownerBody, upperMandible, anchor).apply {
            setLimitsEnabled(true)
            setLimits(-jawSpreadAngle, 0.05)
            setMotorEnabled(true)
            setMotorSpeed(2.0)
            setMaximumMotorTorque(500.0)
        }

        lowerJoint = RevoluteJoint(ownerBody, lowerMandible, anchor).apply {
            setLimitsEnabled(true)
            setLimits(-0.05, jawSpreadAngle)
            setMotorEnabled(true)
            setMotorSpeed(-2.0)
            setMaximumMotorTorque(500.0)
        }
    }

    fun attachToWorld(world: World<Body>) {
        world.addBody(upperMandible)
        world.addBody(lowerMandible)
        world.addJoint(upperJoint)
        world.addJoint(lowerJoint)
    }

    fun triggerBite() {
        isClamping = true
        upperJoint.setMotorSpeed(6.0)
        lowerJoint.setMotorSpeed(-6.0)
    }

    fun releaseBite() {
        isClamping = false
        clampTargetId = null
        upperJoint.setMotorSpeed(-3.0)
        lowerJoint.setMotorSpeed(3.0)
    }

    fun update(deltaTime: Float) {
        val upperAngle = upperMandible.transform.rotationAngle
        val lowerAngle = lowerMandible.transform.rotationAngle
        currentAperture = abs(lowerAngle - upperAngle)

        // Auto release if fully clamped
        if (isClamping && currentAperture < 0.08) {
            // Reopen after bite impact
            releaseBite()
        }
    }
}

/**
 * Main dyn4j physics world coordinator for Spore cells.
 */
class SporePhysicsEngine {
    val world: World<Body> = World<Body>()

    init {
        // Zero gravity in 2D primordial aquatic space
        world.gravity = DynVector2(0.0, 0.0)
    }

    fun createCellBody(radius: Float, posX: Float, posY: Float, isPlayer: Boolean = false): Body {
        val body = Body()
        val circle = Geometry.createCircle(radius.toDouble())
        val fixture = BodyFixture(circle).apply {
            density = 1.2
            friction = 0.3
            restitution = 0.45 // Bouncy organic membrane
        }
        body.addFixture(fixture)
        body.setMass(MassType.NORMAL)
        body.transform.setTranslation(posX.toDouble(), posY.toDouble())
        body.linearDamping = 1.8 // Aquatic drag damping
        body.angularDamping = 3.0
        world.addBody(body)
        return body
    }

    fun removeBody(body: Body) {
        world.removeBody(body)
    }

    fun stepSimulation(deltaTime: Float) {
        val dt = deltaTime.coerceIn(0.005f, 0.033f)
        world.update(dt.toDouble())
    }
}
