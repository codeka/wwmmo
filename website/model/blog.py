import datetime
from flask import current_app
import os
import yaml


class Post(yaml.YAMLObject):
  """An actual blog post."""
  yaml_loader = yaml.SafeLoader

  def __init__(self, html = "", title = "", posted = datetime.datetime.now(),
               updated = datetime.datetime.now(), slug = "", is_published = False):
    self.html = html
    self.title = title
    self.posted = posted
    self.updated = updated
    self.slug = slug
    self.is_published = is_published


def _rootPath():
  #return os.path.join(current_app.config['DATA_PATH'], 'posts')
  return '/home/dean/src/wwmmo/website-data/posts'


def listPosts():
  """Gets a list of all the post files."""
  return sorted([
    os.path.join(dir_path, f)
      for dir_path, dir_name, filenames in os.walk(_rootPath())
      for f in filenames
      if os.path.splitext(f)[1] == '.yaml'], reverse=True)


def findPostBySlug(year, month, slug):
  """Searches for a post with the given year, month and slug. Returns the filename, or None."""
  dir_path = os.path.join(_rootPath(), '%04d/%02d' % (year, month))
  if not os.path.isdir(dir_path):
    return None
  for f in os.listdir(dir_path):
    if f.endswith(slug + '.yaml'):
      return os.path.join(dir_path, f)
  return None


def loadPost(filename):
  """Loads a Post from the file with the given name."""
  try:
    return yaml.load(open(filename, 'r'), Loader=yaml.Loader)
  except:
    # Any errors, we'll just return none. Probably file not found?
    return None


def savePost(post):
  dir_path = os.path.join(_rootPath(), '%04d/%02d' % (post.posted.year, post.posted.month))
  if not os.path.isdir(dir_path):
    return False
  filename = os.path.join(dir_path, post.slug + '.yaml')
  with open(filename, 'w') as f:
    return yaml.dump(post, f)
