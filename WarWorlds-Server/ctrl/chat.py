"""chat.py: Module that handles the chat aspect of the game."""

import base64
from datetime import datetime, timedelta
import logging

from google.appengine.ext import db

import ctrl
import model
from model import gcm as gcm_mdl
from model import chat as chat_mdl

from protobufs import messages_pb2 as pb

def postMessage(user, msg_pb):
  msg_model = chat_mdl.ChatMessage()
  msg_model.user = user
  msg_model.message = msg_pb.message
  if msg_pb.empire_key:
    msg_model.empire = db.Key(msg_pb.empire_key)
  msg_model.put()

  one_day_ago = datetime.now() - timedelta(hours=24)

  # send a message directly to all currently-online players
  registration_ids = []
  for online_device_mdl in model.OnlineDevice.all():
    if online_device_mdl.onlineSince >= one_day_ago:
      try:
        device_mdl = online_device_mdl.device
        registration_ids.append(device_mdl.gcmRegistrationID)
      except:
        pass # ignore ones that give an error

  try:
    gcm = gcm_mdl.GCM('AIzaSyADWOC-tWUbzj-SVW13Sz5UuUiGfcmHHDA')
    gcm.json_request(registration_ids=registration_ids,
                       data={"chat": base64.b64encode(msg_pb.SerializeToString())})
  except:
    logging.warn("An error occurred sending notification, notification not sent")


def getLatestChats(since=None, max_chats=None):
  query = chat_mdl.ChatMessage.all()
  if since:
    query.filter("postedDate >", since)
  query.order("-postedDate")
  if max_chats:
    query.fetch(max_chats)

  n = 0
  chat_msgs_pb = pb.ChatMessages()
  for chat_msg_mdl in query:
    chat_msg_pb = chat_msgs_pb.messages.add()
    ctrl.chatMessageModelToPb(chat_msg_pb, chat_msg_mdl)
    if max_chats and n >= max_chats:
      break
  return chat_msgs_pb
