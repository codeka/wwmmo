package au.com.codeka.warworlds.server.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.Transaction;

import au.com.codeka.warworlds.shared.StarfieldSector;
import au.com.codeka.warworlds.shared.StarfieldStar;
import au.com.codeka.warworlds.shared.util.CoolRandom;

import com.google.android.c2dm.server.PMF;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class StarfieldSectorData implements Serializable {
    private static final long serialVersionUID = 1L;

    @PrimaryKey
    @Persistent
    private Key mKey;

    @Persistent
    private long mSectorX;

    @Persistent
    private long mSectorY;

    @Persistent
    @Element(dependent = "true")
    private Set<StarData> mStars;

    public StarfieldSectorData() {
        mStars = new HashSet<StarData>();
    }

    public Key getKey() {
        return mKey;
    }

    public Set<StarData> getStars() {
        return mStars;
    }

    public long getSectorX() {
        return mSectorX;
    }

    public long getSectorY() {
        return mSectorY;
    }

    public StarfieldSector toStarfieldSector() {
        StarfieldStar[] stars = new StarfieldStar[mStars.size()];
        int i = 0;
        for(StarData star : mStars) {
            stars[i] = star.toStarfieldStar();
            i++;
        }

        return new StarfieldSector(mSectorX, mSectorY, stars);
    }

    public static StarfieldSectorData getSector(long sectorX, long sectorY) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            return getSector(pm, sectorX, sectorY);
        } finally {
            pm.close();
        }
    }

    public static StarfieldSectorData getSector(PersistenceManager pm, long sectorX, long sectorY) {
        Key key = buildKey(sectorX, sectorY);
        StarfieldSectorData sector = pm.getObjectById(StarfieldSectorData.class, key);
        // TODO: is that all?
        return sector;
    }

    public static StarfieldSectorData generate(long sectorX, long sectorY) {
        StarfieldSectorData sector = new StarfieldSectorData();
        sector.mKey = buildKey(sectorX, sectorY);
        sector.mSectorX = sectorX;
        sector.mSectorY = sectorY;

        PersistenceManager pm = PMF.get().getPersistenceManager();
        Transaction tx = null;
        try {
            tx = pm.currentTransaction();
            tx.begin();

            CoolRandom r = new CoolRandom(sectorX, sectorY, new Random().nextInt());
            int numStars = r.nextInt(10, 15);

            for (int i = 0; i < numStars; i++) {
                int starX = r.nextInt(0, 16);
                int starY = r.nextInt(0, 16);

                // check that no other star has the same coordinates...
                boolean dupe = false;
                do {
                    dupe = false;
                    for(StarData existingStar : sector.mStars) {
                        if ((existingStar.getX() / 16) == starX &&
                                (existingStar.getX() / 16) == starY) {
                            dupe = true;
                            starX = r.nextInt(0, 16);
                            starY = r.nextInt(0, 16);
                            break;
                        }
                    }
                } while (dupe);

                final int offsetX = r.nextInt(4, 12);
                final int offsetY = r.nextInt(4, 12);

                final int red = r.nextInt(100, 255);
                final int green = r.nextInt(100, 255);
                final int blue = r.nextInt(100, 255);
                final int colour = 0xff000000 | (red << 16) | (green << 8) | blue;

                final int size = r.nextInt(8, 12);

                final StarData newStar = new StarData(starX*16 + offsetX,
                        starY*16 + offsetY, colour, size);
                sector.mStars.add(newStar);
            }

            pm.makePersistent(sector);
            tx.commit();
        } finally {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }

            pm.close();
        }

        return sector;
    }

    private static Key buildKey(long sectorX, long sectorY) {
        return KeyFactory.createKey(StarfieldSectorData.class.getSimpleName(),
                sectorX + ":" + sectorY);
    }
}
