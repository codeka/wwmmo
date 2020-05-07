package au.com.codeka.warworlds.planetrender.ui

import java.awt.Container
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Insets

/**
 * The normal [FlowLayout] doesn't respond very well when it's contents get too tall. This one
 * works much better.
 *
 *
 * See http://stackoverflow.com/a/4611117/241462
 */
class GoodFlowLayout(align: Int, hgap: Int, vgap: Int) : FlowLayout(align, hgap, vgap) {
  override fun minimumLayoutSize(target: Container): Dimension {
    // Size of largest component, so we can resize it in either direction with something like a
    // split-pane.
    return computeMinSize(target)
  }

  override fun preferredLayoutSize(target: Container): Dimension {
    return computeSize(target)
  }

  private fun computeSize(target: Container): Dimension {
    synchronized(target.treeLock) {
      val hgap = hgap
      val vgap = vgap
      var w = target.width

      // Let this behave like a regular FlowLayout (single row) if the container hasn't been
      // assigned any size yet
      if (w == 0) {
        w = Int.MAX_VALUE
      }
      var insets = target.insets
      if (insets == null) {
        insets = Insets(0, 0, 0, 0)
      }
      var requiredWidth = 0
      val maxWidth = w - (insets.left + insets.right + hgap * 2)
      val n = target.componentCount
      var x = 0
      var y = insets.top + vgap // FlowLayout starts by adding vgap, so do that here too.
      var rowHeight = 0
      for (i in 0 until n) {
        val c = target.getComponent(i)
        if (c.isVisible) {
          val d = c.preferredSize
          if (x == 0 || x + d.width <= maxWidth) {
            // fits in current row.
            if (x > 0) {
              x += hgap
            }
            x += d.width
            rowHeight = Math.max(rowHeight, d.height)
          } else {
            // Start of new row
            x = d.width
            y += vgap + rowHeight
            rowHeight = d.height
          }
          requiredWidth = Math.max(requiredWidth, x)
        }
      }
      y += rowHeight
      y += insets.bottom
      return Dimension(requiredWidth + insets.left + insets.right, y)
    }
  }

  private fun computeMinSize(target: Container): Dimension {
    synchronized(target.treeLock) {
      var minx = Int.MAX_VALUE
      var miny = Int.MIN_VALUE
      var found_one = false
      val n = target.componentCount
      for (i in 0 until n) {
        val c = target.getComponent(i)
        if (c.isVisible) {
          found_one = true
          val d = c.preferredSize
          minx = Math.min(minx, d.width)
          miny = Math.min(miny, d.height)
        }
      }
      return if (found_one) {
        Dimension(minx, miny)
      } else Dimension(0, 0)
    }
  }

  companion object {
    private const val serialVersionUID = 1L
  }
}