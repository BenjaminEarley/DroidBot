package com.benjaminearley.droidbot

import java.lang.Math.PI
import java.lang.Math.abs
import java.lang.Math.cos
import java.lang.Math.sin
import java.lang.Math.sqrt

val Tau = PI.toFloat() * 2.0f
val numWheels = 3

val xUnit = Vector3(1.0f, 0.0f, 0.0f)
val yUnit = Vector3(0.0f, 1.0f, 0.0f)
val zUnit = Vector3(0.0f, 0.0f, 1.0f)

data class Vector2(val x: Float, val y: Float) {
    operator fun plus(v: Vector2): Vector2 = Vector2(x + v.x, y + v.y)
    fun scale(s: Float): Vector2 = Vector2(x * s, y * s)
    fun divide(s: Float): Vector2 = Vector2(x / s, y / s)
    infix fun dot(v: Vector2): Float = (x * v.x) + (y * v.y)

    val clipToUnit: Vector2
        get() = dot(this).let { lengthSquared ->
            if (lengthSquared <= 1.0) this
            else divide(sqrt(lengthSquared.toDouble()).toFloat())
        }
}

data class Vector3(val x: Float, val y: Float, val z: Float) {
    fun scale(s: Float): Vector3 = Vector3(x * s, y * s, z * s)
    infix fun dot(v: Vector3): Float = x * v.x + y * v.y + z * v.z

    fun cross(v: Vector3): Vector3 = Vector3(
        y * v.z - z * v.y,
        z * v.x - x * v.z,
        x * v.y - y * v.x)

    operator fun unaryMinus(): Vector3 = Vector3(-x, -y, -z)
}

data class Quaternion(val s: Float, val v: Vector3) {
    val conjugate: Quaternion get() = Quaternion(s, -v)

    infix fun mul(q: Quaternion): Quaternion = Quaternion(
        s * q.s - (v dot q.v),
        Vector3(
            v.y * q.v.z - v.z * q.v.y + s * q.v.x + v.x * q.s,
            v.z * q.v.x - v.x * q.v.z + s * q.v.y + v.y * q.s,
            v.x * q.v.y - v.y * q.v.x + s * q.v.z + v.z * q.s))

    fun rotate(v: Vector3): Vector3 = (this mul Quaternion(0.0f, v) mul conjugate).v
}

fun quaternionFromVector(axis: Vector3, r: Float): Quaternion = Quaternion(
    cos((r / 2.0f).toDouble()).toFloat(),
    axis.scale(sin((r / 2.0f).toDouble()).toFloat()))

typealias Wheels = List<Vector3>

val wheels: List<Vector3> = (0 until numWheels).map {
    quaternionFromVector(zUnit, (it.toFloat() / numWheels.toFloat()) * Tau).rotate(xUnit)
}

typealias Speeds = List<Float>

fun lateralSpeeds(wheels: Wheels, x: Float, y: Float): Speeds =
    Vector3(x, y, 0.0f).let { direction -> wheels.map { wheel -> direction.cross(wheel).z } }

fun rotationalSpeeds(wheels: Wheels, turnRate: Float): Speeds = wheels.map { -turnRate }

fun combineSpeeds(lateral: Speeds, rotational: Speeds): Speeds =
    lateral.zip(rotational).map { (lat, rot) -> lat + rot }

fun scaleToWithinMaxSpeed(speeds: Speeds): Speeds =
    speeds.map(::abs).max()!!.let { max -> if (max > 1.0) speeds.map { it / max } else speeds }

val maxSum = 1.25f

fun scaleToWithinMaxSum(speeds: Speeds): Speeds =
    speeds.map(::abs).sum().let { sum -> if (sum > maxSum) speeds.map { it * (maxSum / sum) } else speeds }

// Magnitude of direction should not exceed 1.0
// direction x axis is pointed in the direction of the first wheel.
// direction y axis is pointed such that the z axis is pointed upward in a right handed coordinate system.
// turnDirection: range from -1.0 to 1.0
// When turnDirection is positive it is CCW.
// Speed is positive it is CCW when facing toward the center of robot.

fun getSpeeds(direction: Vector2, turnDirection: Float): Speeds =
    direction.clipToUnit.let { (xDir, yDir) ->
        combineSpeeds(
            lateralSpeeds(wheels, xDir, yDir),
            rotationalSpeeds(wheels, turnDirection)
        ) pipe
            ::scaleToWithinMaxSpeed pipe

            // Scale down the sum of all wheel speeds to ensure max power draw is not exceeded.

            ::scaleToWithinMaxSum
    }
