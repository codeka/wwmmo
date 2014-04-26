
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
  def _isWriter(self):
    if not self.user:
      return False
    writers = memcache.get("docs:writers")
    if not writers:
      writers = []
      for writer in model.doc.DocWriter.all():
        writers.append(writer.user.email())
      memcache.set("docs:writers", writers)
    return self.user.email() in writers


class DocViewPage(DocPage):
  def get(self, slug):
    if not slug:
      self.redirect("/doc/")
      return
    page = ctrl.doc.getPage(slug)
    if not page:
      self.render("doc/not_found.html", {"slug": slug})
    else:
      self.render("doc/page_view.html", {"page": page, "is_writer": self._isWriter()})


class DocEditPage(DocPage):
  """This page is for editing documents."""
  def get(self):
    if not self._isLoggedIn():
      return
    slug = self.request.get("slug")
    if not self._isWriter():
      self.redirect("/doc/" + slug)
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
    if not self._isWriter():
      self.redirect("/doc/" + slug)
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
    if not self._isWriter():
      self.redirect("/doc/" + slug)
    page = ctrl.doc.getPage(slug)
    if not page:
      self.response.set_status(404)
      return
    self.render("doc/page_delete.html", {"slug": slug, "page": page})

  def post(self):
    if not self._isLoggedIn():
      return
    if not self._isWriter():
      self.redirect("/doc/")
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
    self.render("doc/revision_history.html", {"page": page,
                                              "revisions": revisions})


app = webapp.WSGIApplication([("/doc/_/edit", DocEditPage),
                              ("/doc/_/delete", DocDeletePage),
                              ("/doc/_/revisions", DocRevisionHistoryPage),
                              ("/doc(/.*)?", DocViewPage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
