"""namegen.py: Contains logic for generating random names."""

import os
import random
import codecs
import logging


_vocabularies = []


def generate(numNames):
  global _vocabularies
  _ensureVocabulariesLoaded()

  names = []
  for _ in range(numNames):
    vocabIndex = random.randint(0, len(_vocabularies)-1)
    vocabulary = _vocabularies[vocabIndex]
    names.append(_generateName(vocabulary))
  return names


def _ensureVocabulariesLoaded():
  """Ensures that we've loaded the vocabulary files. We only need to do it once."""
  global _vocabularies
  if len(_vocabularies) > 0:
    return

  vocabularies = []
  rootFolder = os.path.dirname(__file__)+"/vocabulary"
  for fileName in os.listdir(rootFolder):
    if fileName.endswith(".txt"):
      path = rootFolder+"/"+fileName
      logging.info("Loading vocabulary file: "+path)
      vocabularies.append(_parseVocabulary(path))
  _vocabularies = vocabularies


def _parseVocabulary(file_name):
  """This class parses a vocabulary file.

  The vocabulary file is really just a list of words in a certain "vocabulary" (e.g. Viking names,
  English names, etc). We parse the words from that file, and for each pair of adjacent letters
  we determine which letters are most comment next to each other.
  """
  letter_frequencies = {}

  def addLetter(last_letters, letter):
    if last_letters not in letter_frequencies:
      frequency = {}
      letter_frequencies[last_letters] = frequency
    else:
      frequency = letter_frequencies[last_letters]
    if letter not in frequency:
      frequency[letter] = 1
    else:
      frequency[letter] += 1

  with codecs.open(file_name, 'r', encoding='utf8') as inf:
    for line in inf:
      for word in line.split():
        last_letters = u'  '
        for letter in word:
          addLetter(last_letters, letter)
          last_letters = last_letters[-1]+letter
        addLetter(word[-2:], u' ')

  return letter_frequencies


def _generateName(vocabulary):
  """Generates a single name from the given vocabulary data."""
  def getLetter(last_letter):
    if last_letter not in vocabulary:
      raise ValueError(last_letter)
    frequencies = vocabulary[last_letter]
    max_frequency = 0
    for _,count in frequencies.iteritems():
      max_frequency += count
    index = random.randint(0, max_frequency)
    for letter,count in frequencies.iteritems():
      index -= count
      if index <= 0:
        return letter
    return ' '

  word = getLetter(u'  ')
  word += getLetter(u' '+word[-1])
  while word[-1] != u' ' and len(word) < 10:
    word += getLetter(word[-2:])
  return unicode(word.strip())


# Just for testing...
if __name__ == "__main__":
  logging.basicConfig(level=logging.DEBUG)

  inf = os.path.dirname(__file__)+"/vocabulary/elvish.txt"
  vocab = _parseVocabulary(inf)
  for _ in range(20):
    print _generateName(vocab)
