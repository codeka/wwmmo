package au.com.codeka.planetrendertest;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

public class MainApplet extends JApplet {
  private static final long serialVersionUID = 1L;

  /** Called when the applet is loaded into the browser. */
  public void init() {
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
          add(new AppContent());
        }
      });
    } catch (Exception e) {
      System.err.println("createGUI didn't complete successfully");
    }
  }
}
