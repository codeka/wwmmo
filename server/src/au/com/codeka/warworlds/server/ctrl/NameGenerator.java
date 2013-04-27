package au.com.codeka.warworlds.server.ctrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.lang3.text.WordUtils;

public class NameGenerator {
    private static ArrayList<Vocabulary> sVocabularies;

    public static void setup(String basePath) {
        File path = new File(basePath + "../data/vocab");

        ArrayList<String> files = new ArrayList<String>();
        for (File vocabFile : path.listFiles()) {
            if (vocabFile.isDirectory()) {
                continue;
            }
            files.add(vocabFile.getAbsolutePath());
        }

        loadVocabularies(files);
    }

    public static void loadVocabularies(List<String> files) {
        sVocabularies = new ArrayList<Vocabulary>();
        for (String file : files) {
            sVocabularies.add(parseVocabularyFile(file));
        }
    }

    public String generate(Random rand) {
        Vocabulary vocab = sVocabularies.get(rand.nextInt(sVocabularies.size()));

        StringBuffer word = new StringBuffer();
        word.append(vocab.getLetter("  ", rand));
        word.append(vocab.getLetter(" "+word.charAt(0), rand));
        while (true) {
            char ch = vocab.getLetter(word.substring(word.length() - 2, word.length()), rand);
            if (ch == ' ') {
                break;
            } else {
                word.append(ch);
            }
        }

        if (word.length() > 10) {
            return generate(rand);
        }

        return WordUtils.capitalize(word.toString().toLowerCase());
    }

    private static Vocabulary parseVocabularyFile(String path) {
        BufferedReader ins = null;
        try {
            ins = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

            Vocabulary vocab = new Vocabulary();
            String line;
            while ((line = ins.readLine()) != null) {
                for (String word : line.split("[^a-zA-Z]+")) {
                    word = word.trim();
                    String lastLetters = "  ";
                    for (int i = 0; i < word.length(); i++) {
                        char letter = word.charAt(i);
                        vocab.addLetter(lastLetters, letter);

                        lastLetters += letter;
                        lastLetters = lastLetters.substring(1);
                    }
                    vocab.addLetter(lastLetters, ' ');
                }
            }

            return vocab;
        } catch (IOException e) {
            return null; // should never happen
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static class Vocabulary {
        private TreeMap<String, TreeMap<Character, Integer>> mLetterFrequencies;

        public Vocabulary() {
            mLetterFrequencies = new TreeMap<String, TreeMap<Character, Integer>>();
        }

        public void addLetter(String previousLetters, char letter) {
            TreeMap<Character, Integer> frequencies = mLetterFrequencies.get(previousLetters);
            if (frequencies == null) {
                frequencies = new TreeMap<Character, Integer>();
                mLetterFrequencies.put(previousLetters, frequencies);
            }

            if (frequencies.containsKey(letter)) {
                frequencies.put(letter, frequencies.get(letter) + 1);
            } else {
                frequencies.put(letter, 1);
            }
        }

        public char getLetter(String lastLetters, Random rand) {
            TreeMap<Character, Integer> frequencies = mLetterFrequencies.get(lastLetters);
            if (frequencies == null) {
                throw new RuntimeException("No frequencies for letters: '"+lastLetters+"'");
            }
            int maxFrequency = 0;
            for (int frequency : frequencies.values()) {
                maxFrequency += frequency;
            }

            int index = rand.nextInt(maxFrequency);
            for (Character ch : frequencies.keySet()) {
                index -= frequencies.get(ch);
                if (index <= 0) {
                    return ch;
                }
            }

            return ' ';
        }
    }
}
