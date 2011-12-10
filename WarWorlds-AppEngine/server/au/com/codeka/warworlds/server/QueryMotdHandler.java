package au.com.codeka.warworlds.server;

import java.util.logging.Logger;

import au.com.codeka.warworlds.server.data.MessageOfTheDay;

public class QueryMotdHandler {
	private static final Logger log = Logger.getLogger(HelloWorldService.class.getName());

	public QueryMotdHandler() {
	}

	public static String query() {
		try {
			MessageOfTheDay motd = MessageOfTheDay.getCurrentMotd();
			if (motd == null) {
				return "";
			}
			return motd.getMessage();
		} catch(Exception e) {
			log.severe(e.toString());
			return "ERROR:"+e.toString();
		}
	}
}
