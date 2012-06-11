package au.com.codeka.warworlds.testplanetrender;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.MemoryImageSource;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import au.com.codeka.planetrender.PlanetRenderer;

public class MainWindow {

    private JFrame mFrame;
    private ImagePanel mImagePanel;
    private JLabel mStatus;

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
        au.com.codeka.planetrender.Image img = new au.com.codeka.planetrender.Image(512, 512);

        long startTime = System.nanoTime();
        PlanetRenderer renderer = new PlanetRenderer();
        renderer.render(img);
        long endTime = System.nanoTime();

        MemoryImageSource mis = new MemoryImageSource(img.getWidth(), img.getHeight(), img.getArgb(), 0, img.getWidth());
        mImagePanel.setImage(mFrame.createImage(mis));

        mStatus.setText(String.format("Image rendered in %.4fms", ((double) endTime - startTime) / 1000000.0));
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        mFrame = new JFrame();
        mFrame.setTitle("Planet Render Test");
        mFrame.setBounds(100, 100, 550, 550);
        mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mFrame.getContentPane().setLayout(new BorderLayout(0, 0));

        JToolBar toolBar = new JToolBar();
        mFrame.getContentPane().add(toolBar, BorderLayout.NORTH);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                refreshImage();
            }
        });
        toolBar.add(btnRefresh);

        mImagePanel = new ImagePanel();
        mFrame.getContentPane().add(mImagePanel, BorderLayout.CENTER);
        
        JPanel panel = new JPanel();
        mFrame.getContentPane().add(panel, BorderLayout.SOUTH);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        mStatus = new JLabel("Please wait...");
        panel.add(mStatus);
    }

    private class ImagePanel extends JPanel {
        private Image mImage;

        public void setImage(Image image) {
            mImage = image;
            repaint();
        }

        @Override
        public void paintComponent(Graphics g) {
            if (mImage != null) {
                g.drawImage(mImage, 0, 0, null);
            }
        }
    }
}
