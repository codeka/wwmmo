package au.com.codeka.warworlds.planetrender.ui

import au.com.codeka.warworlds.common.*
import au.com.codeka.warworlds.common.Colour.Companion.BLACK
import au.com.codeka.warworlds.common.Colour.Companion.BLUE
import au.com.codeka.warworlds.common.Colour.Companion.GREEN
import au.com.codeka.warworlds.common.Colour.Companion.TRANSPARENT
import au.com.codeka.warworlds.common.Colour.Companion.WHITE
import au.com.codeka.warworlds.planetrender.*
import au.com.codeka.warworlds.planetrender.Template.*
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.swing.*

/**
 * This is the shared implemented (between the applet and the Swing application). It's got all of
 * the main logic, etc.
 */
class AppContent : JPanel() {
  private var statusLabel: JLabel? = null
  private var seedText: JTextField? = null
  private var newSeedCheckBox: JCheckBox? = null
  private var imageSizeCombo: JComboBox<String>? = null
  private var contentPanel: ImagePanel? = null
  private var templateXml: JTextArea? = null
  private var backgroundColourCombo: JComboBox<String>? = null

  /** Renders just the selected XML to the output image.  */
  private fun render() {
    val tmpl: BaseTemplate?
    try {
      tmpl = template
    } catch (e: TemplateException) {
      statusLabel!!.text = "ERROR: " + e.message
      return
    }
    val startTime = System.nanoTime()
    val img = createBlankImage(TRANSPARENT)
    if (tmpl is PlanetTemplate) {
      val pr = PlanetRenderer(tmpl, random)
      pr.render(img)
    } else if (tmpl is PlanetsTemplate) {
      val pr = PlanetRenderer(tmpl, random)
      pr.render(img)
    } else if (tmpl is TextureTemplate) {
      val texture = TextureGenerator(tmpl, random)
      texture.renderTexture(img)
    } else if (tmpl is VoronoiTemplate) {
      val v: Voronoi = TemplatedVoronoi(tmpl, random)
      v.renderDelaunay(img, GREEN)
      v.renderVoronoi(img, BLUE)
    } else if (tmpl is PointCloudTemplate) {
      val pc: PointCloud = TemplatedPointCloud(tmpl, random)
      pc.render(img)
    } else if (tmpl is PerlinNoiseTemplate) {
      val pn: PerlinNoise = TemplatedPerlinNoise(tmpl, random)
      pn.render(img)
    } else {
      statusLabel!!.text = "Unknown template kind: " + tmpl!!.javaClass.name
      return
    }
    val endTime = System.nanoTime()
    val msg = String.format(
        Locale.US, "%.4fms elapsed.", (endTime - startTime).toDouble() / 1000000.0)
    statusLabel!!.text = msg
    contentPanel!!.setImage(img)
  }

  private fun createBlankImage(baseColour: Colour): Image {
    val sizeStr = imageSizeCombo!!.selectedItem as String
    val size = sizeStr.toInt()
    return Image(size, size, baseColour)
  }

  /** Saves the current image we've rendered.  */
  private fun saveCurrentImage() {
    val fileChooser = JFileChooser()
    fileChooser.showSaveDialog(this)
    val f = fileChooser.selectedFile
    try {
      contentPanel!!.saveImageAs(f)
    } catch (e: IOException) {
      JOptionPane.showMessageDialog(this, "An error occurred saving your image.", "Error",
          JOptionPane.ERROR_MESSAGE)
    }
  }

  private val template: BaseTemplate?
    get() {
      var xml = templateXml!!.selectedText
      if (xml == null || xml.trim { it <= ' ' } == "") {
        xml = templateXml!!.text
      }
      val tmpl: Template
      tmpl = try {
        Template.parse(ByteArrayInputStream(xml!!.toByteArray(charset("utf-8"))))
      } catch (e: UnsupportedEncodingException) {
        throw TemplateException("Unsupported encoding!")
      }
      return tmpl.template
    }

  private val random: Random
    get() {
      if (newSeedCheckBox!!.isSelected || seedText!!.text == "") {
        seedText!!.text = Random().nextLong().toString()
      }
      val seed = seedText!!.text.toLong()
      return Random(seed)
    }

