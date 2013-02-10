"""admin.py: Contains web handlers for the admin interface."""

from datetime import datetime, timedelta
import os
import logging
import json

from google.appengine.api import users
from google.appengine.ext.db.metadata import Kind

import import_fixer
import_fixer.FixImports("google", "protobuf")

import protobufs.protobuf_json
import protobufs.messages_pb2 as pb

import jinja2
import webapp2 as webapp
from mapreduce import control

import model
from model import session
from model import statistics as stats_mdl
from ctrl import empire


Jinja = jinja2.Environment(loader=jinja2.FileSystemLoader(os.path.dirname(__file__)+"/tmpl"))


class AdminPage(webapp.RequestHandler):
  """This is the base class for pages in the admin section."""

  def render(self, tmplName, args):
    """Renders the given template with the given arguments.

    Args:
      tmplName: The name of the template to render, assumed to be under the tmpl/ folder.
      args: A dictionary of arguments to supply to the template.
    """

    args["logout_url"] = users.create_logout_url("/")
    args["logged_in_user"] = self.user.email()
    args["msgs"] = self.session.getValue("msgs", [])
    if args["msgs"] != []:
      self.session.setValue("msgs", [])

    tmpl = Jinja.get_template(tmplName)
    self.response.out.write(tmpl.render(args))

  def dispatch(self):
    """Checks that a user is logged in and such before we process the request."""

    self.user = users.get_current_user()
    if not self.user:
      # not logged in, so redirect to the login page
      self.redirect(users.create_login_url(self.request.uri))
      return

    # TODO: better handling of authorization... one email address ain't enough!
    if self.user.email() != "dean@codeka.com.au":
      # not authorized to view the backend, redirect to the home page instead
      self.redirect('/')
      return

    # load up the current session
    self.session = session.Session.attach(self)

    super(AdminPage, self).dispatch()

    # save the current session
    self.session.detach()

  def addMessage(self, msg):
    """Adds a message to be displayed the next time a page is rendered.

    Args:
      msg: A message to display the next time a page is rendered. This gets saved to the session
          and restored on next page load.
    """

    msgs = self.session.getValue("msgs", [])
    msgs.append(msg)
    self.session.setValue("msgs", msgs)

class DashboardPage(AdminPage):
  """The "dashboard" page, basically what you get when you visit /admin."""

  def get(self):
    empire_nda = {}
    query = (stats_mdl.ActiveEmpires.all().filter("date >", datetime.now() - timedelta(days=30))
                                    .order("date"))
    for nda in query:
      date = nda.date.replace(hour=0, minute=0, second=0, microsecond=0)
      if date in empire_nda:
        values = empire_nda[date]
      else:
        values = {"date": date}
        empire_nda[date] = values
      values[str(nda.numDays)+"da"] = nda.actives
    data = {"empire_nda": empire_nda.values()}
    self.render("admin/dashboard.html", data)


class ChatPage(AdminPage):
  """The chat page lets us chat with all players, make real-time announcements and whatnot."""

  def get(self):
    self.render("admin/chat.html", {})


class DebugEmpiresPage(AdminPage):
  """The 'empires' page lets you view, query, update, delete (etc) empires."""

  def get(self):
    self.render("admin/debug/empires.html", {})


class MotdPage(AdminPage):
  """The "motd" page lets you view/edit the message-of-the-day."""

  def get(self):
    motd = model.MessageOfTheDay.get()
    self.render("admin/motd.html", {"motd": motd})

  def post(self):
    model.MessageOfTheDay.save(self.request.get("new-motd"))

    # redirect back to ourselves...
    self.redirect(self.request.url)


class DebugStarfieldPage(AdminPage):
  def get(self):
    self.render("admin/debug/starfield.html", {})


class DebugReportsPage(AdminPage):
  def get(self):
    self.render("admin/debug/reports.html", {})


class DebugReportsAjaxPage(AdminPage):
  """Helper for the debug/report page that gets called via Ajax.

  The reports will have serialized protocol buffers in them, and because JavaScript doesn't have
  any way to deserialize protocol buffers, we act as an intermediary to do it on the server and
  return JSON directly."""
  def get(self):
    action = self.request.get("action")
    star_key = self.request.get("starKey")

    user = users.User(self.request.get("on_behalf_of"))
    curr_empire_pb = empire.getEmpireForUser(user)
    if action == "scout-reports":
      scout_reports_pb = empire.getScoutReports(star_key, curr_empire_pb.key)
      scout_reports = protobufs.protobuf_json.pb2obj(scout_reports_pb)
      for report in scout_reports["reports"]:
        star_pb = pb.Star()
        star_pb.ParseFromString(report["star_pb"].decode("string_escape"))
        report["star_pb"] = protobufs.protobuf_json.pb2obj(star_pb)
      self.response.headers["Content-Type"] = "application/json"
      self.response.write(json.dumps(scout_reports))



class DebugColoniesPage(AdminPage):
  def get(self):
    self.render("admin/debug/colonies.html", {})


class DebugDataStorePage(AdminPage):
  def get(self):
    kinds = []
    q = Kind.all()
    for p in q:
      kinds.append(p.kind_name)
    self.render("admin/debug/data-store.html", {"kinds": kinds})

  def post(self):
    if self.request.get("action") == "bulk-delete":
      entityKind = self.request.get("entity-kind")
      logging.info("bulk-deleting kind: %s" % (entityKind))

      control.start_map(name="Bulk Delete",
                        handler_spec="tasks.datastore.bulkdelete",
                        reader_spec="mapreduce.input_readers.DatastoreEntityInputReader",
                        mapper_parameters={"entity_kind": entityKind})

      # redirect back to ourselves TODO: butter bar message
      self.addMessage("Entity '%s' has been bulk-deleted." % (entityKind))
      self.redirect(self.request.url)
    else:
      self.response.set_status(400)


class DevicesPage(AdminPage):
  """The "devices" page lets you view all devices that have registered and send them messages."""

  def get(self):
    self.render("admin/devices.html", {})


app = webapp.WSGIApplication([("/admin", DashboardPage),
                              ("/admin/chat", ChatPage),
                              ("/admin/motd", MotdPage),
                              ("/admin/devices", DevicesPage),
                              ("/admin/debug/empires", DebugEmpiresPage),
                              ("/admin/debug/starfield", DebugStarfieldPage),
                              ("/admin/debug/data-store", DebugDataStorePage),
                              ("/admin/debug/colonies", DebugColoniesPage),
                              ("/admin/debug/reports", DebugReportsPage),
                              ("/admin/debug/reports-ajax", DebugReportsAjaxPage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))

