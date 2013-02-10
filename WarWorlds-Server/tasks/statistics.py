"""statistics.py: Generate usage statistics and such-like."""

from datetime import datetime, timedelta
import os
import webapp2 as webapp

from google.appengine.ext import deferred

import model as mdl
from model import statistics as stats_mdl
import tasks


def _count_nday_actives(num_days, first_day):
  query = mdl.LoginHistory.all().filter("date >", first_day)
  logins = {}
  for login in query:
    empire_key = mdl.LoginHistory.empire.get_value_for_datastore(login)
    if empire_key in logins:
      logins[empire_key] += 1
    else:
      logins[empire_key] = 1

  actives = stats_mdl.ActiveEmpires()
  actives.date = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
  actives.numDays = num_days
  actives.actives = len(logins)
  actives.put()


def count_nday_actives(num_days):
  """Goes through the LoginHistory objects and counts the number of distinct empires."""
  first_day = datetime.now() - timedelta(num_days)
  _count_nday_actives(num_days, first_day)


class GeneratePage(tasks.TaskPage):
  def get(self):
    deferred.defer(count_nday_actives, 1,
                   _queue="statistics")
    deferred.defer(count_nday_actives, 7,
                   _queue="statistics")


app = webapp.WSGIApplication([("/tasks/stats/generate", GeneratePage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))

