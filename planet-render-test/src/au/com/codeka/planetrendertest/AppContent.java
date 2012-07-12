package au.com.codeka.planetrendertest;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

import au.com.codeka.planetrender.*;

/**
 * This is the shared implemented (between the applet and the Swing application). It's got
 * all of the main logic, etc.
 */
public class AppContent extends JPanel {
    private static final long serialVersionUID = 1L;

    private JLabel mStatus;
    private JTextField txtSeed;
    private JCheckBox chckbxNewSeedCheckBox;
    private JComboBox cbbxImageSize;
    private JSplitPane mSplitPane;
    private ImagePanel mContentPanel;
    private JTextArea mTemplateXml;
    private JComboBox cbbxBackgroundColour;

    public AppContent() {
        initialize();
    }

    /**
     * Renders just the point cloud using the specified properties.
     */
    private void render() {
        Template.BaseTemplate tmpl;
        try {
            tmpl = getTemplate();
        } catch(TemplateException e) {
            mStatus.setText("ERROR: "+e.getMessage());
            return;
        }

        long startTime = System.nanoTime();
        Image img = createBlankImage(Colour.TRANSPARENT);
        if (tmpl instanceof Template.PlanetTemplate) {
            PlanetRenderer pr = new PlanetRenderer((Template.PlanetTemplate) tmpl, getRandom());
            pr.render(img);
        } else if (tmpl instanceof Template.TextureTemplate) {
            TextureGenerator texture = new TextureGenerator((Template.TextureTemplate) tmpl, getRandom());
            texture.renderTexture(img);
        } else if (tmpl instanceof Template.VoronoiTemplate) {
            Voronoi v = new Voronoi((Template.VoronoiTemplate) tmpl, getRandom());
            v.renderDelaunay(img, Colour.GREEN);
            v.renderVoronoi(img, Colour.BLUE);
        } else if (tmpl instanceof Template.PointCloudTemplate) {
            PointCloud pc = new PointCloud((Template.PointCloudTemplate) tmpl, getRandom());
            pc.render(img);
        } else if (tmpl instanceof Template.PerlinNoiseTemplate) {
            PerlinNoise pn = new PerlinNoise((Template.PerlinNoiseTemplate) tmpl, getRandom());
            pn.render(img);
        } else {
            mStatus.setText("Unknown template kind: "+tmpl.getClass().getName());
            return;
        }
        long endTime = System.nanoTime();

        String msg = String.format("%.4fms elapsed.",
                ((double) (endTime - startTime)) / 1000000.0);
        mStatus.setText(msg);

        mContentPanel.setImage(img);
    }

    private Image createBlankImage(Colour baseColour) {
        String sizeStr = (String) cbbxImageSize.getSelectedItem();
        int size = Integer.parseInt(sizeStr);
        return new Image(size, size, baseColour);
    }

    /**
     * Saves the current image we've rendered.
     */
    private void saveCurrentImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.showSaveDialog(this);

