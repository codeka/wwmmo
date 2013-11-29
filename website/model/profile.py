
from google.appengine.ext import db


class Profile(db.Model):
  """A user's profile information."""
  empire_id = db.IntegerProperty()
  realm_name = db.StringProperty()
  display_name = db.StringProperty()

  @staticmethod
  def GetProfile(user_id):
    profile_key = db.Key.from_path(Profile.__name__, user_id)
    return Profile.get(profile_key)

  @staticmethod
  def SaveProfile(user_id, realm_name, empire_id, display_name):
    profile = Profile.GetProfile(user_id)
    if not profile:
      profile = Profile(key=db.Key.from_path(Profile.__name__, user_id),
                        realm_name=realm_name, empire_id=empire_id, display_name=display_name)
    else:
      profile.realm_name = realm_name
      profile.empire_id = empire_id
      profile.display_name = display_name
    profile.put()
