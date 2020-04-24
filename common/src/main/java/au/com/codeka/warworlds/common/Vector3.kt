package au.com.codeka.warworlds.common

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Helper class that represents a 3-dimensional vector.
 */
class Vector3 {
  var x = 0.0
  var y = 0.0
  var z = 0.0

  constructor()

  constructor(copy: Vector3) {
    reset(copy)
  }

  constructor(x: Double, y: Double, z: Double) {
    reset(x, y, z)
  }

  fun reset(x: Double, y: Double, z: Double): Vector3 {
    this.x = x
    this.y = y
    this.z = z
    return this
  }

  fun reset(other: Vector3): Vector3 {
    x = other.x
    y = other.y
    z = other.z
    return this
  }

  fun length(): Double {
    return sqrt(x * x + y * y + z * z)
  }

  fun normalize() {
    val s = 1.0 / length()
    scale(s)
  }

  fun rotateX(radians: Double) {
    val y1 = y * cos(radians) - z * sin(radians)
    val z1 = y * sin(radians) + z * cos(radians)
    y = y1
    z = z1
  }

  fun rotateY(radians: Double) {
    val x1 = x * cos(radians) - z * sin(radians)
    val z1 = x * sin(radians) + z * cos(radians)
    x = x1
    z = z1
  }

  fun rotateZ(radians: Double) {
    val x1 = x * cos(radians) - y * sin(radians)
    val y1 = x * sin(radians) + y * cos(radians)
    x = x1
    y = y1
  }

  fun scale(s: Double) {
    x *= s
    y *= s
    z *= s
  }

  fun subtract(rhs: Vector3) {
    x -= rhs.x
    y -= rhs.y
    z -= rhs.z
  }

  fun add(rhs: Vector3) {
    x += rhs.x
    y += rhs.y
    z += rhs.z
  }

  companion object {
    fun distanceBetween(lhs: Vector3, rhs: Vector3): Double {
      val dx = rhs.x - lhs.x
      val dy = rhs.y - lhs.y
      val dz = rhs.z - lhs.z
      return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun dot(a: Vector3, b: Vector3): Double {
      return a.x * b.x + a.y * b.y + a.z * b.z
    }

    fun cross(a: Vector3, b: Vector3): Vector3 {
      return Vector3(
          a.y * b.z - a.z * b.y,
          a.z * b.x - a.x * b.z,
          a.x * b.y - a.y * b.x)
    }

    fun interpolate(result: Vector3, rhs: Vector3, n: Double) {
      result.x += n * (rhs.x - result.x)
      result.y += n * (rhs.y - result.y)
      result.z += n * (rhs.z - result.z)
    }
  }
}