"""chat.py: Module that handles the chat aspect of the game."""

import base64
import logging

from model import gcm as gcm_mdl
import model


def postMessage(user, msg_pb):
  msg_model = model.ChatMessage()
  msg_model.user = user
  msg_model.message = msg_pb.message
  msg_model.put()

  # send a message directly to all currently-online players
  registration_ids = []
  for online_device_mdl in model.OnlineDevice.all():
    device_mdl = online_device_mdl.device
    registration_ids.append(device_mdl.deviceRegistrationID)

  try:
    gcm = gcm_mdl.GCM('AIzaSyADWOC-tWUbzj-SVW13Sz5UuUiGfcmHHDA')
    gcm.json_request(registration_ids=registration_ids,
                       data={"chat": base64.b64encode(msg_pb.SerializeToString())})
  except:
    logging.warn("An error occurred sending notification, notification not sent")

