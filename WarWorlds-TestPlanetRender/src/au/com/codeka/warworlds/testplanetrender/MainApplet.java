package au.com.codeka.warworlds.testplanetrender;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

public class MainApplet extends JApplet {
    private static final long serialVersionUID = 1L;

    //Called when this applet is loaded into the browser.
    public void init() {
        //Execute a job on the event-dispatching thread; creating this applet's GUI.
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