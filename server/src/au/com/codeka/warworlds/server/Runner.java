package au.com.codeka.warworlds.server;

import org.eclipse.jetty.server.Server;

import au.com.codeka.warworlds.server.handlers.pages.HtmlPageHandler;
import au.com.codeka.warworlds.server.model.DesignManager;

public class Runner {
    public static void main(String[] args) throws Exception {
        String basePath = System.getProperty("au.com.codeka.warworlds.server.basePath");
        if (basePath == null) {
            basePath = HtmlPageHandler.class.getClassLoader().getResource("").getPath();
        }

        DesignManager.setup(basePath);

        // kick off the event processor thread
        EventProcessor.i.ping();

        Server server = new Server(8080);
        server.setHandler(new RequestRouter());
        server.start();
        server.join();
    }
}
