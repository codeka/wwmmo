'''
Created on 24/03/2012

@author: dean@codeka.com.au
'''

import pickle
from google.appengine.ext import db


class Session(db.Model):
  user = db.UserProperty()
  lastUpdate = db.DateTimeProperty(auto_now=True)
  data = db.BlobProperty()

  sessionData = {}
  isModified = False

  @staticmethod
  def attach(handler, options=None):
    '''Attaches a session to the given webapp.Handler instance.
    
    This is usually called right at the start of the page so that we can query the cookie to
    see if there's already a session establish, and create a new one if not.'''

    if options is None:
      options = {}
    if 'cookie_name' not in options:
      options['cookie_name'] = 'SESSID'

    sess = None
    if options['cookie_name'] in handler.request.cookies:
      session_id = handler.request.cookies[options['cookie_name']]
      sess = Session.get(session_id)
      if sess is not None:
        #TODO: validate session
        if sess.data is not None:
          sess.sessionData = pickle.loads(sess.data)
        else:
          sess.sessionData = {}

    if sess is None:
      sess = Session()
      sess.put()
      handler.response.set_cookie(options['cookie_name'], str(sess.key()))

    sess.options = options
    return sess

  def detach(self):
    '''Called at the end of the page cycle to save the session data back again.'''
    if self.isModified:
      self.data = pickle.dumps(self.sessionData)
      self.put()

  def getValue(self, key, defValue=None):
    if key in self.sessionData:
      return self.sessionData[key]
    return defValue

  def setValue(self, key, value):
    self.sessionData[key] = value
    self.isModified = True
