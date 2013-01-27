"""sector.py: Sector/star/planet related tasks."""

from ctrl import sectorgen
import os
import webapp2 as webapp

import tasks


class GeneratePage(tasks.TaskPage):
  """Simple page that just delegates to SectorGenerator to do the work."""

  def get(self, sectorX, sectorY):
    sectorX = int(sectorX)
    sectorY = int(sectorY)
    sectorgen.generateSector(sectorX, sectorY)


class ExpandUniversePage(tasks.TaskPage):
  """This is a cron job that is called once per day to "expand" the universe.

  We look at all the sectors which currently have at least one colonized star, then make sure
  the universe has all sectors around the centre generated. We expand the range of the universe
  until we've generated 50 sectors -- that's enough for one day."""

  def get(self):
    sectorgen.expandUniverse()


app = webapp.WSGIApplication([("/tasks/sector/generate/([0-9-]+),([0-9-]+)", GeneratePage),
                              ("/tasks/sector/expand-universe", ExpandUniversePage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))

