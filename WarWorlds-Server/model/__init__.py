'''
Created on 11/02/2012

@author: dean@codeka.com.au
'''

from google.appengine.ext import db


class MessageOfTheDay(db.Model):
    '''Message of the day is displayed to users when they first connect.
    '''
    message = db.TextProperty()
    date = db.DateTimeProperty(auto_now=True)

    @staticmethod
    def _getKey():
        return db.Key.from_path('MessageOfTheDay', 'default')

    @staticmethod
    def get():
        '''Gets the current message of the day (there's only ever one)
        '''
        return db.get(MessageOfTheDay._getKey())

    @staticmethod
    def save(msg):
        '''Saves the given message as the new message of the day.
        '''
        motd = MessageOfTheDay.get()
        if not motd:
            motd = MessageOfTheDay(key=MessageOfTheDay._getKey())
        motd.message = msg
        motd.put()
