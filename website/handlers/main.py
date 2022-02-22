from flask import Response, render_template

from model import blog
from . import handlers


@handlers.route('/')
def index():
  return render_template('index.html')


@handlers.route('/privacy-policy')
def privacy_policy():
  return render_template('privacy-policy.html')


@handlers.route('/terms-of-service')
def terms_of_service():
  return render_template('terms-of-service.html')


@handlers.route('/rules')
def rules():
  return render_template('rules.html')


@handlers.route('/donate-thanks')
def donate_thanks():
  return render_template('donate-thanks.html')


@handlers.route('/forum/annoucements/rss')
def announcements_rss():
  return render_template('announcements_rss.xml')


@handlers.route('/sitemap.xml')
def sitemap():
  pages = []
  pages.append({
      "url": "//www.codeka.com/showcase",
      "lastmod": "2022-02-16",
      "priority": 1,
      "changefreq": "yearly"
    })

  for f in blog.listPosts():
    post = blog.loadPost(f)
    pages.append({
        "url": ("//www.codeka.com/blog/%04d/%02d/%s" %
                    (post.posted.year, post.posted.month, post.slug)),
        "lastmod": "%04d-%02d-%02d" % (post.updated.year, post.updated.month, post.updated.day),
        "priority": 10,
        "changefreq": "monthly"
      })

  return Response(render_template("sitemap.xml", pages=pages), content_type="text/xml")
