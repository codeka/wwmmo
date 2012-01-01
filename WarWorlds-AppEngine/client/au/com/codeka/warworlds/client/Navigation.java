package au.com.codeka.warworlds.client;

import au.com.codeka.warworlds.client.pages.*;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RootPanel;

public class Navigation {

    private Navigation() {
    }

    /**
     * "Navigate" to the specified <c>Composite</c>.
     * @param c
     */
    public static void go(Composite c) {
        RootPanel panel = RootPanel.get("maincontent");
        if (panel == null) {
            return;
        }

        panel.clear();
        panel.add(c);
        History.newItem(c.getTitle());
    }
	
    public static void go(String name) {
        // todo: there's got to be a better way....
        if (name.equalsIgnoreCase("dashboard")) {
            go(new DashboardPage());
        } else if (name.equalsIgnoreCase("motd")) {
            go(new MotdPage());
        } else if (name.equalsIgnoreCase("devices")) {
            go(new DevicesPage());
        } else if (name.equalsIgnoreCase("starfield-debug")) {
            go(new StarfieldDebugPage());
        } else if (name.equalsIgnoreCase("datastore-debug")) {
            go(new DataStoreDebugPage());
        } else {
            // go(new NotFoundPage())
        }
    }
}
