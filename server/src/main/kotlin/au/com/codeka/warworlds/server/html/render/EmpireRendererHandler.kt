package au.com.codeka.warworlds.server.html.render

import au.com.codeka.warworlds.common.Log
import java.awt.Color
import java.awt.color.ColorSpace
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO

/** [RendererHandler] for rendering empire shields. */
class EmpireRendererHandler : RendererHandler() {
   private val log = Log("EmpireRendererHandler")

  override fun get() {
    val empireId = getUrlParameter("empire")!!.toLong()
    var width = getUrlParameter("width")!!.toInt()
    var height = getUrlParameter("height")!!.toInt()
    val bucket = getUrlParameter("bucket")
    val factor = BUCKET_FACTORS[bucket]
    if (factor == null) {
      log.warning("Invalid bucket: %s", request.pathInfo)
      response.status = 404
      return
    }
    val cacheFile = File(String.format(Locale.ENGLISH,
        "data/cache/empire/%d/%dx%d/%s.png", empireId, width, height, bucket))
    if (cacheFile.exists()) {
      serveCachedFile(cacheFile)
      return
    } else {
      cacheFile.parentFile.mkdirs()
    }
    width = (width * factor).toInt()
    height = (height * factor).toInt()

    // TODO: if they have a custom one, use that
    //WatchableObject<Empire> empire = EmpireManager.i.getEmpire(empireId);
    var shieldImage = BufferedImage(128, 128, ColorSpace.TYPE_RGB)
    val g = shieldImage.createGraphics()
    g.paint = getShieldColour(empireId)
    g.fillRect(0, 0, 128, 128)

    // Merge the shield image with the outline image.
    shieldImage = mergeShieldImage(shieldImage)

    // Resize the image if required.
    if (width != 128 || height != 128) {
      val w = shieldImage.width
      val h = shieldImage.height
      val after = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
      val at = AffineTransform()
      at.scale(width.toFloat() / w.toDouble(), height.toFloat() / h.toDouble())
      val scaleOp = AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC)
      shieldImage = scaleOp.filter(shieldImage, after)
    }

    ImageIO.write(shieldImage, "png", cacheFile)
    serveCachedFile(cacheFile)
  }

  private fun getShieldColour(empireID: Long): Color {
    if (empireID == 0L) {
      return Color(Color.TRANSLUCENT)
    }
    val rand = Random(empireID xor 7438274364563846L)
    return Color(rand.nextInt(100) + 100, rand.nextInt(100) + 100, rand.nextInt(100) + 100)
  }

  private fun mergeShieldImage(shieldImage: BufferedImage): BufferedImage {
    val finalImage = ImageIO.read(File("data\\renderer\\empire\\shield.png"))
    val width = finalImage.width
    val height = finalImage.height
    val fx = shieldImage.width.toFloat() / width.toFloat()
    val fy = shieldImage.height.toFloat() / height.toFloat()
    for (y in 0 until height) {
      for (x in 0 until width) {
        var pixel = finalImage.getRGB(x, y)
        if (pixel and 0xffffff == 0xff00ff) {
          pixel = shieldImage.getRGB((x * fx).toInt(), (y * fy).toInt())
          finalImage.setRGB(x, y, pixel)
        }
      }
    }
    return finalImage
  }
}