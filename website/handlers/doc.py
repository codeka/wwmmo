
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
  """This is the base class for all doc pages."""
  def _isLoggedIn(self):
    """For pages that require a logged-in user, this can be called to
       ensure you're logged in."""
    self.user = users.get_current_user()
    if (self.user and self.user.email().lower() != "dean@codeka.com.au"
                  and self.user.email().lower() != "lam.h.emily@gmail.com"):
      self.user = None

    if not self.user:
      # not logged in, so redirect to the login page
      opts = {"continue": self.request.path_qs}
      self.redirect("/doc/_/login?"+urllib.urlencode(opts))
      return False

    return True

  def render(self, tmplName, args):
    if not args:
      args = {}
    super(DocPage, self).render(tmplName, args)


class DocLoginPage(DocPage):
  """An interstitial for when you have to log in."""
  def get(self):
    login_url = users.create_login_url(self.request.get("continue"))
    self.render("doc/login.html", {"login_url": login_url})


class DocViewPage(DocPage):
  def get(self, slug):
    if not slug:
      self.redirect("/doc/")
      return
    page = ctrl.doc.getPage(slug)
    if not page:
      self.render("doc/not_found.html", {"slug": slug})
    else:
      self.render("doc/page_view.html", {"page": page})


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
    slug = self.request.POST.get("slug")
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
    self.render("doc/revision_history.html", {"page": page,
                                              "revisions": revisions})


app = webapp.WSGIApplication([("/doc/_/login", DocLoginPage),
                              ("/doc/_/edit", DocEditPage),
                              ("/doc/_/delete", DocDeletePage),
                              ("/doc/_/revisions", DocRevisionHistoryPage),
                              ("/doc(/.*)?", DocViewPage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
