package au.com.codeka.warworlds.testplanetrender;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import au.com.codeka.planetrender.Colour;
import au.com.codeka.planetrender.PlanetRenderer;
import au.com.codeka.planetrender.PlanetTemplate;
import au.com.codeka.planetrender.PointCloud;
import au.com.codeka.planetrender.Image;
import au.com.codeka.planetrender.Voroni;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class MainWindow {

    private JFrame mFrame;
    private ImagePanel mImagePanel;
    private JLabel mStatus;
    private JComboBox mPointCloudGenerator;
    private JTextField txtPointCloudDensity;
    private JButton btnPointCloudRender;
    private JTextField txtSeed;
    private JCheckBox chckbxNewSeedCheckBox;
    private JTextField txtPointCloudRandomness;
    private JButton btnSaveButton;
    private JToolBar toolBar_1;
    private JCheckBox chckbxRenderDelaunay;
    private JToolBar toolBar_2;

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
    }

    private void refreshImage() {
        Image img = new Image(512, 512);

        long startTime = System.nanoTime();
        PlanetRenderer renderer = new PlanetRenderer();
        renderer.render(img);
        long endTime = System.nanoTime();

        mImagePanel.setImage(img);
        mStatus.setText(String.format("Image rendered in %.4fms", ((double) endTime - startTime) / 1000000.0));
    }

    /**
     * Renders just the point cloud using the specified properties.
     */
    private void renderPointCloud() {
        PlanetTemplate tmpl = getTemplate();
        String msg = "";

        long startTime = System.nanoTime();
        PointCloud pc = PointCloud.generate(tmpl);
        long endTime = System.nanoTime();
        long elapsed = endTime - startTime;
        long totalElapsed = elapsed;

        Image img = new Image(512, 512, Colour.TRANSPARENT);
        msg = String.format("Point cloud generated in: %.4fms", ((double) elapsed) / 1000000.0);

        if (chckbxRenderDelaunay.isSelected()) {
            Voroni v = new Voroni(pc);

            startTime = System.nanoTime();
            v.generate();
            endTime = System.nanoTime();
            elapsed = endTime - startTime;
            totalElapsed += elapsed;

            msg += String.format("; triangulated in: %.4fms", ((double) elapsed) / 1000000.0);
            v.renderDelaunay(img, Colour.BLACK);
        }

        // render the point cloud last, so the dots are on top
        pc.render(img);
        mImagePanel.setImage(img);

        msg += String.format("; total elapsed: %.4fms", ((double) totalElapsed) / 1000000.0);
        mStatus.setText(msg);
    }

    /**
     * Saves the current image we've rendered.
     */
    private void saveCurrentImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.showSaveDialog(mFrame);

        File f = fileChooser.getSelectedFile();
        try {
            mImagePanel.saveImageAs(f);
        } catch(IOException e) {
            JOptionPane.showMessageDialog(mFrame, "An error occured saving your image.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private PlanetTemplate getTemplate() {
        if (chckbxNewSeedCheckBox.isSelected()) {
            long seed = new Random().nextLong();
            txtSeed.setText(Long.toString(seed));
        }

        PlanetTemplate tmpl = new PlanetTemplate()
            .setRandomSeed(Long.parseLong(txtSeed.getText()))
            .setPointCloudDensity(Double.parseDouble(txtPointCloudDensity.getText()))
            .setPointCloudGenerator((PlanetTemplate.PointCloudGenerator) mPointCloudGenerator.getSelectedItem())
            .setPointCloudRandomness(Double.parseDouble(txtPointCloudRandomness.getText()));

        return tmpl;
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        mFrame = new JFrame();
        mFrame.setTitle("Planet Render Test");
        mFrame.setBounds(100, 100, 900, 700);
        mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mFrame.getContentPane().setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(10, 64));
        mFrame.getContentPane().add(panel, BorderLayout.NORTH);
        panel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        JToolBar toolBar = new JToolBar();
        panel.add(toolBar);

        JLabel lblSeed = new JLabel("Seed ");
        toolBar.add(lblSeed);

        txtSeed = new JTextField();
        toolBar.add(txtSeed);
        txtSeed.setColumns(10);

        chckbxNewSeedCheckBox = new JCheckBox("New Seed");
        toolBar.add(chckbxNewSeedCheckBox);

        toolBar_2 = new JToolBar();
        panel.add(toolBar_2);

        JLabel lblPointCloud = new JLabel("Point Cloud ");
        toolBar_2.add(lblPointCloud);

        mPointCloudGenerator = new JComboBox(PlanetTemplate.PointCloudGenerator.values());
        toolBar_2.add(mPointCloudGenerator);

        JLabel lblDensity = new JLabel(" Density ");
        toolBar_2.add(lblDensity);

        txtPointCloudDensity = new JTextField();
        toolBar_2.add(txtPointCloudDensity);
        txtPointCloudDensity.setColumns(5);

        btnPointCloudRender = new JButton("Render");
        btnPointCloudRender.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                renderPointCloud();
            }
        });
        
        JLabel lblRandomness = new JLabel(" Randomness ");
        toolBar_2.add(lblRandomness);
        
        txtPointCloudRandomness = new JTextField();
        txtPointCloudRandomness.setText("PointCloudRandomness");
        toolBar_2.add(txtPointCloudRandomness);
        txtPointCloudRandomness.setColumns(5);
        
        chckbxRenderDelaunay = new JCheckBox("Delaunay ");
        toolBar_2.add(chckbxRenderDelaunay);
        toolBar_2.add(btnPointCloudRender);

        toolBar_1 = new JToolBar();
        panel.add(toolBar_1);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                refreshImage();
            }
        });
        toolBar_1.add(btnRefresh);
        
        btnSaveButton = new JButton("Save Image...");
        btnSaveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                saveCurrentImage();
            }
        });
        toolBar_1.add(btnSaveButton);

        mImagePanel = new ImagePanel();
        mFrame.getContentPane().add(mImagePanel, BorderLayout.CENTER);

        panel = new JPanel();
        mFrame.getContentPane().add(panel, BorderLayout.SOUTH);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        mStatus = new JLabel("Please wait...");
        panel.add(mStatus);

        // set up the default values based on an empty template
        PlanetTemplate tmpl = new PlanetTemplate();
        txtSeed.setText("0");
        chckbxNewSeedCheckBox.setSelected(true);
        txtPointCloudDensity.setText(String.format("%.3f", tmpl.getPointCloudDensity()));
        mPointCloudGenerator.setSelectedItem(tmpl.getPointCloudGenerator());
        txtPointCloudRandomness.setText(String.format("%.3f", tmpl.getPointCloudRandomness()));
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
            Rectangle bounds = g.getClipBounds();

            // fill the entire background graw
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

            if (mImage != null) {
                bounds.x += (bounds.width - mImageWidth) / 2;
                bounds.y += (bounds.height - mImageHeight) / 2;
                bounds.width = mImageWidth;
                bounds.height = mImageHeight;

                // we'll draw a checkboard background to represent the transparent parts of the image
                g.setColor(Color.GRAY);
                boolean odd = false;
                for (int y = bounds.y; y < bounds.y + bounds.height; y += 16) {
                    int xOffset = (odd ? 16 : 0);
                    odd = !odd;
                    for (int x = bounds.x + xOffset; x < bounds.x + bounds.width; x += 32) {
                        g.fillRect(x, y, 16, 16);
                    }
                }

                g.drawImage(mImage, bounds.x, bounds.y, null);
            }
        }
    }
}
