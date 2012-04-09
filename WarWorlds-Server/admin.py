'''
Created on 11/02/2012

@author: dean@codeka.com.au
'''

import jinja2, os
import webapp2 as webapp

import model
from model import session
from model import empire

from mapreduce import control
from google.appengine.api import channel
from google.appengine.api import users
from google.appengine.ext import db
from google.appengine.ext.db.metadata import Kind

import logging

jinja = jinja2.Environment(loader=jinja2.FileSystemLoader(os.path.dirname(__file__)+'/tmpl'))


class AdminPage(webapp.RequestHandler):
  '''This is the base class for pages in the admin section.'''

  def render(self, tmplName, args):
    args['logout_url'] = users.create_logout_url('/')
    args['logged_in_user'] = self.user.email()
    args['msgs'] = self.session.getValue('msgs', [])
    if args['msgs'] != []:
      self.session.setValue('msgs', [])

    tmpl = jinja.get_template(tmplName)
    self.response.out.write(tmpl.render(args))

  def dispatch(self):
    '''Checks that a user is logged in and such before we process the request.'''
    self.user = users.get_current_user()
    if not self.user:
      # not logged in, so redirect to the login page
      self.redirect(users.create_login_url(self.request.uri))
      return

    # TODO: better handling of authorization... one email address ain't enough!
    if self.user.email() != 'dean@codeka.com.au':
      # not authorized to view the backend, redirect to the home page instead
      self.redirect('/')
      return

    # load up the current session
    self.session = session.Session.attach(self)

    super(AdminPage, self).dispatch()

    # save the current session
    self.session.detach()

  def addMessage(self, msg):
    '''Adds a message to be displayed the next time a page is rendered.'''
    msgs = self.session.getValue('msgs', [])
    msgs.append(msg)
    self.session.setValue('msgs', msgs)

class DashboardPage(AdminPage):
  '''The "dashboard" page, basically what you get when you visit /admin.'''

  def get(self):
    data = {}
    self.render('admin/index.html', data)


class ChatPage(AdminPage):
  '''The chat page lets us chat with all players, make real-time announcements and whatnot.'''

  def get(self):
    # if there's already a ChatClient for us, just reuse it
    chatClient = None

    clientID = 'server:'+self.user.user_id()
    for cc in model.ChatClient.all().filter("clientID", clientID):
      chatClient = cc
    if chatClient == None:
      chatClient = model.ChatClient()
      chatClient.user = self.user
      chatClient.clientID = clientID
      chatClient.put()

    token = channel.create_channel(clientID)
    self.render('admin/chat.html', {'token': token})


class EmpiresPage(AdminPage):
  '''The 'empires' page lets you view, query, update, delete (etc) empires.'''
  def get(self):
    self.render('admin/empires.html', {})


class MotdPage(AdminPage):
  '''The "motd" page lets you view/edit the message-of-the-day.'''

  def get(self):
    motd = model.MessageOfTheDay.get()
    self.render('admin/motd.html', {'motd': motd})

  def post(self):
    model.MessageOfTheDay.save(self.request.get('new-motd'))

    # redirect back to ourselves...
    self.redirect(self.request.url)


class DebugStarfieldPage(AdminPage):
  def get(self):
    self.render('admin/debug/starfield.html', {})


class DebugDataStorePage(AdminPage):
  def get(self):
    kinds = []
    q = Kind.all()
    for p in q:
      kinds.append(p.kind_name)
    self.render('admin/debug/data-store.html', {'kinds': kinds})

  def post(self):
    if self.request.get('action') == 'bulk-delete':
      entityKind = self.request.get('entity-kind')
      logging.info('bulk-deleting kind: '+entityKind)

      control.start_map(name='Bulk Delete',
                        handler_spec='tasks.datastore.bulkdelete',
                        reader_spec='mapreduce.input_readers.DatastoreEntityInputReader',
                        mapper_parameters={'entity_kind': entityKind})

      # redirect back to ourselves TODO: butter bar message
      self.addMessage('Entity "'+entityKind+'" has been bulk-deleted.')
      self.redirect(self.request.url)
    else:
      self.response.set_status(400)

class DevicesPage(AdminPage):
  ''' The "devices" page lets you view all devices that have registered and send them messages.'''

  def get(self):
    self.render('admin/devices.html', {})

app = webapp.WSGIApplication([('/admin', DashboardPage),
                              ('/admin/empires', EmpiresPage),
                              ('/admin/chat', ChatPage),
                              ('/admin/motd', MotdPage),
                              ('/admin/devices', DevicesPage),
                              ('/admin/debug/starfield', DebugStarfieldPage),
                              ('/admin/debug/data-store', DebugDataStorePage)],
                             debug=True)
