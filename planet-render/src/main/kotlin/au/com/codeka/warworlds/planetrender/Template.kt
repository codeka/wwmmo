package au.com.codeka.warworlds.planetrender

import au.com.codeka.warworlds.common.Colour
import au.com.codeka.warworlds.common.ColourGradient
import au.com.codeka.warworlds.common.Vector3
import au.com.codeka.warworlds.common.XmlIterator.childElements
import au.com.codeka.warworlds.planetrender.Template.AtmosphereTemplate.AtmosphereTemplateFactory
import au.com.codeka.warworlds.planetrender.Template.ColourGradientTemplate.ColourGradientTemplateFactory
import au.com.codeka.warworlds.planetrender.Template.PerlinNoiseTemplate.PerlinNoiseTemplateFactory
import au.com.codeka.warworlds.planetrender.Template.PlanetTemplate.PlanetTemplateFactory
import au.com.codeka.warworlds.planetrender.Template.PlanetsTemplate.PlanetsTemplateFactory
import au.com.codeka.warworlds.planetrender.Template.PointCloudTemplate.PointCloudTemplateFactory
import au.com.codeka.warworlds.planetrender.Template.TextureTemplate.TextureTemplateFactory
import au.com.codeka.warworlds.planetrender.Template.VoronoiTemplate.VoronoiTemplateFactory
import au.com.codeka.warworlds.planetrender.Template.WarpTemplate.WarpTemplateFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.InputStream
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

/**
 * A template is used to create an image (usually a complete planet, but it doesn't have to be).
 */
class Template {
  var template: BaseTemplate? = null
    private set
  var templateVersion = 0
    private set

  /*
   * The name of the template is whatever you want it to be. We don't care...
   */ var name: String? = null

  abstract class TemplateFactory {
    @Throws(TemplateException::class)
    abstract fun parse(elem: Element): BaseTemplate

    @Throws(TemplateException::class)
    protected fun parseVector3(`val`: String): Vector3 {
      val parts = `val`.split(" ").toTypedArray()
      return if (parts.size == 3) {
        Vector3(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble())
      } else {
        throw TemplateException("Invalid vector: $`val`")
      }
    }
  }

  open class BaseTemplate {
    val parameters: MutableList<BaseTemplate>

    fun <T : BaseTemplate?> getParameters(classFactory: Class<T>): List<T> {
      val params: MutableList<T> = ArrayList()
      for (bt in parameters) {
        if (bt.javaClass.isAssignableFrom(classFactory)) {
          @Suppress("UNCHECKED_CAST")
          params.add(bt as T)
        }
      }
      return params
    }

    fun <T : BaseTemplate?> getParameter(classFactory: Class<T>): T? {
      val params = getParameters(classFactory)
      return if (params.isNotEmpty()) {
        params[0]
      } else null
    }

    init {
      parameters = ArrayList()
    }
  }

  class PlanetsTemplate : BaseTemplate() {
    class PlanetsTemplateFactory : TemplateFactory() {
      @Throws(TemplateException::class)
      override fun parse(elem: Element): BaseTemplate {
        val tmpl = PlanetsTemplate()
        for (child in childElements(elem, null)) {
          tmpl.parameters.add(parseElement(child))
        }
        return tmpl
      }
    }
  }

  class PlanetTemplate : BaseTemplate() {
    lateinit var northFrom: Vector3
      private set
    lateinit var northTo: Vector3
      private set
    var planetSize = 0.0
      private set
    var ambient = 0.0
      private set
    lateinit var sunLocation: Vector3
    lateinit var originFrom: Vector3
      private set
    lateinit var originTo: Vector3
      private set

