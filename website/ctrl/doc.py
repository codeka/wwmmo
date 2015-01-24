
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
    self.page_key = None
    self.content = None
    self.user = None
    self.date = None
    self.older_revision_key = None
    self.words = None
    self.words_added = None
    self.words_removed = None
    self.words_changed = None


def getPage(slug):
  """Gets the document page with the give slug."""
  cache_key = "doc:%s" % (slug)
  page = memcache.get(cache_key)
  if page:
    return page

  page_mdl = None
  for mdl in model.doc.DocPage.all().filter("slug", slug).fetch(1):
    page_mdl = mdl
    break
  if not page_mdl:
    return None

  page = DocPage()
  page.key = str(page_mdl.key())
  page.title = page_mdl.title
  page.slug = page_mdl.slug

  # Fetch the last four revisions. The latest one is the current, and
  # then we want the rest so we can display a little history on the page
  # as well (who edited the page, and when).
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

  memcache.set(cache_key, page)
  return page


def getPageRevision(slug, revision_key):
  """Fetches a single revision of the page with the given slug."""
  page_mdl = None
  for mdl in model.doc.DocPage.all().filter("slug", slug).fetch(1):
    page_mdl = mdl
    break
  if not page_mdl:
    return None

  page = DocPage()
  page.key = str(page_mdl.key())
  page.title = page_mdl.title
  page.slug = page_mdl.slug

  rev_mdl = model.doc.DocPageRevision.get(db.Key(revision_key))
  if not rev_mdl:
    return None
  page.content = rev_mdl.content
  revision = DocRevision()
  revision.key = str(rev_mdl.key())
  revision.content = rev_mdl.content
  revision.date = rev_mdl.date
  revision.user = rev_mdl.user
  page.revisions.append(revision)

  return page


def getRevisionHistory(page_key):
  query = (model.doc.DocPageRevision.all()
                                    .ancestor(db.Key(page_key))
                                    .order("-date"))
  return _getRevisionHistory(query)


def getGlobalRevisionHistory():
  query = (model.doc.DocPageRevision.all()
                                    .order("-date")
                                    .fetch(20))
  revisions = _getRevisionHistory(query)
  pages = []
  page_map = {}
  for revision in revisions:
    if revision.page_key not in page_map:
      page_mdl = model.doc.DocPage.get(db.Key(revision.page_key))
      if not page_mdl:
        continue
      page = DocPage()
      page.key = str(page_mdl.key())
      page.title = page_mdl.title
      page.slug = page_mdl.slug
      page_map[revision.page_key] = page
    page = page_map[revision.page_key]
    pages.append({"page": page, "revision": revision})
  return pages


def _getRevisionHistory(query):
  revisions = []
  prev_rev = None
  rev = None
  for rev_mdl in query:
    cache_key = "doc-rev-history:%s" % (str(rev_mdl.key()))
    rev = memcache.get(cache_key)
    if rev:
      revisions.append(rev)
    else:
      rev = DocRevision()
      rev.key = str(rev_mdl.key())
      rev.page_key = str(rev_mdl.key().parent())
      rev.content = rev_mdl.content
      rev.user = rev_mdl.user
      rev.date = rev_mdl.date
      if prev_rev and prev_rev.page_key == rev.page_key:
        _populateDelta(rev, prev_rev)
        prev_rev.older_revision_key = rev.key
        memcache.set("doc-rev-history:%s" % (prev_rev.key), prev_rev)
      revisions.append(rev)
    prev_rev = rev
  if rev and prev_rev and prev_rev.page_key == rev.page_key:
    _populateDelta(rev, prev_rev)
    prev_rev.older_revision_key = rev.key
    memcache.set("doc-rev-history:%s" % (prev_rev.key), prev_rev)
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

  memcache.delete("doc:" + page.slug)
  rev_mdl = model.doc.DocPageRevision(parent=page_mdl,
                                      content=page.content,
                                      user=page.updatedUser,
                                      date=page.updatedDate)
  rev_mdl.put()


def generateDiff(older_rev, newer_rev):
  """Generates an HTML diff of the two revisions."""
  older_words = _splitWords(older_rev.content)
  newer_words = _splitWords(newer_rev.content)
  diff = difflib.ndiff(older_words, newer_words)
  html = ""
  for word in diff:
    action = word[:1]
    if '<' in word:
      html += word[2:]
    elif action == "+":
      html += "<span class=\"diff-added\"> " + word[2:] + " </span>"
    elif action == "-":
      html += "<span class=\"diff-removed\"> " + word[2:] + " </span>"
    elif action != "?":
      html += word[1:]
  return html


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
  for word in diff:
    if word[0] == '+':
      newer_rev.words_added += 1
    elif word[0] == '-':
      newer_rev.words_removed += 1
    elif word[0] == '?':
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
    elif '<' in entry:
      words.append(entry)
    else:
      words.extend(_wordSplitRegex.split(entry))
  return words
