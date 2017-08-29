package au.com.codeka.warworlds.planetrender.ui;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class MainWindow {
  private JFrame frame;

  /** Launch the application. */
  public static void main(String[] args) {
    EventQueue.invokeLater(() -> {
      try {
        MainWindow window = new MainWindow();
        window.frame.setVisible(true);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  /** Create the application. */
  public MainWindow() {
    initialize();
  }

  /** Initialize the contents of the frame. */
  private void initialize() {
    frame = new JFrame();
    frame.setTitle("Planet Render");
    frame.setBounds(100, 100, 1200, 700);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.getContentPane().add(new AppContent());
  }
}
