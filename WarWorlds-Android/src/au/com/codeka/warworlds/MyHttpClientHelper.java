package au.com.codeka.warworlds;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.restlet.Client;
import org.restlet.ext.httpclient.HttpClientHelper;

public class MyHttpClientHelper extends HttpClientHelper {

	public MyHttpClientHelper(Client client) {
		super(client);
	}

	@Override
	protected void configure(SchemeRegistry schemeRegistry) {
		super.configure(schemeRegistry);

		schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
	}
}
