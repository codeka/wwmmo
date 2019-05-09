package au.com.codeka.warworlds.planetrender.ui;

import au.com.codeka.warworlds.common.Colour;
import au.com.codeka.warworlds.common.Image;
import au.com.codeka.warworlds.common.PerlinNoise;
import au.com.codeka.warworlds.common.PointCloud;
import au.com.codeka.warworlds.common.Voronoi;
import au.com.codeka.warworlds.planetrender.PlanetRenderer;
import au.com.codeka.warworlds.planetrender.Template;
import au.com.codeka.warworlds.planetrender.TemplateException;
import au.com.codeka.warworlds.planetrender.TemplatedPerlinNoise;
import au.com.codeka.warworlds.planetrender.TemplatedPointCloud;
import au.com.codeka.warworlds.planetrender.TemplatedVoronoi;
import au.com.codeka.warworlds.planetrender.TextureGenerator;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;

/**
 * This is the shared implemented (between the applet and the Swing application). It's got all of
 * the main logic, etc.
 */
public class AppContent extends JPanel {
  private static final long serialVersionUID = 1L;

  private JLabel statusLabel;
  private JTextField seedText;
  private JCheckBox newSeedCheckBox;
  private JComboBox<String> imageSizeCombo;
  private ImagePanel contentPanel;
  private JTextArea templateXml;
  private JComboBox<String> backgroundColourCombo;

  public AppContent() {
    initialize();
  }

  /** Renders just the selected XML to the output image. */
  private void render() {
    Template.BaseTemplate tmpl;
    try {
      tmpl = getTemplate();
    } catch (TemplateException e) {
      statusLabel.setText("ERROR: " + e.getMessage());
      return;
    }

    long startTime = System.nanoTime();
    Image img = createBlankImage(Colour.TRANSPARENT);
    if (tmpl instanceof Template.PlanetTemplate) {
      PlanetRenderer pr = new PlanetRenderer((Template.PlanetTemplate) tmpl, getRandom());
      pr.render(img);
    } else if (tmpl instanceof Template.PlanetsTemplate) {
      PlanetRenderer pr = new PlanetRenderer((Template.PlanetsTemplate) tmpl, getRandom());
      pr.render(img);
    } else if (tmpl instanceof Template.TextureTemplate) {
      TextureGenerator texture = new TextureGenerator((Template.TextureTemplate) tmpl, getRandom());
      texture.renderTexture(img);
    } else if (tmpl instanceof Template.VoronoiTemplate) {
      Voronoi v = new TemplatedVoronoi((Template.VoronoiTemplate) tmpl, getRandom());
      v.renderDelaunay(img, Colour.GREEN);
      v.renderVoronoi(img, Colour.BLUE);
    } else if (tmpl instanceof Template.PointCloudTemplate) {
      PointCloud pc = new TemplatedPointCloud((Template.PointCloudTemplate) tmpl, getRandom());
      pc.render(img);
    } else if (tmpl instanceof Template.PerlinNoiseTemplate) {
      PerlinNoise pn = new TemplatedPerlinNoise((Template.PerlinNoiseTemplate) tmpl, getRandom());
      pn.render(img);
    } else {
      statusLabel.setText("Unknown template kind: " + tmpl.getClass().getName());
      return;
    }
    long endTime = System.nanoTime();

    String msg = String.format(
        Locale.US, "%.4fms elapsed.", ((double) (endTime - startTime)) / 1000000.0);
    statusLabel.setText(msg);

    contentPanel.setImage(img);
  }

  private Image createBlankImage(Colour baseColour) {
    String sizeStr = (String) imageSizeCombo.getSelectedItem();
    int size = Integer.parseInt(sizeStr);
    return new Image(size, size, baseColour);
  }

  /** Saves the current image we've rendered. */
  private void saveCurrentImage() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.showSaveDialog(this);

