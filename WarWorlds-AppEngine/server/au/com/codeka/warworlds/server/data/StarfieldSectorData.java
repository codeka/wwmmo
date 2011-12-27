package au.com.codeka.warworlds.server.data;

import java.io.Serializable;
import java.util.Random;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import au.com.codeka.warworlds.shared.StarfieldNode;
import au.com.codeka.warworlds.shared.StarfieldSector;
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

    @NotPersistent // don't persist the nodes with the sector, we'll fetch them manually
    private StarfieldNodeData[] mNodes;

    public StarfieldSectorData() {
        mNodes = new StarfieldNodeData[256];
    }

    public void setNode(int nodeX, int nodeY, StarfieldNodeData node) {
        mNodes[nodeY*16+nodeX] = node;
    }

    public StarfieldNodeData getNode(int nodeX, int nodeY) {
        if (nodeX >= 16 || nodeX < 0) {
            throw new IllegalArgumentException("Invalid nodeX "+nodeX);
        }
        if (nodeY >= 16 || nodeY < 0) {
            throw new IllegalArgumentException("Invalid nodeY "+nodeY);
        }

        return mNodes[nodeY*16+nodeX];
    }

    public Key getKey() {
        return mKey;
    }

    public long getSectorX() {
        return mSectorX;
    }

    public long getSectorY() {
        return mSectorY;
    }

    public StarfieldSector toStarfieldSector() {
        StarfieldNode[] nodes = new StarfieldNode[256];
        for(int i = 0; i < 256; i++) {
            if (mNodes[i] == null) {
                nodes[i] = new StarfieldNode(); //??
            } else {
                nodes[i] = mNodes[i].toStarfieldNode();
            }
        }

        return new StarfieldSector(mSectorX, mSectorY, nodes);
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

        sector.mNodes = new StarfieldNodeData[256];
        for(int nodeY = 0; nodeY < 16; nodeY++) {
            for(int nodeX = 0; nodeX < 16; nodeX++) {
                Key nodeKey = buildKey(sector.mKey, nodeX, nodeY);
                StarfieldNodeData node = pm.getObjectById(StarfieldNodeData.class, nodeKey);

                Key starKey = KeyFactory.createKey(nodeKey, 
                        StarfieldStarData.class.getSimpleName(), 1);
                try {
                    node.setStar(pm.getObjectById(StarfieldStarData.class, starKey));
                } catch (JDOObjectNotFoundException e) {
                    // it won't exist if there is no star in this node...
                }

                sector.setNode(nodeX, nodeY, node);
            }
        }

        return sector;
    }

    public static StarfieldSectorData generate(long sectorX, long sectorY) {
        StarfieldSectorData sector = new StarfieldSectorData();
        sector.mKey = buildKey(sectorX, sectorY);
        sector.mSectorX = sectorX;
        sector.mSectorY = sectorY;

        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            pm.makePersistent(sector);

            CoolRandom r = new CoolRandom(sectorX, sectorY, new Random().nextInt());
            for(int nodeY = 0; nodeY < 16; nodeY++) {
                for(int nodeX = 0; nodeX < 16; nodeX++) {
                    Key nodeKey = buildKey(sector.mKey, nodeX, nodeY);

                    StarfieldStarData star = null;
                    if (r.nextBoolean(0.08f)) {
                        Key starKey = KeyFactory.createKey(nodeKey, 
                                StarfieldStarData.class.getSimpleName(), 1);

                        int red = r.nextInt(100, 255);
                        int green = r.nextInt(100, 255);
                        int blue = r.nextInt(100, 255);
                        int colour = 0xff000000 | (red << 16) | (green << 8) | blue;

                        star = new StarfieldStarData(starKey,
                                r.nextInt(-12, 12), r.nextInt(-12, 12),
                                colour, r.nextInt(8, 12));
                        pm.makePersistent(star);
                    }

                    StarfieldNodeData node = new StarfieldNodeData(nodeKey, nodeX, nodeY, star);
                    sector.setNode(nodeX, nodeY, node);
                }
            }

            pm.makePersistentAll(sector.mNodes);
        } finally {
            pm.close();
        }

        return sector;
    }

    private static Key buildKey(long sectorX, long sectorY) {
        return KeyFactory.createKey(StarfieldSectorData.class.getSimpleName(),
                sectorX + ":" + sectorY);
    }

    private static Key buildKey(Key parentKey, int nodeX, int nodeY) {
        return KeyFactory.createKey(parentKey, StarfieldNodeData.class.getSimpleName(),
                nodeX + ":" + nodeY);
    }
}
