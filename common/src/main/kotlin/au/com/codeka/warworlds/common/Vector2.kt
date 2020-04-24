package au.com.codeka.warworlds.common

import java.util.*
import kotlin.math.*

/** Represents a 2-dimensional vector. */
class Vector2 {
  var x: Double
  var y: Double

  constructor() {
    y = 0.0
    x = y
  }

  constructor(x: Double, y: Double) {
    this.x = x
    this.y = y
  }

  constructor(other: Vector2) {
    x = other.x
    y = other.y
  }

  fun reset(x: Double, y: Double): Vector2 {
    this.x = x
    this.y = y
    return this
  }

  fun reset(other: Vector2): Vector2 {
    x = other.x
    y = other.y
    return this
  }

  /**
   * Gets the distance squared to the given other point. This is faster than [distanceTo] (since
   * no sqrt() is required) and still good enough for many purposes.
   */
  fun distanceTo2(other: Vector2): Double {
    val dx = other.x - x
    val dy = other.y - y
    return dx * dx + dy * dy
  }

  fun distanceTo(other: Vector2?): Double {
    val dx = other!!.x - x
    val dy = other.y - y
    return Math.sqrt(dx * dx + dy * dy)
  }

  fun distanceTo2(x: Double, y: Double): Double {
    val dx = x - this.x
    val dy = y - this.y
    return dx * dx + dy * dy
  }

  fun distanceTo(x: Double, y: Double): Double {
    val dx = x - this.x
    val dy = y - this.y
    return Math.sqrt(dx * dx + dy * dy)
  }

  fun length2(): Double {
    return x * x + y * y
  }

  fun length(): Double {
    return sqrt(length2())
  }

  fun add(other: Vector2) {
    x += other.x
    y += other.y
  }

  fun add(x: Float, y: Float) {
    this.x += x.toDouble()
    this.y += y.toDouble()
  }

  fun subtract(other: Vector2) {
    x -= other.x
    y -= other.y
  }

  fun subtract(x: Float, y: Float) {
    this.x -= x.toDouble()
    this.y -= y.toDouble()
  }

  fun rotate(radians: Double) {
    val nx = x * cos(radians) - y * sin(radians)
    val ny = y * cos(radians) + x * sin(radians)
    x = nx
    y = ny
  }

  fun normalize() {
    scale(1.0 / length())
  }

  fun scale(s: Double) {
    x *= s
    y *= s
  }

  fun scale(sx: Double, sy: Double) {
    x *= sx
    y *= sy
  }

  override fun hashCode(): Int {
    // this avoids the boxing that "new Double(x).hashCode()" would require
    val lx = java.lang.Double.doubleToRawLongBits(x)
    val ly = java.lang.Double.doubleToRawLongBits(y)
    return (lx xor ly).toInt()
  }

  override fun toString(): String {
    return String.format(Locale.ENGLISH, "(%.4f, %.4f)", x, y)
  }

  override fun equals(other: Any?): Boolean {
    if (other !is Vector2) {
      return false
    }
    val ov = other
    return x == ov.x && y == ov.y
  }

  fun equals(other: Vector2?, epsilon: Double): Boolean {
    return abs(other!!.x - x) < epsilon && abs(other.y - y) < epsilon
  }

  companion object {
    /**
     * Find the angle between "a" and "b"
     * see: http://www.gamedev.net/topic/487576-angle-between-two-lines-clockwise/
     */
    fun angleBetween(a: Vector2, b: Vector2): Float {
      return atan2(a.x * b.y - a.y * b.x,
          a.x * b.x + a.y * b.y).toFloat()
    }

    fun angleBetweenCcw(a: Vector2, b: Vector2): Float {
      return atan2(a.x * b.x + a.y * b.y,
          a.x * b.y - a.y * b.x).toFloat()
    }
  }
}