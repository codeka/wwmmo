package au.com.codeka.warworlds.testplanetrender;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import au.com.codeka.planetrender.Colour;
import au.com.codeka.planetrender.Image;
import au.com.codeka.planetrender.PerlinNoise;
import au.com.codeka.planetrender.PlanetRenderer;
import au.com.codeka.planetrender.PointCloud;
import au.com.codeka.planetrender.Template;
import au.com.codeka.planetrender.TemplateException;
import au.com.codeka.planetrender.TextureGenerator;
import au.com.codeka.planetrender.Voronoi;

public class MainWindow {

    private JFrame mFrame;
    private JLabel mStatus;
    private JTextField txtSeed;
    private JCheckBox chckbxNewSeedCheckBox;
    private JComboBox cbbxImageSize;
    private JSplitPane mSplitPane;
    private ImagePanel mContentPanel;
    private JTextArea mTemplateXml;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainWindow window = new MainWindow();
                    window.mFrame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public MainWindow() {
        initialize();

        mSplitPane.setDividerLocation(0.666);
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

        String msg = String.format("%.4fms elapsed.", ((double) (endTime - startTime)) / 1000000.0);
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
        fileChooser.showSaveDialog(mFrame);

        File f = fileChooser.getSelectedFile();
        try {
            mContentPanel.saveImageAs(f);
        } catch(IOException e) {
            JOptionPane.showMessageDialog(mFrame, "An error occured saving your image.", 
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

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        mFrame = new JFrame();
        mFrame.setTitle("Planet Render Test");
        mFrame.setBounds(100, 100, 1200, 700);
        mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mFrame.getContentPane().setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        mFrame.getContentPane().add(panel, BorderLayout.NORTH);
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
        mFrame.getContentPane().add(panel, BorderLayout.SOUTH);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        mStatus = new JLabel(" ");
        panel.add(mStatus);
        
        mSplitPane = new JSplitPane();
        mFrame.getContentPane().add(mSplitPane, BorderLayout.CENTER);
        
        mContentPanel = new ImagePanel();
        mContentPanel.setMinimumSize(new Dimension(150, 150));
        mContentPanel.setPreferredSize(new Dimension(640, 480));
        mSplitPane.setLeftComponent(mContentPanel);
        
        mTemplateXml = new JTextArea();
        mTemplateXml.setMinimumSize(new Dimension(150, 150));
        mSplitPane.setRightComponent(mTemplateXml);
    }

    private class ImagePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private java.awt.Image mImage;
        private int mImageWidth;
        private int mImageHeight;

        public void setImage(Image img) {
            MemoryImageSource mis = new MemoryImageSource(img.getWidth(), img.getHeight(),
                                                          img.getArgb(), 0, img.getWidth());
            mImage = mFrame.createImage(mis);
            mImageWidth = img.getWidth();
            mImageHeight = img.getHeight();
            repaint();
        }

        public void saveImageAs(File f) throws IOException {
            BufferedImage img = new BufferedImage(mImageWidth, mImageHeight,
                                                  BufferedImage.TYPE_INT_ARGB);
            Graphics g = img.getGraphics();
            g.drawImage(mImage, 0, 0, this);
            g.dispose();

            ImageIO.write(img, "png", f);
        }

        @Override
        public void paintComponent(Graphics g) {
            int width = getWidth();
            int height = getHeight();

            // fill the entire background graw
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, width, height);

            if (mImage != null) {
                int sx = (width - mImageWidth) / 2;
                int sy = (height - mImageHeight) / 2;
                width = mImageWidth;
                height = mImageHeight;

                // we'll draw a checkboard background to represent the transparent parts of the image
                g.setColor(Color.GRAY);
                boolean odd = false;
                for (int y = sy; y < sy + height; y += 16) {
                    int xOffset = (odd ? 16 : 0);
                    odd = !odd;
                    for (int x = sx + xOffset; x < sx + width; x += 32) {
                        g.fillRect(x, y, 16, 16);
                    }
                }

                g.drawImage(mImage, sx, sy, null);
            }
        }
    }
}
