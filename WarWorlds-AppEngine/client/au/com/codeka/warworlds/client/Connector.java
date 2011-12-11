package au.com.codeka.warworlds.client;

import org.restlet.client.resource.ClientProxy;

/**
 * Helper class for talking via restlet to the server.
 */
public class Connector {

	/**
	 * Creates a connection using the given \c classLiteral (that refers to the interface
	 * of the proxy object to create) and the URL.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ClientProxy> T create(Object proxy, String url) {
    	((T) proxy).getClientResource().setReference("/api/v1" + url);
    	return (T) proxy;
	}

}
