'''
Created on 11/02/2012

@author: dean@codeka.com.au
'''

import jinja2, os
import webapp2 as webapp

import model

from google.appengine.api import users

jinja = jinja2.Environment(loader=jinja2.FileSystemLoader(os.path.dirname(__file__)+'/tmpl'))


class AdminPage(webapp.RequestHandler):
    '''This is the base class for pages in the admin section.
    '''

    def render(self, tmplName, args):
        args['logout_url'] = users.create_logout_url('/')
        args['logged_in_user'] = self.user.email()

        tmpl = jinja.get_template(tmplName)
        self.response.out.write(tmpl.render(args))

    def dispatch(self):
        '''Checks that a user is logged in and such before we process the request.
        '''
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

        super(AdminPage, self).dispatch()

class DashboardPage(AdminPage):
    '''The "dashboard" page, basically what you get when you visit /admin.
    '''

    def get(self):
        self.render('admin/index.html', {})


class MotdPage(AdminPage):
    '''The "motd" page lets you view/edit the message-of-the-day.
    '''

    def get(self):
        motd = model.MessageOfTheDay.get()
        self.render('admin/motd.html', {'motd': motd})

    def post(self):
        model.MessageOfTheDay.save(self.request.get('new-motd'))

        # rediret back to ourselves...
        self.redirect(self.request.url)


class DebugStarfieldPage(AdminPage):
    def get(self):
        self.render('admin/debug/starfield.html', {})


class DevicesPage(AdminPage):
    ''' The "devices" page lets you view all devices that have registered and send them messages.
    '''

    def get(self):
        self.render('admin/devices.html', {})

app = webapp.WSGIApplication([('/admin', DashboardPage),
                              ('/admin/motd', MotdPage),
                              ('/admin/devices', DevicesPage),
                              ('/admin/debug/starfield', DebugStarfieldPage)],
                             debug=True)
