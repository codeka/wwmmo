
from HTMLParser import HTMLParser
import os
import re

from google.appengine.api import memcache


def findCursor(query, keyname, page_no, page_size):
  """Finds the cursor to use for fetching results from the given page.

  We store a mapping of page_no->cursor in memcache. If this result is missing, we look for page_no-1, if that's
  missing we look for page_no-2 and so on. Once we've found one (or we get back to page_no=0) then we need to fetch
  results from that page forward, storing the results back in memcache as we go.

  Args:
    query: A query used to fetch data from the data store
    keyname: A string that'll make the keys unique (e.g. all blog posts could have keyname='blog'
    page_no: The page number we're after
    page_size: The size of pages we're after"""
  cursor_page = page_no
  cursor = memcache.get('post-page-cursor:%s:%d:%d' % (keyname, cursor_page, page_size))
  while not cursor:
    cursor_page -= 1
    if cursor_page == 0:
      break
    cursor = memcache.get('post-page-cursor:%s:%d:%d' % (keyname, cursor_page, page_size))

  while cursor_page < page_no:
    # if we have to fast-forward through pages then we'll store the pages in memcache as we go
    if cursor_page == 0:
      it = query.run()
    else:
      it = query.with_cursor(cursor)
    n = 0
    for _ in it:
      n += 1
      if n >= page_size:
        break
    cursor = query.cursor()
    cursor_page += 1
    memcache.set('post-page-cursor:%s:%d:%d' % (keyname, cursor_page, page_size), cursor)

  return cursor


def makeSlug(tagName):
  slug = tagName.lower().replace(' ', '-')
  slug = re.sub(r'[^a-zA-Z0-9_-]+', '', slug)
  return slug


def isDevelopmentServer():
  """Returns True if we're running on the development server."""
  return os.environ["SERVER_SOFTWARE"].startswith("Development")