    class PlanetTemplateFactory : TemplateFactory() {
      /**
       * Parses a <planet> node and returns the corresponding \c PlanetTemplate.
       */
      override fun parse(elem: Element): BaseTemplate {
        val tmpl = PlanetTemplate()
        tmpl.originFrom = Vector3(0.0, 0.0, 30.0)
        tmpl.originTo = Vector3(0.0, 0.0, 30.0)
        if (elem.getAttribute("origin") != null && elem.getAttribute("origin") != "") {
          val other = parseVector3(elem.getAttribute("origin"))
          tmpl.originFrom.reset(other)
          tmpl.originTo.reset(other)
        }
        if (elem.getAttribute("originFrom") != null
            && elem.getAttribute("originFrom") != "") {
          val other = parseVector3(elem.getAttribute("originFrom"))
          tmpl.originFrom.reset(other)
        }
        if (elem.getAttribute("originTo") != null && elem.getAttribute("originTo") != "") {
          val other = parseVector3(elem.getAttribute("originTo"))
          tmpl.originTo.reset(other)
        }
        tmpl.northFrom = Vector3(0.0, 1.0, 0.0)
        tmpl.northTo = Vector3(0.0, 1.0, 0.0)
        if (elem.getAttribute("northFrom") != null && elem.getAttribute("northFrom") != "") {
          val other = parseVector3(elem.getAttribute("northFrom"))
          tmpl.northFrom.reset(other)
        }
        if (elem.getAttribute("northTo") != null && elem.getAttribute("northTo") != "") {
          val other = parseVector3(elem.getAttribute("northTo"))
          tmpl.northTo.reset(other)
        }
        tmpl.planetSize = 10.0
        if (elem.getAttribute("size") != null && elem.getAttribute("size") != "") {
          tmpl.planetSize = elem.getAttribute("size").toDouble()
        }
        tmpl.ambient = 0.1
        if (elem.getAttribute("ambient") != null && elem.getAttribute("ambient") != "") {
          tmpl.ambient = elem.getAttribute("ambient").toDouble()
        }
        tmpl.sunLocation = Vector3(100.0, 100.0, -150.0)
        if (elem.getAttribute("sun") != null && elem.getAttribute("sun") != "") {
          val other = parseVector3(elem.getAttribute("sun"))
          tmpl.sunLocation.reset(other)
        }
        for (child in childElements(elem, null)) {
          tmpl.parameters.add(parseElement(child))
        }
        return tmpl
      }
    }
  }

  class TextureTemplate : BaseTemplate() {
    enum class Generator {
      VoronoiMap, PerlinNoise
    }

    var generator: Generator? = null
      private set
    var noisiness = 0.0
      private set
    var scaleX = 0.0
      private set
    var scaleY = 0.0
      private set

    class TextureTemplateFactory : TemplateFactory() {
      /**
       * Parses a <texture> node and returns the corresponding \c TextureTemplate.
      </texture> */
      @Throws(TemplateException::class)
      override fun parse(elem: Element): BaseTemplate {
        val tmpl = TextureTemplate()
        val generator = elem.getAttribute("generator")
        if (generator == "voronoi-map") {
          tmpl.generator = Generator.VoronoiMap
        } else if (generator == "perlin-noise") {
          tmpl.generator = Generator.PerlinNoise
        } else {
          throw TemplateException("Unknown <texture generator> attribute: $generator")
        }
        val noisiness = elem.getAttribute("noisiness")
        if (noisiness == null || noisiness == "") {
          tmpl.noisiness = 0.5
        } else {
          tmpl.noisiness = noisiness.toDouble()
        }
        tmpl.scaleX = 1.0
        tmpl.scaleY = 1.0
        if (elem.getAttribute("scaleX") != null && elem.getAttribute("scaleX") != "") {
          tmpl.scaleX = elem.getAttribute("scaleX").toDouble()
        }
        if (elem.getAttribute("scaleY") != null && elem.getAttribute("scaleY") != "") {
          tmpl.scaleY = elem.getAttribute("scaleY").toDouble()
        }
        for (child in childElements(elem, null)) {
          tmpl.parameters.add(parseElement(child))
        }
        return tmpl
      }
    }
  }

  class VoronoiTemplate : BaseTemplate() {
    class VoronoiTemplateFactory : TemplateFactory() {
      /**
       * Parses a <voronoi> node and returns the corresponding \c VoronoiTemplate.
      </voronoi> */
      @Throws(TemplateException::class)
      override fun parse(elem: Element): BaseTemplate {
        val tmpl = VoronoiTemplate()
        for (child in childElements(elem, null)) {
          tmpl.parameters.add(parseElement(child))
        }
        return tmpl
      }
    }
  }

  class PointCloudTemplate : BaseTemplate() {
    enum class Generator {
      Random, Poisson
    }

    var generator: Generator? = null
      private set
    var density = 0.0
      private set
    var randomness = 0.0
      private set

    class PointCloudTemplateFactory : TemplateFactory() {
      /**
       * Parses a <point-cloud> node and returns the corresponding \c PointCloudTemplate.
      </point-cloud> */
      @Throws(TemplateException::class)
      override fun parse(elem: Element): BaseTemplate {
        val tmpl = PointCloudTemplate()
        val `val` = elem.getAttribute("generator")
        if (`val` == "random") {
          tmpl.generator = Generator.Random
        } else if (`val` == "poisson") {
          tmpl.generator = Generator.Poisson
        } else {
          throw TemplateException("Unknown <point-cloud> 'generator' attribute: $`val`")
        }
        tmpl.density = elem.getAttribute("density").toDouble()
        tmpl.randomness = elem.getAttribute("randomness").toDouble()
        return tmpl
      }
    }
  }

