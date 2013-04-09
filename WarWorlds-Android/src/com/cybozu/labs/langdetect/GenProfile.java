package com.cybozu.labs.langdetect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.cybozu.labs.langdetect.util.LangProfile;

/**
 * Load Wikipedia's abstract XML as corpus and
 * generate its language profile in JSON format.
 * 
 * @author Nakatani Shuyo
 * 
 */
public class GenProfile {

    /**
     * Load text file with UTF-8 and generate its language profile
     * @param lang target language name
     * @param file target file path
     * @return Language profile instance
     * @throws LangDetectException 
     */
    public static LangProfile loadFromText(String lang, File file) throws LangDetectException {

        LangProfile profile = new LangProfile(lang);

        BufferedReader is = null;
        try {
            is = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));

            int count = 0;
            while (is.ready()) {
                String line = is.readLine();
                profile.update(line);
                ++count;
            }

            System.out.println(lang + ":" + count);

        } catch (IOException e) {
            throw new LangDetectException(ErrorCode.CantOpenTrainData, "Can't open training database file '" + file.getName() + "'");
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {}
        }
        return profile;
    }
}
