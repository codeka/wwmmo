"""sector.py: Handlers for the /_ah/* URLs."""

import os
import webapp2 as webapp

import tasks


class WarmupPage(tasks.TaskPage):
  """This is called as a warmup request, when a new instance of the app spins up."""
  def get(self):
    pass


app = webapp.WSGIApplication([("/_ah/warmup", WarmupPage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))

