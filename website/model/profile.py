
import json

from google.appengine.ext import db


class Profile(db.Model):
  """A user's profile information."""
  empire_id = db.IntegerProperty()
  alliance_id = db.IntegerProperty()
  realm_name = db.StringProperty()
  display_name = db.StringProperty()
  user = db.UserProperty()

  @staticmethod
  def GetProfile(user_id):
    profile_key = db.Key.from_path(Profile.__name__, user_id)
    return Profile.get(profile_key)

  @staticmethod
  def SaveProfile(user, realm_name, display_name, empire):
    profile = Profile.GetProfile(user.user_id())
    if not profile:
      profile = Profile(key=db.Key.from_path(Profile.__name__, user.user_id()))
    profile.realm_name = realm_name
    profile.display_name = display_name
    profile.user = user
    if empire:
      profile.empire_id = int(empire["key"])
      if "alliance" in empire and "key" in empire["alliance"]:
        profile.alliance_id = int(empire["alliance"]["key"])
    profile.put()
    return profile


class Empire(db.Model):
  """Represents details of an empire that we've synced from the server."""
  empire_id = db.IntegerProperty()
  realm_name = db.StringProperty()
  display_name = db.StringProperty()
  user_email = db.StringProperty()
  empire_json = db.TextProperty()
  name_search = db.StringListProperty()

  @staticmethod
  def Save(realm_name, empire):
    empire_key = db.Key.from_path(Empire.__name__, realm_name+":"+empire["key"])
    empire_mdl = Empire.get(empire_key)
    if not empire_mdl:
      empire_mdl = Empire(key=empire_key, realm_name=realm_name, empire_id=int(empire["key"]),
                          display_name=empire["display_name"].strip(), user_email=empire["email"],
                          empire_json=json.dumps(empire))
    else:
      empire_mdl.display_name = empire["display_name"].strip()
      empire_mdl.user_email = empire["email"]
      empire_mdl.empire_json = json.dumps(empire)

    empire_mdl.name_search = []
    for substr in empire["display_name"].split():
      empire_mdl.name_search.append(substr.lower().strip())

    empire_mdl.put()


class EmpireAssociateRequest(db.Model):
  """Represents a request to associate a profile with a empire."""
  empire_id = db.IntegerProperty()
  realm_name = db.StringProperty()
  profile = db.ReferenceProperty(Profile)
  cookie = db.StringProperty()
  request_date = db.DateTimeProperty()


class Alliance(db.Model):
  alliance_id = db.IntegerProperty()
  realm_name = db.StringProperty()
  name = db.StringProperty()
  alliance_json = db.TextProperty()

  @staticmethod
  def Save(realm_name, alliance):
    alliance_key = db.Key.from_path(Alliance.__name__, realm_name+":"+alliance["key"])
    alliance_mdl = Alliance.get(alliance_key)
    if not alliance_mdl:
      alliance_mdl = Alliance(key=alliance_key, realm_name=realm_name, alliance_id=int(alliance["key"]),
                              name=alliance["name"].strip(), alliance_json=json.dumps(alliance))
    else:
      alliance_mdl.name = alliance["name"]
      alliance_mdl.alliance_json = json.dumps(alliance)
    alliance_mdl.put()


  @staticmethod
  def Fetch(realm_name, alliance_id):
    # TODO: cache
    alliance_key = db.Key.from_path(Alliance.__name__, realm_name+":"+str(alliance_id))
    return Alliance.get(alliance_key)
