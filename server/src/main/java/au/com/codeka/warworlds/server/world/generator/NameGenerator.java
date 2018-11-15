package au.com.codeka.warworlds.server.world.generator;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;

public class NameGenerator {
  private static final Log log = new Log("NameGenerator");
  private static final ArrayList<Vocabulary> VOCABULARIES = new ArrayList<>();
  private static final ArrayList<String> BLACKLIST = new ArrayList<>();

  static {
    File path = new File("data/vocab");

    ArrayList<String> files = new ArrayList<>();
    for (File vocabFile : Preconditions.checkNotNull(path.listFiles())) {
      if (vocabFile.isDirectory()) {
        continue;
      }
      if (!vocabFile.getName().endsWith(".txt")) {
        continue;
      }
      files.add(vocabFile.getAbsolutePath());
    }
    loadVocabularies(files);


    try {
      List<String> lines = Files.readLines(new File(path, "blacklist"), Charset.defaultCharset());
      for (String line : lines) {
        line = line.trim().toLowerCase();
        if (line.isEmpty()) {
          continue;
        }
        BLACKLIST.add(line);
      }
    } catch (IOException e) {
      log.error("Error loading blacklist.", e);
    }
  }

  public String generate(Random rand) {
    Vocabulary vocab = VOCABULARIES.get(rand.nextInt(VOCABULARIES.size()));

    // We'll want to reject about 50% of 3-letter words, otherwise there's just too many.
    boolean rejectThreeLetterWord = rand.nextFloat() < 0.5;

    String name;
    while (true) {
      name = tryGenerate(vocab, rand);
      if (name == null) {
        continue;
      }

      // Too long, try again.
      if (name.length() > 10) {
        continue;
      }

      // To short, try again.
      if (name.length() <= (rejectThreeLetterWord ? 3 : 2)) {
        continue;
      }

      // If it's in the blacklist, try again
      for (String blacklist : BLACKLIST) {
        if (blacklist.contains(name)) {
          continue;
        }
      }

      // Just right, lets use it.
      break;
    }

    // Make sure it's title case.
    name = name.toLowerCase();
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name);
  }

  @Nullable
  private String tryGenerate(Vocabulary vocab, Random rand) {
    StringBuilder word = new StringBuilder();
    word.append(vocab.getLetter("  ", rand));
    if (Character.isWhitespace(word.charAt(0))) {
      return null;
    }
    word.append(vocab.getLetter(" "+word.charAt(0), rand));
    if (Character.isWhitespace(word.charAt(1))) {
      return null;
    }
    while (true) {
      try {

        char ch = vocab.getLetter(word.substring(word.length() - 2, word.length()), rand);
        if (Character.isWhitespace(ch)) {
          break;
        } else {
          word.append(ch);
        }
      } catch (Exception e) {
        log.warning("Unexpected error generating name.", e);
        return null;
      }
    }
    return word.toString();
  }

  private static void loadVocabularies(List<String> files) {
    for (String file : files) {
      VOCABULARIES.add(parseVocabularyFile(file));
    }
  }

  @Nullable
  private static Vocabulary parseVocabularyFile(String path) {
    log.info("Parsing vocabulary file: %s", path);
    BufferedReader ins = null;
    try {
      ins = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

      Vocabulary vocab = new Vocabulary(path);
      String line;
      while ((line = ins.readLine()) != null) {
        for (String word : line.split("[^a-zA-Z]+")) {
          word = word.trim();
          if (word.isEmpty()) {
            continue;
          }
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
          // ignore.
        }
      }
    }
  }

  private static class Vocabulary {
    private final TreeMap<String, TreeMap<Character, Integer>> letterFrequencies = new TreeMap<>();
    private final String name;

    public Vocabulary(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void addLetter(String previousLetters, char letter) {
      TreeMap<Character, Integer> frequencies = letterFrequencies.get(previousLetters);
      if (frequencies == null) {
        frequencies = new TreeMap<>();
        letterFrequencies.put(previousLetters, frequencies);
      }

      if (frequencies.containsKey(letter)) {
        frequencies.put(letter, frequencies.get(letter) + 1);
      } else {
        frequencies.put(letter, 1);
      }
    }

    public char getLetter(String lastLetters, Random rand) {
      TreeMap<Character, Integer> frequencies = letterFrequencies.get(lastLetters);
      if (frequencies == null) {
        throw new RuntimeException("No frequencies for letters: '"+lastLetters+"' ");
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

  public static void main(String[] args) {
    NameGenerator generator = new NameGenerator();
    Random rand = new Random();
    Map<String, Integer> names = new TreeMap<>();
    for (int i = 0; i < 10000; i++) {
      String name = generator.generate(rand);
      if (names.containsKey(name)) {
        names.put(name, names.get(name) + 1);
      } else {
        names.put(name, 1);
      }
    }

    for (String key : names.keySet()) {
     // if (names.get(key) < 10) {
     //   continue;
     // }

      System.out.println(key + "               ".substring(0, 12 - key.length())
          + Integer.toString(names.get(key)));
    }
  }
}
