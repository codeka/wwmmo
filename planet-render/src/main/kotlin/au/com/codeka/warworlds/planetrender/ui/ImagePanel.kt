package au.com.codeka.warworlds.planetrender.ui

import au.com.codeka.warworlds.common.Colour
import java.awt.Color
import java.awt.Graphics
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.image.MemoryImageSource
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.JPanel

internal class ImagePanel : JPanel() {
  private var image: Image? = null
  private var imageWidth = 0
  private var imageHeight = 0
  private var backgroundColour: Colour? = null
  fun setImage(img: au.com.codeka.warworlds.common.Image) {
    val mis = MemoryImageSource(img.width, img.height, img.argb, 0, img.width)
    image = topLevelAncestor.createImage(mis)
    imageWidth = img.width
    imageHeight = img.height
    repaint()
  }

  @Throws(IOException::class)
  fun saveImageAs(f: File?) {
    val img = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
    val g = img.graphics
    g.drawImage(image, 0, 0, this)
    g.dispose()
    ImageIO.write(img, "png", f)
  }

  fun setBackgroundColour(c: Colour?) {
    backgroundColour = c
    repaint()
  }

  public override fun paintComponent(g: Graphics) {
    var width = width
    var height = height

    // fill the entire background gray
    var bg = Color.LIGHT_GRAY
    if (backgroundColour != null) {
      bg = Color(backgroundColour!!.r.toFloat(), backgroundColour!!.g.toFloat(),
          backgroundColour!!.b.toFloat(), backgroundColour!!.a.toFloat())
    }
    g.color = bg
    g.fillRect(0, 0, width, height)
    if (image != null) {
      val sx = (width - imageWidth) / 2
      val sy = (height - imageHeight) / 2
      width = imageWidth
      height = imageHeight

      // If the background is set to transparent, we'll draw a checkerboard background to represent
      // the transparent parts of the image.
      if (backgroundColour == null) {
        g.color = Color.GRAY
        var odd = false
        run {
          var y = sy
          while (y < sy + height) {
            val xOffset = if (odd) 16 else 0
            odd = !odd
            run {
              var x = sx + xOffset
              while (x < sx + width) {
                g.fillRect(x, y, 16, 16)
                x += 32
              }
            }
            y += 16
          }
        }
      }
      g.drawImage(image, sx, sy, null)
    }
  }

  companion object {
    private const val serialVersionUID = 1L
  }
}