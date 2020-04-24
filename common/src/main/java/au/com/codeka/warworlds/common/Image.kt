package au.com.codeka.warworlds.common

import kotlin.math.abs

/**
 * Represents an image, and provides methods for converting to/from common formats.
 */
class Image @JvmOverloads constructor(width: Int, height: Int, fill: Colour = Colour.BLACK) {
  val argb: IntArray
  val width: Int
  val height: Int

  fun getPixelColour(x: Int, y: Int): Colour {
    return Colour(argb[y * width + x])
  }

  fun setPixelColour(x: Int, y: Int, c: Colour) {
    if (x < 0 || x >= width) return
    if (y < 0 || y >= height) return
    argb[y * width + x] = c.toArgb()
  }

  fun blendPixelColour(x: Int, y: Int, c: Colour) {
    if (x < 0 || x >= width) return
    if (y < 0 || y >= height) return
    var imgColour = Colour(argb[y * width + x])
    imgColour = Colour.blend(imgColour, c)
    argb[y * width + x] = imgColour.toArgb()
  }

  /**
   * Draws a circle at the given (x,y) coordinates with the given radius and colour.
   */
  fun drawCircle(cx: Int, cy: Int, radius: Double, c: Colour) {
    val centre = Vector2(cx.toDouble(), cy.toDouble())
    for (y in (cy - radius).toInt()..(cy + radius).toInt()) {
      for (x in (cx - radius).toInt()..(cx + radius).toInt()) {
        val distance = centre.distanceTo(x.toDouble(), y.toDouble())
        if (distance < radius) {
          setPixelColour(x, y, c)
        }
      }
    }
  }

  /**
   * Draws a line from the given (x1,y1) to (x2,y2) in the given colour.
   */
  fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int, c: Colour) {
    var x1 = x1
    var y1 = y1
    val dx = abs(x2 - x1)
    val dy = abs(y2 - y1)
    val sx = if (x1 < x2) 1 else -1
    val sy = if (y1 < y2) 1 else -1
    var err = dx - dy
    while (true) {
      setPixelColour(x1, y1, c)
      if (x1 == x2 && y1 == y2) break
      val e2 = 2 * err
      if (e2 > -dy) {
        err -= dy
        x1 += sx
      }
      if (e2 < dx) {
        err += dx
        y1 += sy
      }
    }
  }

  /**
   * Draws the given triangle to this image.
   *
   * http://joshbeam.com/articles/triangle_rasterization/
   */
  fun drawTriangle(x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int, c: Colour) {
    val edges = arrayOf(
        TriangleEdge(x1, y1, x2, y2),
        TriangleEdge(x2, y2, x3, y3),
        TriangleEdge(x3, y3, x1, y1)
    )

    // find out which edge is the tallest
    var maxLength = 0
    var longEdge = 0
    for (i in 0..2) {
      val length = edges[i].y2 - edges[i].y1
      if (length > maxLength) {
        maxLength = length
        longEdge = i
      }
    }
    drawSpansBetweenEdges(edges[longEdge], edges[(longEdge + 1) % 3], c)
    drawSpansBetweenEdges(edges[longEdge], edges[(longEdge + 2) % 3], c)
  }

  /**
   * This is basically a modification on bresenham's algroithm we use for drawLine()
   */
  private fun drawSpansBetweenEdges(longEdge: TriangleEdge, shortEdge: TriangleEdge, c: Colour) {
    var x11 = longEdge.x1
    val x12 = longEdge.x2
    var x21 = shortEdge.x1
    val x22 = shortEdge.x2
    var y11 = longEdge.y1
    val y12 = longEdge.y2
    var y21 = shortEdge.y1
    val y22 = shortEdge.y2
    val dx1 = abs(x12 - x11)
    val dy1 = abs(y12 - y11)
    val dx2 = abs(x22 - x21)
    val dy2 = abs(y22 - y21)
    val sx1 = if (x11 < x12) 1 else -1
    val sy1 = if (y11 < y12) 1 else -1
    val sx2 = if (x21 < x22) 1 else -1
    val sy2 = if (y21 < y22) 1 else -1
    var err1 = dx1 - dy1
    var err2 = dx2 - dy2
    while (true) {
      if (y11 == y21) {
        var startX = x11
        var endX = x21
        if (startX > endX) {
          val tmp = startX
          startX = endX
          endX = tmp
        }
        for (x in startX..endX) {
          setPixelColour(x, y11, c)
        }
      }
      if (x11 == x12 && y11 == y12) return
      if (x21 == x22 && y21 == y22) return
      val e12 = 2 * err1
      if (e12 > -dy1) {
        err1 -= dy1
        x11 += sx1
      }
      if (e12 < dx1) {
        err1 += dx1
        y11 += sy1
      }
      while (y11 > y21) {
        val e22 = 2 * err2
        if (e22 > -dy2) {
          err2 -= dy2
          x21 += sx2
        }
        if (e22 < dx2) {
          err2 += dx2
          y21 += sy2
        }
        if (x21 == x22 && y21 == y22) return
      }
    }
  }

  private class TriangleEdge(x1: Int, y1: Int, x2: Int, y2: Int) {
    var x1 = 0
    var y1 = 0
    var x2 = 0
    var y2 = 0

    init {
      if (y1 < y2) {
        this.x1 = x1
        this.y1 = y1
        this.x2 = x2
        this.y2 = y2
      } else {
        this.x1 = x2
        this.y1 = y2
        this.x2 = x1
        this.y2 = y1
      }
    }
  }

  init {
    val argb = fill.toArgb()
    this.width = width
    this.height = height
    this.argb = IntArray(width * height)
    for (y in 0 until height) {
      for (x in 0 until width) {
        this.argb[y * width + x] = argb
      }
    }
  }
}