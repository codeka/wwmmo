package au.com.codeka.warworlds.server.ctrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

public class NameGenerator {
    private static ArrayList<Vocabulary> sVocabularies;
    static {
        String path = System.getProperty("au.com.codeka.warworlds.server.basePath");
        if (path == null) {
            path = NameGenerator.class.getClassLoader().getResource("").getPath();
        }
        path += "../data/vocab";
        File rootPath = new File(path);

        sVocabularies = new ArrayList<Vocabulary>();
        for (File vocabFile : rootPath.listFiles()) {
            if (vocabFile.isDirectory()) {
                continue;
            }
            sVocabularies.add(parseVocabularyFile(vocabFile.getAbsolutePath()));
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

        return word.toString();
    }

    private static Vocabulary parseVocabularyFile(String path) {
        BufferedReader ins = null;
        try {
            ins = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

            Vocabulary vocab = new Vocabulary();
            String line;
            while ((line = ins.readLine()) != null) {
                for (String word : line.split("[ \t]+")) {
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
