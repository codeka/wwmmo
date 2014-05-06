
import os
from datetime import datetime, timedelta
import webapp2 as webapp

import handlers
import model.ping

class StatusPage(handlers.BaseHandler):
  def get(self):
    min_date = datetime.now() - timedelta(hours=24)

    pings = []
    # TODO: cache(?)
    query = model.ping.Ping.all().filter("date >", min_date).order("date")
    for ping in query:
      pings.append(ping)

    self.render("status.html", {"pings": pings})



app = webapp.WSGIApplication([("/status/?", StatusPage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
