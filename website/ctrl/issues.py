
from google.appengine.api import search 
from google.appengine.ext import db
from google.appengine.ext import deferred

import ctrl
import model.issues


def searchIssues(query):
  index = search.Index("issue")
  results = index.search(search.Query(query_string=query,
        options=search.QueryOptions(ids_only=True)))

  issues = []
  for doc in results:
    issues.append(model.issues.Issue.get(doc.doc_id))
  return issues


def createIssue(summary, type, priority, description, user):
  ids = db.allocate_ids(db.Key.from_path(model.issues.Issue.__name__, 1), 1)
  key = db.Key.from_path(model.issues.Issue.__name__, ids[0])
  issue = model.issues.Issue(key=key, summary=summary, description=description, creator_user=user,
                             type=type, priority=priority, state=model.issues.State.New, resolution=None)
  issue.put()
  indexIssue(issue, [])
  return ids[0]


def getIssue(id):
  key = db.Key.from_path(model.issues.Issue.__name__, id)
  issue = model.issues.Issue.get(key)

  query = model.issues.IssueUpdate.all().ancestor(issue).order("posted")
  updates = []
  for update in query:
    updates.append(update)

  return issue, updates


def getPossibleActions(issue):
  """Gets a list of the possible actions you can take on the given issue."""
  if issue.state == model.issues.State.New:
    return [(model.issues.Action.Leave, 'Leave New'),
            (model.issues.Action.Open, 'Accept Bug'),
            (model.issues.Action.CloseFixed, 'Fixed'),
            (model.issues.Action.CloseDupe, 'Duplicate'),
            (model.issues.Action.CloseWorksForMe, 'Works for me'),
            (model.issues.Action.CloseReject, 'Reject')]
  elif issue.state == model.issues.State.Open:
    return [(model.issues.Action.Leave, 'Leave Open'),
            (model.issues.Action.CloseFixed, 'Fixed'),
            (model.issues.Action.CloseDupe, 'Duplicate'),
            (model.issues.Action.CloseWorksForMe, 'Works for me'),
            (model.issues.Action.CloseReject, 'Reject')]
  else: # state == model.issues.State.Closed
    return [(model.issues.Action.Leave, 'Leave Closed'),
            (model.issues.Action.Open, 'Reopen')]


def saveUpdate(issue, updates, update):
  # TODO: check if they have permission to do this
  issue_updated = False
  if update.action == model.issues.Action.Open:
    issue.state = model.issues.State.Open
    issue.resolution = None
    issue_updated = True
  elif update.action == model.issues.Action.CloseFixed:
    issue.state = model.issues.State.Closed
    issue.resolution = model.issues.Resolution.Fixed
  elif update.action == model.issues.Action.CloseDupe:
    issue.state = model.issues.State.Closed
    issue.resolution = model.issues.Resolution.Duplicate
    #issue.duplicateOf = update.duplicateOf
    issue_updated = True
  elif update.action == model.issues.Action.CloseWorksForMe:
    issue.state = model.issues.State.Closed
    issue.resolution = model.issues.Resolution.WorksForMe
    issue_updated = True
  elif update.action == model.issues.Action.CloseReject:
    issue.state = model.issues.State.Closed
    issue.resolution = model.issues.Resolution.Rejected
    issue_updated = True

  if update.new_priority:
    issue.priority = update.new_priority
    issue_updated = True
  if update.new_type:
    issue.type = update.new_type
    issue_updated = True

  if issue_updated:
    issue.put()
  update.put()
  updates.append(update)
  indexIssue(issue, updates)


def _indexIssue(issue, updates):
  """Does the actual work of indexing the given issue. We expect to be called in a deferred handler."""
  fields = [search.TextField(name="summary", value=issue.summary),
            search.TextField(name="description", value=issue.description),
            search.AtomField(name="id", value=str(issue.key().id_or_name())),
            search.AtomField(name="type", value=issue.type),
            search.NumberField(name="priority", value=issue.priority),
            search.AtomField(name="state", value=issue.state),
            search.AtomField(name="resolution", value=issue.resolution)]

  if not updates:
    issue, updates = getIssue(issue.key().id())

  comments = ""
  for update in updates:
    if update.comment:
      if comments:
        comments += "\r\n<hr />\r\n"
      comments += update.comment
  fields.append(search.HtmlField(name="comments", value=comments))

  doc = search.Document(
    doc_id = str(issue.key()),
    fields = fields)

  index = search.Index(name="issue")
  index.put(doc)


def indexIssue(issue, updates = None):
  """Queues the given issue to be indexed."""
  deferred.defer(_indexIssue, issue, updates, _queue="issuesync")
