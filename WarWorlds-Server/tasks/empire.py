'''
Created on 14/04/2012

@author: dean@codeka.com.au
'''

import webapp2 as webapp
import tasks
from model import empire as mdl
from datetime import datetime, timedelta
import ctrl
from ctrl import empire as ctl
from google.appengine.ext import db
import logging


class BuildCheckPage(tasks.TaskPage):
  def get(self):
    '''This page is called when a build operation is scheduled to finish.

    We need to confirm that the build is actually complete, set up the building or ship with the
    empire that built it, and then reschedule ourselves for the next build.
    '''
    def _tx(build_key):
      build = mdl.BuildOperation.get(build_key)
      if not build:
        return None

      # OK, this build operation is complete (or will complete in the next 10 seconds -- close
      # enough) so we need to make sure the building/ship itself is added to the empire.
      colony_key = mdl.BuildOperation.colony.get_value_for_datastore(build)
      empire_key = mdl.BuildOperation.empire.get_value_for_datastore(build)
      star_key = mdl.BuildOperation.star.get_value_for_datastore(build)
      logging.info("Build for empire \"%s\" complete." % (empire_key))
      building_model = mdl.Building()
      building_model.colony = colony_key
      building_model.empire = empire_key
      building_model.star = star_key
      building_model.designName = build.designName
      building_model.buildTime = datetime.now()

      # and now we're done with this operation
      build.delete()
      return building_model

    # Fetch the keys outside of the transaction, cause we can't do that in a TX
    buildings = []
    query = mdl.BuildOperation.all().filter("endTime <", datetime.now() + timedelta(seconds=10))
    for build in query:
      building_model = db.run_in_transaction(_tx, build.key())
      if building_model:
        buildings.append(building_model)

    keys_to_clear = []
    for building_model in buildings:
      building_model.put()

      # clear the cached items that reference this building
      keys_to_clear.append('star:%s' % (building_model.star.key()))
      keys_to_clear.append('colonies:for-empire:%s' % (build.empire.key()))

    ctrl.clearCached(keys_to_clear)
    ctl.scheduleBuildCheck()

    self.response.write("Success!")

app = webapp.WSGIApplication([('/tasks/empire/build-check', BuildCheckPage)],
                             debug=False)

