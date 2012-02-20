'''
Created on 18/02/2012

@author: dean@codeka.com.au
'''

from google.appengine.ext import db
import logging
import random

import namegen


class Sector(db.Model):
    x = db.IntegerProperty()
    y = db.IntegerProperty()

class Star(db.Model):
    name = db.StringProperty()
    colour = db.IntegerProperty()
    size = db.IntegerProperty()
    x = db.IntegerProperty()
    y = db.IntegerProperty()


class Planet(db.Model):
    pass


class SectorManager:
    """ 'Manages' the sectors, stars, planets, etc. Lets you fetch them, update them, etc

    The most important job of the SectorManager is to maintain consistency of the database. It also
    helps with caching and building new sectors when they come into view for the first time.
    """
    @staticmethod
    def getSector(x, y):
        """ Gets the sector at the given (x,y) coordinates.
        """
        sectors = SectorManager.getSectors(x, y, x+1, y+1)
        return sectors[SectorManager._getSectorKey(x, y)]

    @staticmethod
    def getSectors(x1, y1, x2, y2):
        """ Gets all of the sectors in the rectangle defined by (x1, y1)(x2,y2)

        Sectors are return inclusive of (x1,y1) and exclusive of (x2,y2).
        """

        sectors = {}
        for x in range(x1, x2):
            # Because of limitation in App Engine's filters, we can't search for all sectors
            # where x between x1,x2 AND y between y1,y2 in the same query. So we need to run
            # multiple queries like this....
            query = Sector.all()
            query = query.filter("y >=", y1).filter("y <", y2)
            query = query.filter("x =", x)
            for sector in query:
                sectors[SectorManager._getSectorKey(sector.x, sector.y)] = sector

        for key in sectors:
            sector = sectors[key]
            # fetch all of the sector's stars as well
            query = Star.all().ancestor(sector.key())
            sector.stars = []
            for star in query:
                sector.stars.append(star)

        # now for any sectors which they asked for but which weren't in the data store
        # already, we'll need to generate them from scratch...
        for y in range(y1, y2):
            for x in range(x1, x2):
                key = SectorManager._getSectorKey(x, y)
                if key not in sectors:
                    generator = SectorGenerator(x, y)
                    sectors[key] = generator.generate()

        return sectors

    @staticmethod
    def _getSectorKey(x, y):
        return str(x)+","+str(y)

class SectorGenerator:
    """ This class generates a brand new sector. We give it some stars, planets and whatnot.
    """

    def __init__(self, x, y):
        self.x = x
        self.y = y

    def generate(self):
        """ Generates a new sector at our (x,y) corrdinate
        
        TODO: currently this could result in two sectors being generated at the same (x,y)
        coordinate. We need to do something to avoid that (e.g. specify the key ourselves,
        transactions, something like that).
        """
        logging.info("Generating new sector ("+str(self.x)+","+str(self.y)+")...")
        sector = Sector()
        sector.x = self.x
        sector.y = self.y
        sector.put()

        sector.stars = []

        SECTOR_SIZE = 1024
        GRID_SIZE = SECTOR_SIZE / 16
        GRID_CENTRE = GRID_SIZE / 2

        numStars = random.randint(10, 15)
        for _ in range(numStars):
            star = Star(parent=sector)

            # Make sure there's no other stars with the same coordinates as us. For the initial
            # generation, we generate stars on a 16x16 grid. Then we scale that to fit the whole
            # sector later. This way, stars are created on a semi-regular grid and we can be sure
            # no two stars are ever "too close" to each other.
            dupe = True
            while dupe:
                star.x = random.randint(0, 15)
                star.y = random.randint(0, 15)
                dupe = False

                for otherStar in sector.stars:
                    if otherStar.x == star.x and otherStar.y == star.y:
                        dupe = True

            star.name = namegen.generate(1)[0]
            star.size = random.randint(GRID_SIZE/4, GRID_SIZE/3)

            r = random.randint(100, 255)
            g = random.randint(100, 255)
            b = random.randint(100, 255)
            star.colour = 0xff000000 | (r << 16) | (g << 8) | b

            sector.stars.append(star)

        for star in sector.stars:
            offsetX = random.randint(GRID_CENTRE-(GRID_CENTRE/2.0), GRID_CENTRE+(GRID_CENTRE/2.0))
            offsetY = random.randint(GRID_CENTRE-(GRID_CENTRE/2.0), GRID_CENTRE+(GRID_CENTRE/2.0))
            
            star.x = star.x*GRID_SIZE + offsetX
            star.y = star.y*GRID_SIZE + offsetY
            star.put()

        return sector
