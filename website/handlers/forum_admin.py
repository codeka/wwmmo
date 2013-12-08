import os
import datetime
import webapp2 as webapp

from google.appengine.api import memcache
import model.forum
import ctrl.blog
import handlers
import handlers.admin


class ForumAdminPage(handlers.admin.AdminPage):
  pass


class DashboardPage(ForumAdminPage):
  def get(self):
    self.render("admin/forum/dashboard.html", {})


class ForumListPage(ForumAdminPage):
  def get(self):
    forums = []
    for forum in model.forum.Forum.all():
      forums.append(forum)

    self.render("admin/forum/forum_list.html", {"forums": forums})


class ForumEditPage(ForumAdminPage):
  def get(self, id=None):
    if id:
      forum = model.forum.Forum.get_by_id(int(id))
    else:
      forum = None

    self.render("admin/forum/forum_edit.html", {"forum": forum})

  def post(self, id=None):
    if id:
      forum = model.forum.Forum.get_by_id(int(id))
    else:
      forum = model.forum.Forum()

    forum.name = self.request.POST.get("forum-name")
    forum.slug = self.request.POST.get("forum-slug")
    forum.description = self.request.POST.get("forum-desc")
    forum.put()
    memcache.delete("forums")

    self.redirect("/admin/forum/forums/edit/"+str(forum.key().id_or_name()))


app = webapp.WSGIApplication([("/admin/forum/?", DashboardPage),
                              ("/admin/forum/forums/?", ForumListPage),
                              ("/admin/forum/forums/edit", ForumEditPage),
                              ("/admin/forum/forums/edit/(.*)", ForumEditPage)],
                             debug=os.environ['SERVER_SOFTWARE'].startswith('Development'))