    File f = fileChooser.getSelectedFile();
    try {
      contentPanel.saveImageAs(f);
    } catch (IOException e) {
      JOptionPane.showMessageDialog(this, "An error occurred saving your image.", "Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private Template.BaseTemplate getTemplate() throws TemplateException {
    String xml = templateXml.getSelectedText();
    if (xml == null || xml.trim().equals("")) {
      xml = templateXml.getText();
    }
    Template tmpl;
    try {
      tmpl = Template.parse(new ByteArrayInputStream(xml.getBytes("utf-8")));
    } catch (UnsupportedEncodingException e) {
      throw new TemplateException("Unsupported encoding!");
    }

    return tmpl.getTemplate();
  }

  private Random getRandom() {
    if (newSeedCheckBox.isSelected() || seedText.getText().equals("")) {
      seedText.setText(Long.toString(new Random().nextLong()));
    }

    long seed = Long.parseLong(seedText.getText());
    return new Random(seed);
  }

  private void initialize() {
    setLayout(new BorderLayout(0, 0));

    JPanel panel = new JPanel();
    this.add(panel, BorderLayout.NORTH);
    panel.setLayout(new GoodFlowLayout(FlowLayout.LEADING, 0, 0));

    JToolBar toolBar = new JToolBar();
    panel.add(toolBar);

    toolBar.add(new JLabel("Seed "));

    seedText = new JTextField();
    toolBar.add(seedText);
    seedText.setColumns(10);

    newSeedCheckBox = new JCheckBox("New Seed");
    newSeedCheckBox.setSelected(true);
    toolBar.add(newSeedCheckBox);

    toolBar = new JToolBar();
    panel.add(toolBar);

    toolBar.add(new JLabel("Image Size "));

    imageSizeCombo = new JComboBox<>();
    imageSizeCombo.setModel(new DefaultComboBoxModel<>(
        new String[] {"16", "32", "64", "128", "256", "512", "1024", "2048", "4096"}));
    imageSizeCombo.setSelectedIndex(5);
    toolBar.add(imageSizeCombo);

    toolBar.add(new JLabel(" Background: "));

    backgroundColourCombo = new JComboBox<>();
    backgroundColourCombo.addActionListener(ae -> {
      String sel = (String) backgroundColourCombo.getSelectedItem();
      switch (sel) {
        case "Transparent":
          contentPanel.setBackgroundColour(null);
          break;
        case "Black":
          contentPanel.setBackgroundColour(Colour.BLACK);
          break;
        case "White":
          contentPanel.setBackgroundColour(Colour.WHITE);
          break;
      }
    });
    backgroundColourCombo.setModel(
        new DefaultComboBoxModel<>(new String[] {"Transparent", "Black", "White"}));
    toolBar.add(backgroundColourCombo);

    toolBar = new JToolBar();
    panel.add(toolBar);

    final JComboBox<String> templatesCombo = new JComboBox<>();
    templatesCombo.setModel(new DefaultComboBoxModel<>(getTemplateNames()));
    toolBar.add(templatesCombo);

    JButton btnLoadTemplate = new JButton("Load");
    btnLoadTemplate.addActionListener(ae -> {
      String tmpl = loadTemplate(templatesCombo.getItemAt(templatesCombo.getSelectedIndex()));
      templateXml.setText(tmpl);
      render();
    });
    toolBar.add(btnLoadTemplate);

    JButton btnRefresh = new JButton("Refresh");
    btnRefresh.addActionListener(ae -> render());
    toolBar.add(btnRefresh);

    JButton btnSaveButton = new JButton("Save Image...");
    btnSaveButton.addActionListener(ae -> saveCurrentImage());
    toolBar.add(btnSaveButton);

    panel = new JPanel();
    this.add(panel, BorderLayout.SOUTH);
    panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

    statusLabel = new JLabel(" ");
    panel.add(statusLabel);

    JSplitPane splitPane = new JSplitPane();
    this.add(splitPane, BorderLayout.CENTER);

    contentPanel = new ImagePanel();
    contentPanel.setMinimumSize(new Dimension(150, 150));
    contentPanel.setPreferredSize(new Dimension(550, 480));
    splitPane.setLeftComponent(contentPanel);

    templateXml = new JTextArea();
    templateXml.setMinimumSize(new Dimension(150, 150));
    JScrollPane sp = new JScrollPane(templateXml);
    splitPane.setRightComponent(sp);
    splitPane.setDividerLocation(0.666);
  }

  private String[] getTemplateNames() {
    File[] files = getSamplesDirectory().listFiles();
    if (files == null) {
      return new String[] {};
    }

    ArrayList<String> fileNames = new ArrayList<>();
    getTemplateNames(getSamplesDirectory(), "", fileNames);

    String[] array = new String[fileNames.size()];
    fileNames.toArray(array);
    Arrays.sort(array);
    return array;
  }

  private void getTemplateNames(File rootDir, String prefix, List<String> fileNames) {
    File[] files = rootDir.listFiles();
    if (files == null) {
      return;
    }

    for (File file : files) {
      if (file.isDirectory() && !file.getName().startsWith(".")) {
        getTemplateNames(file, prefix + file.getName() + File.separator, fileNames);
        continue;
      }

      String name = file.getName();
      if (!name.endsWith(".xml")) {
        continue;
      }

      fileNames.add(prefix + name);
    }
  }

  private String loadTemplate(String name) {
    File dir = getSamplesDirectory();
    File templateFile = new File(dir, name);
    try {
      return new String(Files.readAllBytes(Paths.get(templateFile.toString())),
          Charset.forName("utf-8"));
    } catch (IOException e) {
      return "";
    }
  }

  private File getSamplesDirectory() {
    return new File("renderer");
  }
}