  class ColourGradientTemplate : BaseTemplate() {
    lateinit var colourGradient: ColourGradient
      private set

    /**
     * Parses a <colour> node and returns the corresponding \c ColourGradient.
    </colour> */
    class ColourGradientTemplateFactory : TemplateFactory() {
      override fun parse(elem: Element): BaseTemplate {
        val tmpl = ColourGradientTemplate()
        tmpl.colourGradient = ColourGradient()
        for (child in childElements(elem, "node")) {
          val n = child.getAttribute("n").toDouble()
          val argb = child.getAttribute("colour").toLong(16).toInt()
          tmpl.colourGradient.addNode(n, Colour(argb))
        }
        return tmpl
      }
    }
  }

  class PerlinNoiseTemplate : BaseTemplate() {
    enum class Interpolation {
      None, Linear, Cosine
    }

    var persistence = 0.0
      private set
    var interpolation: Interpolation? = null
      private set
    var startOctave = 0
      private set
    var endOctave = 0
      private set

    class PerlinNoiseTemplateFactory : TemplateFactory() {
      override fun parse(elem: Element): BaseTemplate {
        val tmpl = PerlinNoiseTemplate()
        val `val` = elem.getAttribute("interpolation")
        if (`val` == null || `val` == "linear") {
          tmpl.interpolation = Interpolation.Linear
        } else if (`val` == "cosine") {
          tmpl.interpolation = Interpolation.Cosine
        } else if (`val` == "none") {
          tmpl.interpolation = Interpolation.None
        } else {
          throw TemplateException("Unknown <perlin> 'interpolation' attribute: $`val`")
        }
        tmpl.persistence = elem.getAttribute("persistence").toDouble()
        if (elem.getAttribute("startOctave") == null) {
          tmpl.startOctave = 1
        } else {
          tmpl.startOctave = elem.getAttribute("startOctave").toInt()
        }
        if (elem.getAttribute("endOctave") == null) {
          tmpl.endOctave = 10
        } else {
          tmpl.endOctave = elem.getAttribute("endOctave").toInt()
        }
        return tmpl
      }
    }
  }

  class AtmosphereTemplate : BaseTemplate() {
    enum class BlendMode {
      Alpha, Additive, Multiply
    }

    var innerTemplate: InnerOuterTemplate? = null
      private set
    var outerTemplate: InnerOuterTemplate? = null
      private set
    var starTemplate: StarTemplate? = null
      private set

    open class InnerOuterTemplate : BaseTemplate() {
      var sunStartShadow = 0.0
      var sunShadowFactor = 0.0
      var size = 0.0
      var noisiness = 0.0
      var blendMode: BlendMode? = null
    }

    class StarTemplate : InnerOuterTemplate() {
      var numPoints = 0
      var baseWidth = 0.0
      var slope = 0.0
    }

    class AtmosphereTemplateFactory : TemplateFactory() {
      override fun parse(elem: Element): BaseTemplate {
        val tmpl = AtmosphereTemplate()
        for (child in childElements(elem, null)) {
          when (child.tagName) {
            "inner" -> {
              tmpl.innerTemplate = InnerOuterTemplate()
              parseInnerOuterTemplate(tmpl.innerTemplate!!, child)
            }
            "outer" -> {
              tmpl.outerTemplate = InnerOuterTemplate()
              parseInnerOuterTemplate(tmpl.outerTemplate!!, child)
            }
            "star" -> {
              tmpl.starTemplate = StarTemplate()
              parseStarTemplate(tmpl.starTemplate!!, child)
            }
          }
        }
        return tmpl
      }

      private fun parseInnerOuterTemplate(tmpl: InnerOuterTemplate, elem: Element) {
        if (elem.getAttribute("sunStartShadow") != null
            && elem.getAttribute("sunStartShadow") != "") {
          tmpl.sunStartShadow = elem.getAttribute("sunStartShadow").toDouble()
        }
        if (elem.getAttribute("sunShadowFactor") != null
            && elem.getAttribute("sunShadowFactor") != "") {
          tmpl.sunShadowFactor = elem.getAttribute("sunShadowFactor").toDouble()
        }
        tmpl.size = 1.0
        if (elem.getAttribute("size") != null && elem.getAttribute("size") != "") {
          tmpl.size = elem.getAttribute("size").toDouble()
        }
        tmpl.blendMode = BlendMode.Alpha
        if (elem.getAttribute("blend") != null && elem.getAttribute("blend") != "") {
          when {
            elem.getAttribute("blend").equals("alpha", ignoreCase = true) -> {
              tmpl.blendMode = BlendMode.Alpha
            }
            elem.getAttribute("blend").equals("additive", ignoreCase = true) -> {
              tmpl.blendMode = BlendMode.Additive
            }
            elem.getAttribute("blend").equals("multiply", ignoreCase = true) -> {
              tmpl.blendMode = BlendMode.Multiply
            }
            else -> {
              throw TemplateException("Unknown 'blend' value: " + elem.getAttribute("blend"))
            }
          }
        }
        if (elem.getAttribute("noisiness") != null && elem.getAttribute("noisiness") != "") {
          tmpl.noisiness = elem.getAttribute("noisiness").toDouble()
        }
        for (child in childElements(elem, null)) {
          tmpl.parameters.add(parseElement(child))
        }
      }