  private fun initialize() {
    layout = BorderLayout(0, 0)
    var panel = JPanel()
    this.add(panel, BorderLayout.NORTH)
    panel.layout = GoodFlowLayout(FlowLayout.LEADING, 0, 0)
    var toolBar = JToolBar()
    panel.add(toolBar)
    toolBar.add(JLabel("Seed "))
    seedText = JTextField()
    toolBar.add(seedText)
    seedText!!.columns = 10
    newSeedCheckBox = JCheckBox("New Seed")
    newSeedCheckBox!!.isSelected = true
    toolBar.add(newSeedCheckBox)
    toolBar = JToolBar()
    panel.add(toolBar)
    toolBar.add(JLabel("Image Size "))
    imageSizeCombo = JComboBox()
    imageSizeCombo!!.model =
        DefaultComboBoxModel(arrayOf("16", "32", "64", "128", "256", "512", "1024", "2048", "4096"))
    imageSizeCombo!!.selectedIndex = 5
    toolBar.add(imageSizeCombo)
    toolBar.add(JLabel(" Background: "))
    backgroundColourCombo = JComboBox()
    backgroundColourCombo!!.addActionListener {
      when (backgroundColourCombo!!.selectedItem as String) {
        "Transparent" -> contentPanel!!.setBackgroundColour(null)
        "Black" -> contentPanel!!.setBackgroundColour(BLACK)
        "White" -> contentPanel!!.setBackgroundColour(WHITE)
      }
    }
    backgroundColourCombo!!.model = DefaultComboBoxModel(arrayOf("Transparent", "Black", "White"))
    toolBar.add(backgroundColourCombo)
    toolBar = JToolBar()
    panel.add(toolBar)
    val templatesCombo = JComboBox<String?>()
    templatesCombo.model = DefaultComboBoxModel(templateNames)
    toolBar.add(templatesCombo)
    val btnLoadTemplate = JButton("Load")
    btnLoadTemplate.addActionListener {
      val tmpl = loadTemplate(templatesCombo.getItemAt(templatesCombo.selectedIndex)!!)
      templateXml!!.text = tmpl
      render()
    }
    toolBar.add(btnLoadTemplate)
    val btnRefresh = JButton("Refresh")
    btnRefresh.addActionListener { render() }
    toolBar.add(btnRefresh)
    val btnSaveButton = JButton("Save Image...")
    btnSaveButton.addActionListener { saveCurrentImage() }
    toolBar.add(btnSaveButton)
    panel = JPanel()
    this.add(panel, BorderLayout.SOUTH)
    panel.layout = FlowLayout(FlowLayout.LEFT, 5, 5)
    statusLabel = JLabel(" ")
    panel.add(statusLabel)
    val splitPane = JSplitPane()
    this.add(splitPane, BorderLayout.CENTER)
    contentPanel = ImagePanel()
    contentPanel!!.minimumSize = Dimension(150, 150)
    contentPanel!!.preferredSize = Dimension(550, 480)
    splitPane.leftComponent = contentPanel
    templateXml = JTextArea()
    templateXml!!.minimumSize = Dimension(150, 150)
    val sp = JScrollPane(templateXml)
    splitPane.rightComponent = sp
    splitPane.setDividerLocation(0.666)
  }

  private val templateNames: Array<String?>
    get() {
      val fileNames = ArrayList<String>()
      getTemplateNames(samplesDirectory, "", fileNames)
      val array = arrayOfNulls<String>(fileNames.size)
      fileNames.toArray(array)
      Arrays.sort(array)
      return array
    }

  private fun getTemplateNames(rootDir: File, prefix: String, fileNames: MutableList<String>) {
    val files = rootDir.listFiles() ?: return
    for (file in files) {
      if (file.isDirectory && !file.name.startsWith(".")) {
        getTemplateNames(file, prefix + file.name + File.separator, fileNames)
        continue
      }
      val name = file.name
      if (!name.endsWith(".xml")) {
        continue
      }
      fileNames.add(prefix + name)
    }
  }

  private fun loadTemplate(name: String): String {
    val dir = samplesDirectory
    val templateFile = File(dir, name)
    return try {
      String(Files.readAllBytes(Paths.get(templateFile.toString())),
          Charset.forName("utf-8"))
    } catch (e: IOException) {
      ""
    }
  }

  private val samplesDirectory: File
    get() = File("renderer")

  companion object {
    private const val serialVersionUID = 1L
  }

  init {
    initialize()
  }
}