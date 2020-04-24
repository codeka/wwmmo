package au.com.codeka.warworlds.server.world.generator

import au.com.codeka.warworlds.common.Log
import com.google.common.base.CaseFormat
import com.google.common.base.Preconditions
import com.google.common.io.Files
import java.io.*
import java.nio.charset.Charset
import java.util.*

class NameGenerator {
  companion object {
    private val log = Log("NameGenerator")
    private val VOCABULARIES = ArrayList<Vocabulary?>()
    private val BLACKLIST = ArrayList<String>()
    private fun loadVocabularies(files: List<String>) {
      for (file in files) {
        VOCABULARIES.add(parseVocabularyFile(file))
      }
    }

    private fun parseVocabularyFile(path: String): Vocabulary? {
      log.info("Parsing vocabulary file: %s", path)
      var ins: BufferedReader? = null
      return try {
        ins = BufferedReader(InputStreamReader(FileInputStream(path)))
        val vocab = Vocabulary(path)
        var line: String
        while (ins.readLine().also { line = it } != null) {
          for (word in line.split("[^a-zA-Z]+").toTypedArray()) {
            var letters = word.trim { it <= ' ' }
            if (letters.isEmpty()) {
              continue
            }
            var lastLetters = "  "
            for (i in 0 until letters.length) {
              val letter = letters[i]
              vocab.addLetter(lastLetters, letter)
              lastLetters += letter
              lastLetters = lastLetters.substring(1)
            }
            vocab.addLetter(lastLetters, ' ')
          }
        }
        vocab
      } catch (e: IOException) {
        null // should never happen
      } finally {
        if (ins != null) {
          try {
            ins.close()
          } catch (e: IOException) {
            // ignore.
          }
        }
      }
    }

    @JvmStatic
    fun main(args: Array<String>) {
      val generator = NameGenerator()
      val rand = Random()
      val names: MutableMap<String, Int> = TreeMap()
      for (i in 0..9999) {
        val name = generator.generate(rand)
        if (names.containsKey(name)) {
          names[name] = names[name]!! + 1
        } else {
          names[name] = 1
        }
      }
      for (key in names.keys) {
        // if (names.get(key) < 10) {
        //   continue;
        // }
        println(key + "               ".substring(0, 12 - key.length)
            + Integer.toString(names[key]!!))
      }
    }

    init {
      val path = File("data/vocab")
      val files = ArrayList<String>()
      for (vocabFile in path.listFiles()) {
        if (vocabFile.isDirectory) {
          continue
        }
        if (!vocabFile.name.endsWith(".txt")) {
          continue
        }
        files.add(vocabFile.absolutePath)
      }
      loadVocabularies(files)
      try {
        val lines = Files.readLines(File(path, "blacklist"), Charset.defaultCharset())
        for (l in lines) {
          var line = l.trim { it <= ' ' }.toLowerCase()
          if (line.isEmpty()) {
            continue
          }
          BLACKLIST.add(line)
        }
      } catch (e: IOException) {
        log.error("Error loading blacklist.", e)
      }
    }
  }

  fun generate(rand: Random?): String {
    val vocab = VOCABULARIES[rand!!.nextInt(VOCABULARIES.size)]

    // We'll want to reject about 50% of 3-letter words, otherwise there's just too many.
    val rejectThreeLetterWord = rand.nextFloat() < 0.5
    var name: String?
    while (true) {
      name = tryGenerate(vocab, rand)
      if (name == null) {
        continue
      }

      // Too long, try again.
      if (name.length > 10) {
        continue
      }

      // To short, try again.
      if (name.length <= (if (rejectThreeLetterWord) 3 else 2)) {
        continue
      }

      // If it's in the blacklist, try again
      for (blacklist in BLACKLIST) {
        if (blacklist.contains(name)) {
          continue
        }
      }

      // Just right, lets use it.
      break
    }

    // Make sure it's title case.
    name = name!!.toLowerCase()
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name)
  }

  private fun tryGenerate(vocab: Vocabulary?, rand: Random?): String? {
    val word = StringBuilder()
    word.append(vocab!!.getLetter("  ", rand))
    if (Character.isWhitespace(word[0])) {
      return null
    }
    word.append(vocab.getLetter(" " + word[0], rand))
    if (Character.isWhitespace(word[1])) {
      return null
    }
    while (true) {
      try {
        val ch = vocab.getLetter(word.substring(word.length - 2, word.length), rand)
        if (Character.isWhitespace(ch)) {
          break
        } else {
          word.append(ch)
        }
      } catch (e: Exception) {
        log.warning("Unexpected error generating name.", e)
        return null
      }
    }
    return word.toString()
  }

  private class Vocabulary(val name: String) {
    private val letterFrequencies = TreeMap<String, TreeMap<Char, Int>>()

    fun addLetter(previousLetters: String, letter: Char) {
      var frequencies = letterFrequencies[previousLetters]
      if (frequencies == null) {
        frequencies = TreeMap()
        letterFrequencies[previousLetters] = frequencies
      }
      if (frequencies.containsKey(letter)) {
        frequencies[letter] = frequencies[letter]!! + 1
      } else {
        frequencies[letter] = 1
      }
    }

    fun getLetter(lastLetters: String, rand: Random?): Char {
      val frequencies = letterFrequencies[lastLetters]
          ?: throw RuntimeException("No frequencies for letters: '$lastLetters' ")
      var maxFrequency = 0
      for (frequency in frequencies.values) {
        maxFrequency += frequency
      }
      var index = rand!!.nextInt(maxFrequency)
      for (ch in frequencies.keys) {
        index -= frequencies[ch]!!
        if (index <= 0) {
          return ch
        }
      }
      return ' '
    }

  }
}