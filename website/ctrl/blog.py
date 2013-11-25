

import datetime
import re
import logging

from google.appengine.api import memcache

import ctrl
import model.blog


def getPosts(pageNo=0, pageSize=5, includeUnpublished=False):
  keyname = 'posts:%d:%d:%d' % (pageNo, pageSize, int(includeUnpublished))
  posts = memcache.get(keyname)
  if not posts:
    query = model.blog.Post.all()
    if not includeUnpublished:
      query = query.filter("isPublished", True)
    query = query.order('-posted')

    if pageNo == 0:
      it = query.run(limit=pageSize)
    else:
      cursor = ctrl.findCursor(query, "blog-posts:%d" % (int(includeUnpublished)), pageNo, pageSize)
      it = query.with_cursor(cursor)

    posts = []
    for post in it:
      posts.append(post)
      if len(posts) >= pageSize:
        break

    # also, save the cursor for the next page, since it's likely that the user will want to navigate to the
    # next page anyway
    memcache.set('post-page-cursor:%d:%d:%d' % (pageNo+1, pageSize, int(includeUnpublished)), query.cursor())

    memcache.set(keyname, posts)

  return posts


def getPost(postID):
  keyname = 'post:%d' % postID
  post = memcache.get(keyname)
  if not post:
    post = model.blog.Post.get_by_id(postID)
    memcache.set(keyname, post)

  return post


def getPostBySlug(year, month, slug):
  keyname = 'post-by-slug:%d:%d:%s' % (year, month, slug)
  post = memcache.get(keyname)
  if not post:
    dt = datetime.datetime(year, month, 1)
    for post in model.blog.Post.all().filter('slug', slug):
      postdt = datetime.datetime(post.posted.year, post.posted.month, 1)
      if postdt == dt:
        break
    memcache.set(keyname, post)
  return post


def _makeSlug(tagName):
  slug = tagName.lower().replace(' ', '-')
  slug = re.sub(r'[^a-zA-Z0-9_-]+', '', slug)
  return slug


def savePost(post):
  for tagName in post.tags:
    tag = model.blog.Tag.get_by_key_name(_makeSlug(tagName))
    if not tag:
      tag = model.blog.Tag(key_name = _makeSlug(tagName))
      tag.name = tagName
      tag.postCount = 1
      tag.put()
    else:
      pass

  if not post.slug:
    post.slug = _makeSlug(post.title)

  post.put()
  memcache.flush_all()


def deletePost(postID):
  post = getPost(postID)

  post.delete()
  memcache.flush_all()
