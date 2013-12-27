
from google.appengine.ext import db

import ctrl
import model.issues


def createIssue(summary, description, user):
  ids = db.allocate_ids(db.Key.from_path(model.issues.Issue.__name__, 1), 1)
  key = db.Key.from_path(model.issues.Issue.__name__, ids[0])
  issue = model.issues.Issue(key=key, summary=summary, description=description, creator_user=user)
  issue.put()
  return ids[0]


def getIssue(id):
  key = db.Key.from_path(model.issues.Issue.__name__, id)
  issue = model.issues.Issue.get(key)

  query = model.issues.IssueUpdate.all().ancestor(issue)
  updates = []
  for update in query:
    updates.append(update)

  return issue, updates


def saveUpdate(issue, update):
  update.put()
