'''
Created on 19/02/2012

@author: dean@codeka.com.au
'''

import os, random
import logging


_generators = []


def generate(numNames, minNameSyllables = 2, maxNameSyllables = 4):
  '''Generates numNames names from the vocabularies defined in the vocabulary directory.'''
  global _generators
  _ensureGeneratorsLoaded()

  names = []
  for _ in range(numNames):
    genIndex = random.randint(0, len(_generators)-1)
    numSyllables = random.randint(minNameSyllables, maxNameSyllables)
    names.append(_generators[genIndex].compose(numSyllables))
  return names

def _ensureGeneratorsLoaded():
  '''Ensures that we've loaded the NameGenerators. We only need to do it once.'''
  global _generators
  if len(_generators) > 0:
    return

  generators = []
  rootFolder = os.path.dirname(__file__)+"/vocabulary"
  for fileName in os.listdir(rootFolder):
    if fileName.endswith(".txt"):
      path = rootFolder+"/"+fileName
      logging.info("Loading vocabulary file: "+path)
      generator = NameGenerator()
      with open(path, "r") as inf:
        generator.parse(inf)
      generators.append(generator)
  _generators = generators


class NameGenerator:
  def __init__(self):
    self.prefixes = []
    self.suffixes = []
    self.middles = []

  def parse(self, inf):
    '''Parses a vocabulary file and populates our internal data so that we can generate names.'''
    self.prefixes = []
    self.suffixes = []
    self.middles = []

    for line in inf:
      # remove comments (everything after a '#'
      line = line.split('#', 2)[0]
      line = line.strip()

      # ignore blank lines
      if line == "":
        continue

      isPrefix = False
      isSuffix = False
      if line[0] == '+':
        isPrefix = True
        line = line[1:].strip()
      elif line[0] == '-':
        isSuffix = True
        line = line[1:].strip()

      tokens = line.split()
      syllable = _Syllable()
      syllable.syllable = tokens[0].lower()
      for token in tokens[1:]:
        if token == '+v':
          syllable.requiresNextVowel = True
        elif token == '-v':
          syllable.requiresPreviousVowel = True
        elif token == '+c':
          syllable.requiresNextConsonant = True
        elif token == '-c':
          syllable.requiresPreviousConsonant = True

      syllable.startsWithVowel = self._isVowel(syllable.syllable[0])
      syllable.endsWithVowel = self._isVowel(syllable.syllable[-1])

      if isPrefix:
        self.prefixes.append(syllable)
      elif isSuffix:
        self.suffixes.append(syllable)
      else:
        self.middles.append(syllable)

  def compose(self, numSyllables):
    '''Composes a new word, returning the given number of syllables.'''
    word = []
    word.append(self.prefixes[random.randint(0, len(self.prefixes)-1)])
    for _ in range(1, numSyllables - 1):
      word.append(self._getSyllable(self.middles, word[-1]))
    word.append(self._getSyllable(self.suffixes, word[-1]))

    return self._combine(word)

  def _combine(self, syllables):
    '''Combines the given collection of syllables into a complete word.'''
    word = ""
    for s in syllables:
      word += s.syllable
    return word

  def _getSyllable(self, pool, prevSyllable):
    '''Looks in the given "pool" of syllables for a valid syllable to add to the list.

    Note that it's possible for this method to go into an infinite loop if there are no
    valid combinations... be careful! (TODO: fix this!)'''
    while True:
      nextSyllable = pool[random.randint(0, len(pool)-1)]
      if self._isValidPair(prevSyllable, nextSyllable):
        return nextSyllable

  def _isValidPair(self, prevSyllable, nextSyllable):
    '''Checks whether the given two syllables are valid together or not.'''
    if prevSyllable.requiresNextVowel and not nextSyllable.startsWithVowel:
      return False
    if prevSyllable.requiresNextConsonant and nextSyllable.startsWithVowel:
      return False

    if nextSyllable.requiresPreviousVowel and not prevSyllable.endsWithVowel:
      return False
    if nextSyllable.requiresPreviousConsonant and prevSyllable.endsWithVowel:
      return False

    return True

  def _isVowel(self, ch):
    return ch in ['a', 'e', 'i', 'o', 'u']

class _Syllable:
  syllable = None
  requiresNextVowel = False
  requiresPreviousVowel = False
  requiresNextConsonant = False
  requiresPreviousConsonant = False
  startsWithVowel = False
  endsWithVowel = True