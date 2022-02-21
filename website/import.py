from datetime import datetime
import os
from model import blog
import yaml

import sys
sys.path.append('/usr/lib/google-cloud-sdk/platform/google_appengine')

from google.appengine.datastore import entity_pb
from google.appengine.api.files import records
from google.appengine.ext import ndb


class TestModel(ndb.Model):  # we need the definition of the model we want to read
    foobar = ndb.StringProperty(indexed=False)


typeCounts = {}

base_dir = "/home/dean/src/wwmmo/website-data/posts"


for filename in sys.argv[1:]:
  with open(filename, 'rb') as f:
    for r in records.RecordsReader(f):
      entity = entity_pb.EntityProto(r)
      type = entity.key().path().element(0).type()
      typeCounts[type] = typeCounts.get(type, 0) + 1
      if type == 'Post':
        #print(entity)
        post = blog.Post()
        for prop in entity.property_list():
          if prop.name() == "isPublished":
            if not prop.value().booleanvalue():
              continue
          elif prop.name() == "posted":
            post.posted = datetime.fromtimestamp(prop.value().int64value() / 1000000)
          elif prop.name() == "slug":
            post.slug = prop.value().stringvalue()
          elif prop.name() == "title":
            post.title = prop.value().stringvalue()
          elif prop.name() == "updated":
            post.updated = datetime.fromtimestamp(prop.value().int64value() / 1000000)
        for prop in entity.raw_property_list():
          if prop.name() == "html":
            post.html = prop.value().stringvalue()
        filename = "%s/%04d/%02d/%s.yaml" % (base_dir, post.posted.year, post.posted.month, post.slug)
        if not os.path.isdir(os.path.dirname(filename)):
          os.makedirs(os.path.dirname(filename))
        with open(filename, "w") as f:
          yaml.dump(post, f)


for k, v in typeCounts.items():
  print(k + " = " + str(v))

