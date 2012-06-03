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


app = webapp.WSGIApplication([("/", MainPage)],
                              debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
