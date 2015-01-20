
import datetime
import re
import logging
import random

from google.appengine.api import memcache
from google.appengine.ext import db

import model.doc


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


def getPage(slug):
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
  for rev_mdl in (model.doc.DocPageRevision.all()
                                           .ancestor(db.Key(page_key))
                                           .order("-date")):
    rev = DocRevision()
    rev.key = str(rev_mdl.key())
    rev.content = rev_mdl.content
    rev.user = rev_mdl.user
    rev.date = rev_mdl.date
    revisions.append(rev)
  return revisions


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

