package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

/** Profanity filter looks at a string and returns an integer describing the "profanity level"
 * of that string, 0 = not at all profane, 1 = somewhat profane, 2 = strong profanity.
 */
public class ProfanityFilter {
    private static final Logger log = LoggerFactory.getLogger(ProfanityFilter.class);
    private static HashMap<String, Integer> sProfaneWords = new HashMap<String, Integer>();

    /**
     * Reset the filter and cause it to be reloaded from the database. This is useful when we
     * update the list in the backend.
     */
    public static void resetFilter() {
        sProfaneWords.clear();
    }

    public static int filter(String words) {
        ensureFilter();

        int level = 0;
        for (String word : words.split("\\s+")) {
            word = word.replaceAll("^\\W*(.*)\\W*$", "$1");
            word = word.toLowerCase();

            Integer wordLevel = sProfaneWords.get(word);
            if (wordLevel != null) {
                level += (int) wordLevel;
            }
        }

        if (level > 2) {
            level = 2;
        }
        return level;
    }

    private static void ensureFilter() {
        if (!sProfaneWords.isEmpty()) {
            return;
        }

        String sql = "SELECT * FROM chat_profane_words";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                int profanityLevel = rs.getInt("profanity_level");
                String words = rs.getString("words");

                for (String word : words.split("\\s+")) {
                    sProfaneWords.put(word.trim().toLowerCase(), profanityLevel);
                }
            }
        } catch(Exception e) {
            log.error("Error fetching profane words list.", e);
        }
    }
}
