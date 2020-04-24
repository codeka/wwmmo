package au.com.codeka.warworlds.common

import java.util.*

/**
 * A colour gradient is a a transformation between a floating point number between 0 and 1,
 * and a colour. The simplest gradient has two different colours at 0 and 1, and we will
 * return a value in between those two colours. More complicated options are possible by
 * having additional points in the middle.
 */
class ColourGradient {
  private val nodes = ArrayList<Node>()

  /**
   * Adds a node at the specified location on the gradient, with the specified colour.
   *
   * @param n      A value between 0 and 1.
   * @param colour The colour to return at that point.
   */
  fun addNode(n: Double, colour: Colour?) {
    var index = 0
    while (index < nodes.size - 1) {
      if (nodes[index].n > n) {
        break
      }
      index++
    }
    if (nodes.isNotEmpty()) {
      index++
    }
    nodes.add(index, Node(n, colour!!))
  }

  /**
   * Gets the [Colour] at the corresponding point on the gradient.
   */
  fun getColour(n: Double): Colour {
    if (nodes.isEmpty()) {
      return Colour(Colour.TRANSPARENT)
    }

    // if the value they gave us is less that our first node, return it's colour.
    if (nodes[0].n > n) {
      return Colour(nodes[0].colour)
    }
    val last = nodes.size - 1
    for (i in 0 until last) {
      val lhs = nodes[i]
      val rhs = nodes[i + 1]
      if (rhs.n > n) {
        val factor = (n - lhs.n) / (rhs.n - lhs.n)
        val c = Colour(lhs.colour)
        return Colour.interpolate(c, rhs.colour, factor)
      }
    }

    // if we get here, it's because the n they gave us is bigger than all nodes we've got
    return Colour(nodes[nodes.size - 1].colour)
  }

  /**
   * A node on the [ColourGradient]. We represent a value between 0 and 1, and the
   * corresponding colour.
   */
  internal inner class Node(var n: Double, var colour: Colour)
}