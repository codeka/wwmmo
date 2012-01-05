package au.com.codeka.warworlds.shared.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * This class can be used to parse a syllable file, and generate a list of random names
 * from those syllables. We use it generate star names, and whatnot.
 */
public class NameGenerator {
    private ArrayList<Syllable> mPrefixes;
    private ArrayList<Syllable> mSuffixes;
    private ArrayList<Syllable> mSyllables; // the "middle" of a word

    private final static char[] sVowels = {'a', 'e', 'i', 'o', 'u'};

    public NameGenerator(InputStream ins) throws IOException {
        InputStreamReader sr = new InputStreamReader(ins);
        try {
            parse(new BufferedReader(sr));
        } finally {
            sr.close();
        }
    }

    public NameGenerator(BufferedReader reader) throws IOException {
        parse(reader);
    }

    /**
     * Reparses the given file, replacing the contents of this \c NameGenerator with data
     * from the given file.
     * @throws IOException 
     */
    public void parse(BufferedReader reader) throws IOException {
        mPrefixes = new ArrayList<Syllable>();
        mSuffixes = new ArrayList<Syllable>();
        mSyllables = new ArrayList<Syllable>();

        String line;
        while ((line = reader.readLine()) != null) {
            int comment = line.indexOf("#");
            if (comment >= 0) {
                line = line.substring(0, comment);
            }

            line = line.trim();
            if (line == "") {
                continue;
            }

            boolean isPrefix = false;;
            boolean isSuffix = false;
            if (line.charAt(0) == '+') {
                isPrefix = true;
                line = line.substring(1);
            }
            else if (line.charAt(0) == '-') {
                isSuffix = true;
                line = line.substring(1);
            }

            Syllable s = new Syllable();

            if (line.indexOf("+v") > 0) {
                s.requiresNextVowel = true;
                line = line.replace("+v", "");
            }
            if (line.indexOf("-v") > 0) {
                s.requiresPreviousVowel = true;
                line = line.replace("-v", "");
            }
            if (line.indexOf("+c") > 0) {
                s.requiresNextConsonant = true;
                line = line.replace("+c", "");
            }
            if (line.indexOf("-c") > 0) {
                s.requiresPreviousConsonant = true;
                line = line.replace("-c", "");
            }

            s.syllable = line.trim().toLowerCase();
            s.endsWithVowel = arrayContains(sVowels, s.syllable.charAt(s.syllable.length() - 1));
            s.startsWithVowel = arrayContains(sVowels, s.syllable.charAt(0));

            if (isPrefix) {
                mPrefixes.add(s);
            } else if (isSuffix) {
                mSuffixes.add(s);
            } else {
                mSyllables.add(s);
            }
        }
    }

    /**
     * Composes a new word, using the given random number generator and returning the given number
     * of syllables.
     */
    public String compose(CoolRandom rand, int numSyllables) {
        ArrayList<Syllable> word = new ArrayList<Syllable>();
        word.add(mPrefixes.get(rand.nextInt(mPrefixes.size())));
        for(int i = 1; i < numSyllables - 1; i++) {
            word.add(getSyllable(rand, mSyllables, word.get(word.size() - 1)));
        }
        word.add(getSyllable(rand, mSuffixes, word.get(word.size() - 1)));

        return combine(word);
    }

    /**
     * Combines the given list of syllables (which we assume is valid according to the
     * consonant/vowel rules) and returns the complete word.
     */
    private String combine(List<Syllable> syllables) {
        StringBuilder sb = new StringBuilder();
        for(Syllable s : syllables) {
            sb.append(s.syllable);
        }
        return sb.toString();
    }

    /**
     * Looks in the given \c pool for a valid syllable to add to the list. This can go into an
     * infinite loop if there is no valid combination... be careful!
     */
    private Syllable getSyllable(CoolRandom rand, List<Syllable> pool, Syllable previousSyllable) {
        Syllable nextSyllable = null;
        do {
            nextSyllable = pool.get(rand.nextInt(pool.size()));
        } while (!isValidPair(previousSyllable, nextSyllable));

        return nextSyllable;
    }

    /**
     * Checks whether the given two syllables are valid together or not.
     */
    private boolean isValidPair(Syllable prev, Syllable next) {
        if (prev.requiresNextVowel && !next.startsWithVowel)
            return false;
        if (prev.requiresNextConsonant && next.startsWithVowel)
            return false;

        if (next.requiresPreviousVowel && !prev.endsWithVowel)
            return false;
        if (next.requiresPreviousConsonant && prev.endsWithVowel)
            return false;

        return true;
    }

    private static boolean arrayContains(char[] array, char ch) {
        for(char ach : array) {
            if (ach == ch) {
                return true;
            }
        }

        return false;
    }

    /**
     * Contains the data we know about a particular syllable in the vocabulary.
     */
    private class Syllable {
        /** The actual syllable itself. */
        public String syllable;

        /** Does the syllable start with a vowel? (if not, obviously it starts with a consonant) */
        public boolean startsWithVowel;

        /** Does the syllable end with a vowel? */
        public boolean endsWithVowel;

        /** Does this syllable require the \i next syllable to start with a vowel? */
        public boolean requiresNextVowel;

        /** Does this syllable require the \i next syllable to start with a consonant? */
        public boolean requiresNextConsonant;

        /** Does this syllable require the \i previous syllable to end with a vowel? */
        public boolean requiresPreviousVowel;

        /** Does this syllable require the \i previous syllable to end with a consonant? */
        public boolean requiresPreviousConsonant;
    }
}
