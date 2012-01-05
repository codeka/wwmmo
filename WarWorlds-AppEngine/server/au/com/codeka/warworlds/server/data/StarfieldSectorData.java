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
import au.com.codeka.warworlds.shared.constants.SectorConstants;
import au.com.codeka.warworlds.shared.util.CoolRandom;
import au.com.codeka.warworlds.world.SectorGenerator;

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

            SectorGenerator.generate(sector);

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
