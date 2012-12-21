"""frontend.py: The 'frontend' is just a dummy page, "nothing to see"."""

import os

from google.appengine.api import users

import jinja2
import webapp2 as webapp

jinja = jinja2.Environment(loader=jinja2.FileSystemLoader(os.path.dirname(__file__)+"/tmpl"))


class MainPage(webapp.RequestHandler):
  def get(self):
    user = users.get_current_user()
    if user:
      logout_url = users.create_logout_url(self.request.uri)
    else:
      logout_url = None

    tmpl = jinja.get_template("frontend/index.html")
    self.response.out.write(tmpl.render({"logout_url": logout_url}))


class IntelPage(webapp.RequestHandler):
  def get(self):
    user = users.get_current_user()
    if not user:
      self.redirect(users.create_login_url(self.request.uri))
    else:
      tmpl = jinja.get_template("intel/index.html")
      self.response.out.write(tmpl.render({}))


app = webapp.WSGIApplication([("/", MainPage),
                              ("/intel", IntelPage)],
                              debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
