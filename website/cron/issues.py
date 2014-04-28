
import os
from datetime import datetime
import json
import logging
import re
import urllib
import webapp2 as webapp

from google.appengine.ext import deferred
from google.appengine.api import memcache
from google.appengine.api import urlfetch
from google.appengine.api import users

import cron
import ctrl.issues
import model.issues


class BasePage(cron.BaseHandler):
  def render(self, tmplName, args):
    if not args:
      args = {}
    super(BasePage, self).render(tmplName, args)


class IndexAllIssues(BasePage):
  def get(self):
    """This is a cron job that goes through all issues and (re-)indexes them."""
    for issue in model.issues.Issue.all():
      ctrl.issues.indexIssue(issue)


app = webapp.WSGIApplication([("/cron/issues/index-all-issues", IndexAllIssues)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
