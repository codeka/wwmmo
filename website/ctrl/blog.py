import datetime
import logging
import model.blog


def getPosts(pageNo=0, pageSize=5, includeUnpublished=False):
  post_files = model.blog.listPosts()
  posts = []
  for filename in post_files[pageNo * pageSize : (pageNo + 1) * pageSize]:
    posts.append(model.blog.loadPost(filename))
  return posts


def getPostBySlug(year, month, slug):
  filename = model.blog.findPostBySlug(year, month, slug)
  if not filename:
    return None
  return model.blog.loadPost(filename)
  