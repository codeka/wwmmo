package au.com.codeka.warworlds.planetrender.ui

import javax.swing.JApplet
import javax.swing.SwingUtilities

class MainApplet : JApplet() {
  /** Called when the applet is loaded into the browser.  */
  override fun init() {
    try {
      SwingUtilities.invokeAndWait { add(AppContent()) }
    } catch (e: Exception) {
      System.err.println("createGUI didn't complete successfully")
    }
  }

  companion object {
    private const val serialVersionUID = 1L
  }
}