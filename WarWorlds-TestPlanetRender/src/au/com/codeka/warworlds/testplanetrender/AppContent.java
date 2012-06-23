package au.com.codeka.warworlds.testplanetrender;

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
        Template.BaseTemplate tmpl = getTemplate();

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

    private Template.BaseTemplate getTemplate() {
        String xml = mTemplateXml.getSelectedText();
        if (xml == null || xml.trim().equals("")) {
            xml = mTemplateXml.getText();
        }
        Template tmpl = null;

        try {
            tmpl = Template.parse(new ByteArrayInputStream(xml.getBytes("utf-8")));
        } catch (TemplateException e) {
            // TODO Auto-generated catch block
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
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

        JLabel lblSeed = new JLabel("Seed ");
        toolBar.add(lblSeed);

        txtSeed = new JTextField();
        toolBar.add(txtSeed);
        txtSeed.setColumns(10);

        chckbxNewSeedCheckBox = new JCheckBox("New Seed");
        toolBar.add(chckbxNewSeedCheckBox);
        
        JToolBar toolBar_3 = new JToolBar();
        panel.add(toolBar_3);
        
        JLabel lblImageSize = new JLabel("Image Size ");
        toolBar_3.add(lblImageSize);
        
        cbbxImageSize = new JComboBox();
        cbbxImageSize.setModel(new DefaultComboBoxModel(new String[] {"16", "32", "64", "128", "256", "512", "1024", "2048", "4096"}));
        cbbxImageSize.setSelectedIndex(5);
        toolBar_3.add(cbbxImageSize);
        
        JLabel lblBackground = new JLabel(" Background: ");
        toolBar_3.add(lblBackground);
        
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
        toolBar_3.add(cbbxBackgroundColour);

        JToolBar toolBar_1 = new JToolBar();
        panel.add(toolBar_1);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                render();
            }
        });
        toolBar_1.add(btnRefresh);
        
        JButton btnSaveButton = new JButton("Save Image...");
        btnSaveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                saveCurrentImage();
            }
        });
        toolBar_1.add(btnSaveButton);

        panel = new JPanel();
        this.add(panel, BorderLayout.SOUTH);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        mStatus = new JLabel(" ");
        panel.add(mStatus);
        
        mSplitPane = new JSplitPane();
        this.add(mSplitPane, BorderLayout.CENTER);
        
        mContentPanel = new ImagePanel();
        mContentPanel.setMinimumSize(new Dimension(150, 150));
        mContentPanel.setPreferredSize(new Dimension(640, 480));
        mSplitPane.setLeftComponent(mContentPanel);
        
        mTemplateXml = new JTextArea();
        mTemplateXml.setMinimumSize(new Dimension(150, 150));
        mSplitPane.setRightComponent(mTemplateXml);
        mSplitPane.setDividerLocation(0.666);
    }
}
