
from datetime import datetime
import os
import logging
import time
import traceback
import webapp2 as webapp

from google.appengine.api import mail
from google.appengine.api import urlfetch

import cron
import ctrl.tmpl
import model.ping


# This is the URL that we'll ping. It's the home star of the 'Hunters' test user.
# This is a good URL to test because it does a bunch of reads and writes to the database,
# and a bunch of CPU, so it should be a good well-rounded test of everything.
PING_URL = "https://game.war-worlds.com/realms/beta/stars/268299/simulate?nolog=1&update=1"


class BasePage(cron.BaseHandler):
  pass


def notifySiteDown():
  """Sends an email notification when we detect that the game is down."""
  tmpl = ctrl.tmpl.getTemplate("email/ping_sitedown.txt")

  body = ctrl.tmpl.render(tmpl, {})
  sender = "ping@warworldssite.appspotmail.com"
  recipient = "dean@codeka.com.au"
  logging.info("Sending email: {from:"+sender+", recipient:"+recipient+", subject:URGENT: WAR WORLDS IS DOWN, body:"+str(len(body))+" bytes")
  mail.send_mail(sender, recipient, "URGENT: WAR WORLDS IS DOWN", body)


class PingGame(BasePage):
  def get(self):
    """This is a cron job that pings a URL in the game, designed to test whether the game is up & healthy."""
    ping = model.ping.Ping()
    ping.date = datetime.now()
    start = time.time()
    try:
      result = urlfetch.fetch(url=PING_URL, method=urlfetch.POST, deadline=10,
                              validate_certificate=True)
      ping.response_status = result.status_code
    except:
      ping.error = str(traceback.format_exc())
      ping.response_status = -1
      notifySiteDown()
    ping.response_time = int((time.time() - start) * 1000.0)
    ping.put()


app = webapp.WSGIApplication([("/cron/ping/ping-game", PingGame)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
