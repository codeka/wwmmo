package au.com.codeka.warworlds.server.handlers.admin;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.LoginController;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

public class AdminLoginHandler extends AdminHandler {
    private final static String CLIENT_ID = "1021675369049-sumlr2cihs72j4okvfl8hl72keognhsa.apps.googleusercontent.com";

    @Override
    protected void get() throws RequestException {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("client_id", CLIENT_ID);
        render("login.html", data);
    }

    @Override
    protected void post() throws RequestException {
        String emailAddr = null;
        try {
            String authResult = getRequest().getParameter("auth-result");
            JSONObject json = (JSONObject) new JSONParser().parse(authResult);
            String idToken = (String) json.get("id_token");

            TokenParser parser = new TokenParser(new String[] { CLIENT_ID }, CLIENT_ID);
            GoogleIdToken.Payload payload = parser.parse(idToken);
            if (payload == null) {
                throw new RequestException(500, parser.problem());
            }
            emailAddr = payload.getEmail();
            if (emailAddr == null) {
                String entries = null;
                for (Map.Entry<String, Object> entry : payload.entrySet()) {
                    if (entries == null) {
                        entries = "";
                    } else {
                        entries += ", ";
                    }
                    entries += String.format("%s = %s", entry.getKey(), entry.getValue());
                }
                entries += "\r\n" + authResult;
                throw new RequestException(500, "No email address: "+entries);
            }
        } catch (ParseException e) {
            throw new RequestException(e);
        }

        String cookieValue = new LoginController().generateCookie(emailAddr, true, null);

        Cookie cookie = new Cookie("SESSION", cookieValue);
        getResponse().addCookie(cookie);
        getResponse().setStatus(302);

        String continueUrl = getRequest().getParameter("continue");
        if (continueUrl == null) {
            continueUrl = "/realms/"+getRealm()+"/admin";
        }
        getResponse().addHeader("Location", continueUrl);
    }

    public class TokenParser {
        private final List<String> mClientIDs;
        private final String mAudience;
        private final GoogleIdTokenVerifier mVerifier;
        private final JsonFactory mJFactory;
        private String mProblem = "Verification failed. (Time-out?)";

        public TokenParser(String[] clientIDs, String audience) {
            mClientIDs = Arrays.asList(clientIDs);
            mAudience = audience;
            NetHttpTransport transport = new NetHttpTransport();
            mJFactory = new GsonFactory();
            mVerifier = new GoogleIdTokenVerifier(transport, mJFactory);
        }

        public GoogleIdToken.Payload parse(String tokenString) {
            GoogleIdToken.Payload payload = null;
            try {
                GoogleIdToken token = GoogleIdToken.parse(mJFactory, tokenString);
                if (mVerifier.verify(token)) {
                    GoogleIdToken.Payload tempPayload = token.getPayload();
                    if (!tempPayload.getAudience().equals(mAudience))
                        mProblem = "Audience mismatch, " + mAudience + " != " + tempPayload.getAudience();
                    else if (!mClientIDs.contains(tempPayload.getAuthorizedParty()))
                        mProblem = "Client ID mismatch";
                    else
                        payload = tempPayload;
                }
            } catch (GeneralSecurityException e) {
                mProblem = "Security issue: " + e.getLocalizedMessage();
            } catch (IOException e) {
                mProblem = "Network problem: " + e.getLocalizedMessage();
            }
            return payload;
        }

        public String problem() {
            return mProblem;
        }
    }
}