      @Throws(TemplateException::class)
      private fun parseStarTemplate(tmpl: StarTemplate, elem: Element) {
        parseInnerOuterTemplate(tmpl, elem)
        tmpl.numPoints = 5
        if (elem.getAttribute("points") != null && elem.getAttribute("points") != "") {
          tmpl.numPoints = elem.getAttribute("points").toInt()
        }
        tmpl.baseWidth = 1.0
        if (elem.getAttribute("baseWidth") != null && elem.getAttribute("baseWidth") != "") {
          tmpl.baseWidth = elem.getAttribute("baseWidth").toDouble()
        }
        tmpl.slope = 0.0
        if (elem.getAttribute("slope") != null && elem.getAttribute("slope") != "") {
          tmpl.slope = elem.getAttribute("slope").toDouble()
        }
      }
    }
  }

  class WarpTemplate : BaseTemplate() {
    enum class NoiseGenerator {
      Perlin, Spiral
    }

    var noiseGenerator: NoiseGenerator? = null
      private set
    var warpFactor = 0.0
      private set

    class WarpTemplateFactory : TemplateFactory() {
      /**
       * Parses a <warp> node and returns the corresponding \c ImageWarpTemplate.
      </warp> */
      @Throws(TemplateException::class)
      override fun parse(elem: Element): BaseTemplate {
        val tmpl = WarpTemplate()
        if (elem.getAttribute("generator") == "perlin-noise") {
          tmpl.noiseGenerator = NoiseGenerator.Perlin
        } else if (elem.getAttribute("generator") == "spiral") {
          tmpl.noiseGenerator = NoiseGenerator.Spiral
        } else {
          throw TemplateException("Unknown generator type: " + elem.getAttribute("generator"))
        }
        tmpl.warpFactor = 0.1
        if (elem.getAttribute("factor") != null && elem.getAttribute("factor").length > 0) {
          tmpl.warpFactor = elem.getAttribute("factor").toDouble()
        }
        for (child in childElements(elem, null)) {
          tmpl.parameters.add(parseElement(child))
        }
        return tmpl
      }
    }
  }

  companion object {
    /**
     * Parses the given string input and returns the \c Template it represents.
     */
    @Throws(TemplateException::class)
    fun parse(inp: InputStream?): Template {
      val builderFactory = DocumentBuilderFactory.newInstance()
      builderFactory.isValidating = false
      val xmldoc: Document
      xmldoc = try {
        val builder = builderFactory.newDocumentBuilder()
        builder.parse(inp)
      } catch (e: Exception) {
        throw TemplateException(e)
      }
      val tmpl = Template()
      val version = xmldoc.documentElement.getAttribute("version")
      if (version != null && version.length > 0) {
        tmpl.templateVersion = version.toInt()
      } else {
        tmpl.templateVersion = 1
      }
      tmpl.template = parseElement(xmldoc.documentElement)
      return tmpl
    }

    @Throws(TemplateException::class)
    private fun parseElement(elem: Element): BaseTemplate {
      val factory = when (elem.tagName) {
        "planets" -> {
          PlanetsTemplateFactory()
        }
        "planet" -> {
          PlanetTemplateFactory()
        }
        "texture" -> {
          TextureTemplateFactory()
        }
        "point-cloud" -> {
          PointCloudTemplateFactory()
        }
        "voronoi" -> {
          VoronoiTemplateFactory()
        }
        "colour" -> {
          ColourGradientTemplateFactory()
        }
        "perlin" -> {
          PerlinNoiseTemplateFactory()
        }
        "atmosphere" -> {
          AtmosphereTemplateFactory()
        }
        "warp" -> {
          WarpTemplateFactory()
        }
        else -> {
          throw TemplateException("Unknown element: " + elem.tagName)
        }
      }
      return factory.parse(elem)
    }
  }
}