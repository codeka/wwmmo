package au.com.codeka.warworlds.world;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import au.com.codeka.warworlds.server.data.StarData;
import au.com.codeka.warworlds.server.data.StarfieldSectorData;
import au.com.codeka.warworlds.shared.constants.SectorConstants;
import au.com.codeka.warworlds.shared.util.CoolRandom;
import au.com.codeka.warworlds.shared.util.NameGenerator;

/**
 * Generates a whole new sector the first time someone vists it.
 * 
 * \param sector The StarfieldSectorData we're going to populate. We assume the
 * \c getSectorX() and \c getSectorY() will return valid data.
 */
public class SectorGenerator {
    private static ArrayList<NameGenerator> sNameGenerators;

    static {
        sNameGenerators = new ArrayList<NameGenerator>();
        File parentDirectory = new File("./data/vocabulary");
        for (String name : parentDirectory.list()) {
            try {
                sNameGenerators.add(new NameGenerator(new FileInputStream("./data/vocabulary/"+name)));
            } catch (FileNotFoundException e) {
                // shouldn't happen, we just checked...
            } catch (IOException e) {
                // ignore...
            }
        }
    }

    public static void generate(StarfieldSectorData sector) {
        CoolRandom r = new CoolRandom(sector.getSectorX(), sector.getSectorY(), new Random().nextInt());
        int numStars = r.nextInt(SectorConstants.MinStars, SectorConstants.MaxStars);

        final int gridX = (SectorConstants.Width / 16);
        final int gridY = (SectorConstants.Height / 16);
        final int gridCentreX = gridX / 2;
        final int gridCentreY = gridY / 2;

        for (int i = 0; i < numStars; i++) {
            int starX = r.nextInt(0, 16);
            int starY = r.nextInt(0, 16);

            // check that no other star has the same coordinates...
            boolean dupe = false;
            do {
                dupe = false;
                for(StarData existingStar : sector.getStars()) {
                    if ((existingStar.getX() / gridX) == starX &&
                            (existingStar.getY() / gridY) == starY) {
                        dupe = true;
                        starX = r.nextInt(0, 16);
                        starY = r.nextInt(0, 16);
                        break;
                    }
                }
            } while (dupe);

            final int offsetX = r.nextInt(gridCentreX - (gridCentreX / 2), gridCentreX + (gridCentreX / 2));
            final int offsetY = r.nextInt(gridCentreY - (gridCentreY / 2), gridCentreY + (gridCentreY / 2));

            final int nameGeneratorIndex = r.nextInt(sNameGenerators.size());
            final String name = sNameGenerators.get(nameGeneratorIndex).compose(r, r.nextInt(2, 6));

            final int red = r.nextInt(100, 255);
            final int green = r.nextInt(100, 255);
            final int blue = r.nextInt(100, 255);
            final int colour = 0xff000000 | (red << 16) | (green << 8) | blue;

            final int size = r.nextInt(gridX / 4, gridX / 3);

            final StarData newStar = new StarData(name, starX*gridX + offsetX,
                    starY*gridY + offsetY, colour, size);
            sector.getStars().add(newStar);
        }
    }
}
