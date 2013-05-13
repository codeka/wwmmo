package au.com.codeka.warworlds.server.ctrl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslateController {
    private final Logger log = LoggerFactory.getLogger(TranslateController.class);

    private static String TRANSLATE_API_KEY = "AIzaSyANXsZc4CaLMXDBJDClO9uAnzuYysQJ0zw";
    private static String TRANSLATE_BASE_URL = "https://www.googleapis.com/language/translate/v2?key="+TRANSLATE_API_KEY;

    /**
     * Attempts to translate the given string to English. This results in a call to the Google API,
     * and it will block the current thread.
     *
     * @param source The string you want to translate.
     * @return The translated string, or \c null if no translation was applied (for example if we
     *         think the string is already in English)
     */
    public String translate(String source) {
        if (isEnglish(source)) {
            log.info("String is English, not translating...");
            return null;
        }

        String url;
        try {
            url = TRANSLATE_BASE_URL+"&target=en&q="+URLEncoder.encode(source, "utf-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Error encoding traslate request.", e);
            return null;
        }
        log.info("Translate URL: "+url);

        JSONObject json;
        try {
            URLConnection conn = new URL(url).openConnection();
            InputStream ins = conn.getInputStream();
            String encoding = conn.getContentEncoding();
            if (encoding == null) {
                encoding = "utf-8";
            }
            InputStreamReader isr = new InputStreamReader(ins, encoding);
            json = (JSONObject) JSONValue.parse(isr);
        } catch (Exception e) {
            log.error("Error posting traslate request.", e);
            return null;
        }

        try {
            JSONObject data = (JSONObject) json.get("data");
            JSONArray translations = (JSONArray) data.get("translations");
            JSONObject translation = (JSONObject) translations.get(0);
            String text = (String) translation.get("translatedText");
            return text.replace("&quot;", "\"");
        } catch (Exception e) {
            log.error("Error decoding traslate response.", e);
            return null;
        }
    }

    /**
     * Very simple check to decide whether we need to perform a translation on this string or not.
     */
    private static boolean isEnglish(String str) {
        for (int i = 0; i < str.length(); i++) {
            Character ch = str.charAt(i);
            if (ch > 0x80) {
                return false;
            }
        }
        return true;
    }
}
