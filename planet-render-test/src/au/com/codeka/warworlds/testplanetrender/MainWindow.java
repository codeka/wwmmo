package au.com.codeka.warworlds.testplanetrender;

import java.awt.EventQueue;

import javax.swing.JFrame;

public class MainWindow {

    private JFrame mFrame;

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

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        mFrame = new JFrame();
        mFrame.setTitle("Planet Render Test");
        mFrame.setBounds(100, 100, 1200, 700);
        mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mFrame.getContentPane().add(new AppContent());
    }
}
