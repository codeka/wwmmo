package au.com.codeka.warworlds.planetrender.ui

import java.awt.EventQueue
import javax.swing.JFrame
import javax.swing.WindowConstants

class MainWindow {
  private var frame: JFrame? = null

  /** Initialize the contents of the frame.  */
  private fun initialize() {
    frame = JFrame()
    frame!!.title = "Planet Render"
    frame!!.setBounds(100, 100, 1200, 700)
    frame!!.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame!!.contentPane.add(AppContent())
  }

  companion object {
    /** Launch the application.  */
    @JvmStatic
    fun main(args: Array<String>) {
      EventQueue.invokeLater {
        try {
          val window = MainWindow()
          window.frame!!.isVisible = true
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
  }

  /** Create the application.  */
  init {
    initialize()
  }
}