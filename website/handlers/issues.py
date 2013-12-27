
import os
from datetime import datetime
import logging
import math
import re
import urllib
import webapp2 as webapp

from google.appengine.api import users
from google.appengine.api import memcache

import ctrl
import ctrl.issues
import ctrl.profile
import model.issues
import handlers


class IssuesPage(handlers.BaseHandler):
  pass


class IssuesListPage(IssuesPage):
  def get(self):
    self.render("issues/issues_list.html", {})


class IssuesCreatePage(IssuesPage):
  def get(self):
    self.render("issues/issues_edit.html", {})
  def post(self):
    id = ctrl.issues.createIssue(self.request.POST.get("issue-summary"),
                                 self.request.POST.get("issue-description"),
                                 self.user)
    self.redirect("/issues/"+str(id))


class IssuesViewPage(IssuesPage):
  def get(self, issue_id):
    data = {}
    issue, updates = ctrl.issues.getIssue(int(issue_id))
    data["issue"] = issue
    data["updates"] = updates

    user_ids = [issue.creator_user.user_id()]
    for update in updates:
      if update.user.user_id() not in user_ids:
        user_ids.append(update.user.user_id())
    profiles = ctrl.profile.getProfiles(user_ids)
    data["profiles"] = profiles

    self.render("issues/issues_view.html", data)

  def post(self, issue_id):
    """Posting to the issue page we create an IssueUpdate for you."""
    issue, updates = ctrl.issues.getIssue(int(issue_id))
    if not issue:
      self.error(404)
      return

    update = model.issues.IssueUpdate(parent=issue)
    update.posted = datetime.now()
    update.user = self.user
    if self.request.POST.get("issue-comment"):
      update.comment = ctrl.sanitizeHtml(self.request.POST.get("issue-comment"))
    ctrl.issues.saveUpdate(issue, update)

    # redirect back with a GET
    self.redirect("/issues/"+str(issue_id))


app = webapp.WSGIApplication([("/issues/?", IssuesListPage),
                              ("/issues/new", IssuesCreatePage),
                              ("/issues/([0-9]+)", IssuesViewPage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
