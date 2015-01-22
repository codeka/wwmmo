
import os
from datetime import datetime
import re
import urllib
import webapp2 as webapp

from google.appengine.api import users
from google.appengine.api import memcache

import ctrl.doc
import model.doc
import handlers


class DocPage(handlers.BaseHandler):
  def _getProfiles(self, revisions):
    user_ids = []
    for revision in revisions:
      if revision.user.user_id() not in user_ids:
        user_ids.append(revision.user.user_id())
    return ctrl.profile.getProfiles(user_ids)

  pass


class DocViewPage(DocPage):
  def get(self, slug):
    if not slug:
      self.redirect("/doc/")
      return
    page = ctrl.doc.getPage(slug)
    if not page:
      self.render("doc/not_found.html", {"slug": slug})
    else:
      profiles = self._getProfiles(page.revisions)
      self.render("doc/page_view.html", {"page": page, "profiles": profiles})


class DocEditPage(DocPage):
  """This page is for editing documents."""
  def get(self):
    if not self._isLoggedIn():
      return
    slug = self.request.get("slug")
    page = ctrl.doc.getPage(slug)
    if not page:
      page = ctrl.doc.DocPage()
      page.title = ""
      page.content = ""
    self.render("doc/page_edit.html", {"slug": slug, "page": page})

  def post(self):
    if not self._isLoggedIn():
      return
    slug = self.request.get("slug")
    if slug[0] != "/":
      # TODO: invalid slug!
      return
    slug = slug.lower()
    page = ctrl.doc.getPage(slug)
    if not page:
      page = ctrl.doc.DocPage()
    page.title = self.request.POST.get("page-title")
    page.slug = slug
    page.content = self.request.POST.get("page-content")
    page.updatedDate = datetime.now()
    page.updatedUser = self.user
    ctrl.doc.savePage(page)
    self.redirect("/doc"+slug)


class DocDeletePage(DocPage):
  def get(self):
    if not self._isLoggedIn():
      return
    slug = self.request.get("slug")
    page = ctrl.doc.getPage(slug)
    if not page:
      self.response.set_status(404)
      return
    self.render("doc/page_delete.html", {"slug": slug, "page": page})

  def post(self):
    if not self._isLoggedIn():
      return
    key = self.request.POST.get("key")
    ctrl.doc.deletePage(key)
    self.redirect("/doc/")


class DocRevisionHistoryPage(DocPage):
  def get(self):
    slug = self.request.get("slug")
    if slug[0] != '/':
      # TODO: invalid slug
      return
    page = ctrl.doc.getPage(slug)
    if not page:
      self.response.set_status(404)
      return
    revisions = ctrl.doc.getRevisionHistory(page.key)
    for revision in revisions:
      # Add a % value for displaying in a nice graph
      total_changes = revision.words_added + revision.words_removed + revision.words_changed
      if total_changes < len(revision.words):
        total_changes = len(revision.words)
      revision.words_added_pct = int(100.0 * revision.words_added / total_changes)
      revision.words_removed_pct = int(100.0 * revision.words_removed / total_changes)
      revision.words_changed_pct = int(100.0 * revision.words_changed / total_changes)
    profiles = self._getProfiles(revisions)
    self.render("doc/revision_history.html", {"page": page,
                                              "revisions": revisions,
                                              "profiles": profiles})


app = webapp.WSGIApplication([("/doc/_/edit", DocEditPage),
                              ("/doc/_/delete", DocDeletePage),
                              ("/doc/_/revisions", DocRevisionHistoryPage),
                              ("/doc(/.*)?", DocViewPage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
