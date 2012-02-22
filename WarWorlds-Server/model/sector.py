'''
Created on 18/02/2012

@author: dean@codeka.com.au
'''

from google.appengine.ext import db
import logging
import random

import namegen


class PlanetType:
    def __init__(self, name="", minSize=5, maxSize=50):
        self.name = name
        self.minSize = minSize
        self.maxSize = maxSize

# This is the description of different types of planets. The order is important and
# must match what is defined for Planet.PLANET_TYPE in warworlds.proto
_planetTypes = [
    PlanetType(name="Gas Giant",
               minSize=40, maxSize=50),
    PlanetType(name="Radiated",
               minSize=5, maxSize=30),
    PlanetType(name="Inferno",
               minSize=5, maxSize=30),
    PlanetType(name="Asteroids",
               minSize=50, maxSize=50),
    PlanetType(name="Water",
               minSize=20, maxSize=40),
    PlanetType(name="Toxic",
               minSize=10, maxSize=20),
    PlanetType(name="Desert",
               minSize=10, maxSize=30),
    PlanetType(name="Swamp",
               minSize=10, maxSize=30),
    PlanetType(name="Terran",
               minSize=10, maxSize=30)
    ]


class Sector(db.Model):
    x = db.IntegerProperty()
    y = db.IntegerProperty()


class Star(db.Model):
    starID = db.IntegerProperty()
    name = db.StringProperty()
    colour = db.IntegerProperty()
    size = db.IntegerProperty()
    x = db.IntegerProperty()
    y = db.IntegerProperty()
    planets = None


class Planet(db.Model):
    index = db.IntegerProperty()
    planetTypeID = db.IntegerProperty(name="planetType")
    planetType = None # Will be filled in with a PlanetType instance
    size = db.IntegerProperty()


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
    def getStar(sectorX, sectorY, starID):
        """Gets all of the details about a single star, including planets colonies and so on.

        We don't worry about whether the sector has been generated. If not, we just return None.
        After all, you shouldn't be asking for a star in a sector you haven't visited yet.
        """
        sectorQuery = Sector.all().filter("x", sectorX).filter("y", sectorY)
        for sector in sectorQuery:
            logging.info("Got sector: "+str(sector.key()))
            starQuery = Star.all().ancestor(sector.key()).filter("starID", starID)
            for star in starQuery:
                planetQuery = Planet.all().ancestor(star.key())
                star.planets = []
                for planet in planetQuery:
                    star.planets.append(planet)
            return star

        return None

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
        for i in range(numStars):
            star = Star(parent=sector)
            star.starID = i+1

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

            numPlanets = random.randint(2, 5)
            for index in range(numPlanets):
                planet = Planet(parent=star)
                planet.index = (index+1)
                planet.planetTypeID = random.randint(0, len(_planetTypes) - 1)
                planet.planetType = _planetTypes[planet.planetTypeID]
                planet.size = random.randint(planet.planetType.minSize, planet.planetType.maxSize)
                planet.put()

        return sector
