
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


class DocViewPage(DocPage):
  def get(self, slug):
    if not slug:
      self.redirect("/doc/")
      return
    if not self.request.get("revision"):
      page = ctrl.doc.getPage(slug)
    else:
      page = ctrl.doc.getPageRevision(slug, self.request.get("revision"))
    if not page:
      self.render("doc/not_found.html", {"slug": slug})
    else:
      data = {"page": page}
      tmpl_name = "doc/page_view.html"
      if self.request.get("revision"):
        tmpl_name = "doc/page_viewrevision.html"
      if self.request.get("diff"):
        previous_revision = ctrl.doc.getPageRevision(slug, self.request.get("diff"))
        data["diff"] = ctrl.doc.generateDiff(previous_revision.revisions[0], page.revisions[0])
        tmpl_name = "doc/page_viewdiff.html"
      data["profiles"] = self._getProfiles(page.revisions)
      self.render(tmpl_name, data)


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


def calculate_percents(revision):
  if revision.words_added == None:
    return
  # Add a % value for displaying in a nice graph
  total_changes = revision.words_added + revision.words_removed + revision.words_changed
  if total_changes < len(revision.words):
    total_changes = len(revision.words)
  revision.words_added_pct = int(100.0 * revision.words_added / total_changes)
  revision.words_removed_pct = int(100.0 * revision.words_removed / total_changes)
  revision.words_changed_pct = int(100.0 * revision.words_changed / total_changes)


class DocRevisionHistoryPage(DocPage):
  def get(self):
    if not self._isLoggedIn():
      return
    slug = self.request.get("slug")
    if slug[0] != '/':
      # Invalid slug
      self.render("doc/not_found.html", {})
      self.response.set_status(404)
      return
    page = ctrl.doc.getPage(slug)
    if not page:
      self.render("doc/not_found.html", {})
      self.response.set_status(404)
      return
    revisions = ctrl.doc.getRevisionHistory(page.key)
    # Add a % value for displaying in a nice graph
    for revision in revisions:
      calculate_percents(revision)
    profiles = self._getProfiles(revisions)
    self.render("doc/revision_history.html", {"page": page,
                                              "revisions": revisions,
                                              "profiles": profiles})


class DocGlobalRevisionHistoryPage(DocPage):
  """Like DocRevisionHistoryPage, but for the entire site not just one page."""
  def get(self):
    if not self._isLoggedIn():
      return
    pages = ctrl.doc.getGlobalRevisionHistory()
    all_revisions = []
    for entry in pages:
      all_revisions.append(entry["revision"])
    # Add a % value for displaying in a nice graph
    for revision in all_revisions:
      calculate_percents(revision)
    profiles = self._getProfiles(all_revisions)
    self.render("doc/revision_history_global.html", {"pages": pages, "profiles": profiles})


class DocRevisionRevert(DocPage):
  def get(self):
    if not self._isLoggedIn():
      return
    slug = self.request.get("slug")
    revision_key = self.request.get("revision")
    if revision_key:
      page = ctrl.doc.getPageRevision(slug, revision_key)
    else:
      page = None
    if not page:
      self.render("doc/not_found.html", {})
      self.response.set_status(404)
    profiles = self._getProfiles(page.revisions)
    self.render("doc/revision_revert.html", {"page": page,
                                             "revision": page.revisions[0],
                                             "profiles": profiles})

  def post(self):
    if not self._isLoggedIn():
      return
    revision_key = self.request.POST.get("key")
    ctrl.doc.revertTo(revision_key, self.user)
    self.redirect("/doc" + self.request.POST.get("slug"))


app = webapp.WSGIApplication([("/doc/_/edit", DocEditPage),
                              ("/doc/_/revisions", DocRevisionHistoryPage),
                              ("/doc/_/revert", DocRevisionRevert),
                              ("/doc/_/recent-changes", DocGlobalRevisionHistoryPage),
                              ("/doc(/.*)?", DocViewPage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