        File f = fileChooser.getSelectedFile();
        try {
            mContentPanel.saveImageAs(f);
        } catch(IOException e) {
            JOptionPane.showMessageDialog(this, "An error occured saving your image.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Template.BaseTemplate getTemplate() throws TemplateException {
        String xml = mTemplateXml.getSelectedText();
        if (xml == null || xml.trim().equals("")) {
            xml = mTemplateXml.getText();
        }
        Template tmpl = null;

        try {
            tmpl = Template.parse(new ByteArrayInputStream(xml.getBytes("utf-8")));
        } catch (UnsupportedEncodingException e) {
            throw new TemplateException("Unsupported encoding!");
        }

        return tmpl.getTemplate();
    }

    private Random getRandom() {
        if (chckbxNewSeedCheckBox.isSelected() || txtSeed.getText().equals("")) {
            txtSeed.setText(Long.toString(new Random().nextLong()));
        }

        long seed = Long.parseLong(txtSeed.getText());
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

        txtSeed = new JTextField();
        toolBar.add(txtSeed);
        txtSeed.setColumns(10);

        chckbxNewSeedCheckBox = new JCheckBox("New Seed");
        chckbxNewSeedCheckBox.setSelected(true);
        toolBar.add(chckbxNewSeedCheckBox);

        toolBar = new JToolBar();
        panel.add(toolBar);

        toolBar.add(new JLabel("Image Size "));

        cbbxImageSize = new JComboBox();
        cbbxImageSize.setModel(new DefaultComboBoxModel(new String[] {"16", "32", "64", "128", "256", "512", "1024", "2048", "4096"}));
        cbbxImageSize.setSelectedIndex(5);
        toolBar.add(cbbxImageSize);

        toolBar.add(new JLabel(" Background: "));

        cbbxBackgroundColour = new JComboBox();
        cbbxBackgroundColour.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String sel = (String) cbbxBackgroundColour.getSelectedItem();
                if (sel.equals("Transparent")) {
                    mContentPanel.setBackgroundColour(null);
                } else if (sel.equals("Black")) {
                    mContentPanel.setBackgroundColour(Colour.BLACK);
                } else if (sel.equals("White")) {
                    mContentPanel.setBackgroundColour(Colour.WHITE);
                }
            }
        });
        cbbxBackgroundColour.setModel(new DefaultComboBoxModel(new String[] {"Transparent", "Black", "White"}));
        toolBar.add(cbbxBackgroundColour);

        toolBar = new JToolBar();
        panel.add(toolBar);

        final JComboBox cbbxTemplates = new JComboBox();
        cbbxTemplates.setModel(new DefaultComboBoxModel(new String[] {
                "Gas Giant", "Inferno", "Radiated", "Terran", "Toxic"
            }));
        toolBar.add(cbbxTemplates);

        JButton btnLoadTemplate = new JButton("Load");
        btnLoadTemplate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String tmpl = TEMPLATES[cbbxTemplates.getSelectedIndex()];
                mTemplateXml.setText(tmpl);
                render();
            }
        });
        toolBar.add(btnLoadTemplate);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                render();
            }
        });
        toolBar.add(btnRefresh);

        JButton btnSaveButton = new JButton("Save Image...");
        btnSaveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                saveCurrentImage();
            }
        });
        toolBar.add(btnSaveButton);

        panel = new JPanel();
        this.add(panel, BorderLayout.SOUTH);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        mStatus = new JLabel(" ");
        panel.add(mStatus);

        mSplitPane = new JSplitPane();
        this.add(mSplitPane, BorderLayout.CENTER);

        mContentPanel = new ImagePanel();
        mContentPanel.setMinimumSize(new Dimension(150, 150));
        mContentPanel.setPreferredSize(new Dimension(550, 480));
        mSplitPane.setLeftComponent(mContentPanel);

        mTemplateXml = new JTextArea();
        mTemplateXml.setMinimumSize(new Dimension(150, 150));
        JScrollPane sp = new JScrollPane(mTemplateXml);
        mSplitPane.setRightComponent(sp);
        mSplitPane.setDividerLocation(0.666);
    }

    // these are in the same order as the combo box cbbxTemplates...
    private static final String[] TEMPLATES = {
            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" +
            "<planet version=\"1\" northFrom=\"-0.7 1 0\" northTo=\"0.7 1 0\">\r\n" +
            "  <texture generator=\"voronoi-map\" noisiness=\"0.8\" scaleX=\"0.15\">\r\n" +
            "    <voronoi>\r\n" +
            "      <point-cloud generator=\"poisson\" density=\"0.2\" randomness=\"0.8\" />\r\n" +
            "    </voronoi>\r\n" +
            "    <colour>\r\n" +
            "      <node n=\"0.0\" colour=\"ff252baa\" />\r\n" +
            "      <node n=\"0.3\" colour=\"ff494a9d\" />\r\n" +
            "      <node n=\"0.5\" colour=\"ff0a739d\" />\r\n" +
            "      <node n=\"0.8\" colour=\"ff0a4e9d\" />\r\n" +
            "      <node n=\"1.0\" colour=\"ff0aae9d\" />\r\n" +
            "    </colour>\r\n" +
            "    <perlin persistence=\"0.5\" startOctave=\"6\" endOctave=\"8\" interpolation=\"linear\" />\r\n" +
            "  </texture>\r\n" +
            "</planet>",

            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" +
            "<planet version=\"1\" ambient=\"0.4\">\r\n" +
            "  <texture generator=\"voronoi-map\" noisiness=\"0.8\" scaleX=\"1.2\" scaleY=\"0.6\">\r\n" +
            "    <voronoi>\r\n" +
            "      <point-cloud generator=\"poisson\" density=\"0.2\" randomness=\"0.8\" />\r\n" +
            "    </voronoi>\r\n" +
            "    <colour>\r\n" +
            "      <node n=\"0.0\" colour=\"ff600000\" />\r\n" +
            "      <node n=\"0.8\" colour=\"ffb00000\" />\r\n" +
            "      <node n=\"1.0\" colour=\"ffffff00\" />\r\n" +
            "    </colour>\r\n" +
            "    <perlin persistence=\"0.5\" startOctave=\"6\" endOctave=\"8\" interpolation=\"linear\" />\r\n" +
            "  </texture>\r\n" +
            "  <atmosphere>\r\n" +
            "    <outer size=\"6\" noisiness=\"0.4\">\r\n" +
            "      <colour>\r\n" +
            "        <node n=\"0.0\" colour=\"ffff0000\" />\r\n" +
            "        <node n=\"0.8\" colour=\"00ff0000\" />\r\n" +
            "      </colour>\r\n" +
            "      <perlin persistence=\"0.5\" startOctave=\"3\" endOctave=\"6\" interpolation=\"linear\" />\r\n" +
            "    </outer>\r\n" +
            "    <inner>\r\n" +
            "      <colour>\r\n" +
            "        <node n=\"0.0\" colour=\"00ff0000\" />\r\n" +
            "        <node n=\"0.6\" colour=\"a0ff0000\" />\r\n" +
            "      </colour>\r\n" +
            "    </inner>\r\n" +
            "  </atmosphere>\r\n" +
            "</planet>",

            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" +
            "<planet version=\"1\">\r\n" +
            "  <texture generator=\"voronoi-map\" scaleX=\"1.2\">\r\n" +
            "      <voronoi>\r\n" +
            "        <point-cloud generator=\"poisson\" density=\"0.2\" randomness=\"0.8\" />\r\n" +
            "      </voronoi>\r\n" +
            "      <perlin persistence=\"0.5\" startOctave=\"7\" endOctave=\"16\" interpolation=\"cosine\" />\r\n" +
            "      <colour>\r\n" +
            "        <node n=\"0.0\" colour=\"ffff004d\" />\r\n" +
            "        <node n=\"0.3\" colour=\"ff5600ff\" />\r\n" +
            "        <node n=\"0.6\" colour=\"ffff00ff\" />\r\n" +
            "        <node n=\"1.0\" colour=\"ff5600ff\" />\r\n" +
            "      </colour>\r\n" +
            "  </texture>\r\n" +
            "  <atmosphere>\r\n" +
            "    <outer size=\"8\" noisiness=\"0.7\">\r\n" +
            "      <colour>\r\n" +
            "        <node n=\"0.0\" colour=\"ffff00ff\" />\r\n" +
            "        <node n=\"0.8\" colour=\"00ff00ff\" />\r\n" +
            "      </colour>\r\n" +
            "      <perlin persistence=\"0.6\" startOctave=\"3\" endOctave=\"8\" interpolation=\"linear\" />\r\n" +
            "    </outer>\r\n" +
            "    <inner sunStartShadow=\"1\" sunShadowFactor=\"0.3\">\r\n" +
            "      <colour>\r\n" +
            "        <node n=\"0.0\" colour=\"00ff00ff\" />\r\n" +
            "        <node n=\"0.6\" colour=\"60ff00ff\" />\r\n" +
            "      </colour>\r\n" +
            "    </inner>\r\n" +
            "  </atmosphere>\r\n" +
            "</planet>",

            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" +
            "<planet version=\"1\">\r\n" +
            "  <texture generator=\"perlin-noise\" scaleX=\"2\">\r\n" +
            "    <colour>\r\n" +
            "      <node n=\"0.0\" colour=\"ff066b8d\" />\r\n" +
            "      <node n=\"0.2\" colour=\"ff06418d\" />\r\n" +
            "      <node n=\"0.5\" colour=\"ff0855b8\" />\r\n" +
            "      <node n=\"0.5001\" colour=\"ff21972c\" />\r\n" +
            "      <node n=\"1.0\" colour=\"ff08550b\" />\r\n" +
            "    </colour>\r\n" +
            "    <perlin persistence=\"0.5\" startOctave=\"2\" endOctave=\"4\" interpolation=\"linear\" />\r\n" +
            "  </texture>\r\n" +
            "  <atmosphere>\r\n" +
            "    <inner sunStartShadow=\"1.2\" sunShadowFactor=\"0.8\">\r\n" +
            "      <colour>\r\n" +
            "        <node n=\"0.0\" colour=\"00000000\" />\r\n" +
            "        <node n=\"0.4\" colour=\"000054c1\" />\r\n" +
            "        <node n=\"1.0\" colour=\"ff0054c1\" />\r\n" +
            "      </colour>\r\n" +
            "    </inner>\r\n" +
            "    <outer size=\"2\" sunStartShadow=\"1\" sunShadowFactor=\"0.5\">\r\n" +
            "      <colour>\r\n" +
            "        <node n=\"0.0\" colour=\"ff0054c1\" />\r\n" +
            "        <node n=\"0.8\" colour=\"000054c1\" />\r\n" +
            "        <node n=\"1.0\" colour=\"00000000\" />\r\n" +
            "      </colour>\r\n" +
            "    </outer>\r\n" +
            "  </atmosphere>\r\n" +
            "</planet>",

            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" +
            "<planet version=\"1\">\r\n" +
            "  <texture generator=\"perlin-noise\">\r\n" +
            "      <perlin persistence=\"0.5\" startOctave=\"7\" endOctave=\"9\" interpolation=\"linear\" />\r\n" +
            "      <colour>\r\n" +
            "        <node n=\"0.0\" colour=\"ff006006\" />\r\n" +
            "        <node n=\"0.3\" colour=\"ff002960\" />\r\n" +
            "        <node n=\"1.0\" colour=\"ff00ff06\" />\r\n" +
            "      </colour>\r\n" +
            "  </texture>\r\n" +
            "  <atmosphere>\r\n" +
            "    <outer size=\"5\" noisiness=\"0.7\" sunStartShadow=\"1\" sunShadowFactor=\"0.6\">\r\n" +
            "      <colour>\r\n" +
            "        <node n=\"0.0\" colour=\"ff24ff60\" />\r\n" +
            "        <node n=\"0.8\" colour=\"0024ff60\" />\r\n" +
            "      </colour>\r\n" +
            "      <perlin persistence=\"0.6\" startOctave=\"3\" endOctave=\"6\" interpolation=\"linear\" />\r\n" +
            "    </outer>\r\n" +
            "  </atmosphere>\r\n" +
            "</planet>"
        };
}
