package au.com.codeka.warworlds.server;

import org.eclipse.jetty.server.Server;

public class Runner {
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.setHandler(new RequestRouter());
        server.start();
        server.join();
    }
}
