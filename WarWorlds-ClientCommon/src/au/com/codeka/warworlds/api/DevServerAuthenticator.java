package au.com.codeka.warworlds.api;

/**
 * This is an authenticator that'll authenticate with the App Engine dev server. It's pretty simple,
 * because the dev server doesn't actually do anything, it just trusts the cookie we hand it.
 */
public class DevServerAuthenticator {
    /**
     * Authenticates the given user with App Engine.
     * 
     * Note: You CANNOT call this method on the main thread. Do it in a background thread, because
     * it can (will) block on network calls.
     * 
     * @param accountName The account we want to authenticate as.
     * @param admin If true, we authenticate as an administrator.
     * @return The authCookie we can use in subsequent calls to App Engine.
     */
    public static String authenticate(String accountName, boolean admin) {
        return "dev_appserver_login="+accountName+":"+(admin ? "true" : "false")+":"+accountName;
    }

}
