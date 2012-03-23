package au.com.codeka.warworlds.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import au.com.codeka.warworlds.api.RequestManager.ResultWrapper;

/**
 * This is an authenticator that'll authenticate with the App Engine production environment. It
 * uses the ClientLogin protocol to get a cookie we can use later on.
 */
public class ClientLoginAuthenticator {
    /**
     * Authenticates the given user with App Engine.
     * 
     * Note: You CANNOT call this method on the main thread. Do it in a background thread, because
     * it can (will) block on network calls.
     * 
     * @param accountName The account we want to authenticate as.
     * @param password The password needed to login to that account.
     * @throws ApiException 
     */
    public static String authenticate(String accountName, String password) throws ApiException {
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("Email", accountName));
            params.add(new BasicNameValuePair("Passwd", password));
            params.add(new BasicNameValuePair("accountType", "HOSTED_OR_GOOGLE"));
            params.add(new BasicNameValuePair("service", "ah"));
            params.add(new BasicNameValuePair("source", "warworldsmmo")); // TODO
            HttpEntity entity = new UrlEncodedFormEntity(params);

            ResultWrapper result = RequestManager.request(
                    "POST", "https://www.google.com/accounts/ClientLogin", null, entity);
            try {
                HttpResponse resp = result.getResponse();
                if (resp.getStatusLine().getStatusCode() != 200) {
                    throw new ApiException("Authentication failed: "+resp.getStatusLine());
                }

                List<String> lines = IOUtils.readLines(resp.getEntity().getContent(), "utf-8");
                for(String line : lines) {
                    if (line.startsWith("Auth=")) {
                        String token = line.substring(5);
                        return AppEngineAuthenticator.authenticate(token);
                    }
                }
            } finally {
                result.close();
            }
        } catch (UnsupportedEncodingException e) {
            // shouldn't happen!
            throw new ApiException(e);
        } catch (IOException e) {
            throw new ApiException(e);
        }

        throw new ApiException("No authentication response found!");
    }

}
