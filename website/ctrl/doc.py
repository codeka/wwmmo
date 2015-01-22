
import datetime
import difflib
import re
import logging
import random

from google.appengine.api import memcache
from google.appengine.ext import db

import model.doc

import html2text


class DocPage(object):
  """Represents everything we need to display a specific page."""
  def __init__(self):
    self.key = None
    self.title = None
    self.content = None
    self.slug = None
    self.revisions = []


class DocRevision(object):
  def __init__(self):
    self.key = None
    self.content = None
    self.user = None
    self.date = None
    self.words = None
    self.words_added = None
    self.words_removed = None
    self.words_changed = None


def getPage(slug, revisionKey=None):
  """Gets the document page with the give slug."""
  page_mdl = None
  for mdl in model.doc.DocPage.all().filter("slug", slug).fetch(1):
    page_mdl = mdl
    break
  if not page_mdl:
    return None

  # Fetch the last four revisions. The latest one is the current, and
  # then we want the rest so we can display a little history on the page
  # as well (who edited the page, and when).
  page = DocPage()
  page.key = str(page_mdl.key())
  page.title = page_mdl.title
  page.slug = page_mdl.slug

  if revisionKey:
    rev_mdl = model.doc.DocPageRevision.get(db.Key(revisionKey))
    if not rev_mdl:
      return None
    page.content = rev_mdl.content
    revision = DocRevision()
    revision.key = str(rev_mdl.key())
    revision.date = rev_mdl.date
    revision.user = rev_mdl.user
    page.revisions.append(revision)

  for rev_mdl in (model.doc.DocPageRevision.all()
                                           .ancestor(page_mdl)
                                           .order("-date")
                                           .fetch(4)):
    if not page.content:
      page.content = rev_mdl.content
    revision = DocRevision()
    revision.key = str(rev_mdl.key())
    revision.date = rev_mdl.date
    revision.user = rev_mdl.user
    page.revisions.append(revision)
  return page


def getRevisionHistory(page_key):
  revisions = []
  prev_rev = None
  rev = None
  for rev_mdl in (model.doc.DocPageRevision.all()
                                           .ancestor(db.Key(page_key))
                                           .order("-date")):
    rev = DocRevision()
    rev.key = str(rev_mdl.key())
    rev.content = rev_mdl.content
    rev.user = rev_mdl.user
    rev.date = rev_mdl.date
    if prev_rev:
    	_populateDelta(rev, prev_rev)
    prev_rev = rev
    revisions.append(rev)
  if rev and prev_rev:
    _populateDelta(rev, prev_rev)
  return revisions


def revertTo(revision_key, user):
  """Reverting a revision is simple, just re-save it as if it was brand new."""
  rev_mdl = model.doc.DocPageRevision.get(db.Key(revision_key))
  if not rev_mdl:
    return
  new_rev_mdl = model.doc.DocPageRevision(parent=rev_mdl.parent(),
                                          content=rev_mdl.content,
                                          user=user,
                                          date=datetime.datetime.now())
  new_rev_mdl.put()


def savePage(page):
  """Saves the given page to the data store."""
  if not page.key:
    page_mdl = model.doc.DocPage(slug=page.slug, title=page.title)
  else:
    page_mdl = model.doc.DocPage.get(db.Key(page.key))
    page_mdl.title = page.title
  page_mdl.put()

  rev_mdl = model.doc.DocPageRevision(parent=page_mdl,
                                      content=page.content,
                                      user=page.updatedUser,
                                      date=page.updatedDate)
  rev_mdl.put()


def deletePage(key):
  page_mdl = model.doc.DocPage.get(db.Key(key))
  page_mdl.delete()


def _populateDelta(older_rev, newer_rev):
  """Populates the delta between the older revision and the newer."""
  if not older_rev.words:
    older_rev.words = _splitWords(older_rev.content)
  if not newer_rev.words:
    newer_rev.words = _splitWords(newer_rev.content)
  newer_rev.words_added = 0
  newer_rev.words_removed = 0
  newer_rev.words_changed = 0
  diff = difflib.ndiff(older_rev.words, newer_rev.words)

  last_change = ' '
  for line in diff:
    if line[0] == '+':
      newer_rev.words_added += 1
    elif line[0] == '-':
      newer_rev.words_removed += 1
    elif line[0] == '?':
      if last_change == '+':
        newer_rev.words_added -= 1
      elif last_change == '-':
        newer_rev.words_removed -= 1
      newer_rev.words_changed += 1


_htmlSplitRegex = re.compile(r"(\s*<[^>]+>\s*)")
_wordSplitRegex = re.compile(r"\s+")

def _splitWords(content):
  """Splits the given string into words.

  We first split the words into HTML tags and "text", then further split
  the text into words (by spaces). For example, the following string:

  Hello World, <a href="index.html">Link</a>

  Will be split into:

  ['Hello', 'World,', '<a href="index.html">', 'Link', '</a>']"""
  # Santize the input a little.
  content = content.replace("&nbsp;", " ")
  words = []
  for entry in _htmlSplitRegex.split(content):
    if entry.strip() == "":
      continue
    elif entry[0] == '<':
      words.append(entry)
    else:
      words.extend(_wordSplitRegex.split(entry))
  return words
