
  
import datetime
from flask import abort, render_template, request, redirect, Response

import ctrl.blog

from . import handlers

@handlers.route('/blog')
def blog_index():
  pageNo = 0
  if request.args.get('page'):
    pageNo = int(request.args.get('page'))
  if pageNo < 0:
    pageNo = 0

  posts = ctrl.blog.getPosts(pageNo)
  if not posts and pageNo > 0:
    redirect('/blog?page=%d' % (pageNo - 1))
  return render_template('blog/index.html', posts=posts, pageNo=pageNo)

@handlers.route('/blog/<year>/<month>/<slug>')
def blog_post(year, month, slug):
  post = ctrl.blog.getPostBySlug(int(year), int(month), slug)
  if not post:
    abort(404)
  return render_template('blog/post.html', post=post)


@handlers.route('/blog/rss')
def blog_rss():
  posts = ctrl.blog.getPosts(0, 15)
  pubDate = datetime.time()
  if posts and len(posts) > 0:
    pubDate = posts[0].posted
  
  return Response(
      render_template('blog/rss.xml', posts=posts,
                      pubDate=pubDate.strftime('%a, %d %b %Y %H:%M:%S GMT')),
      content_type='application/rss+xml')
