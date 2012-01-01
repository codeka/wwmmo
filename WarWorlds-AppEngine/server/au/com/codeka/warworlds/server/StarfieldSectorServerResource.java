package au.com.codeka.warworlds.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;

import org.restlet.data.Form;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.android.c2dm.server.PMF;

import au.com.codeka.warworlds.server.data.StarfieldSectorData;
import au.com.codeka.warworlds.shared.StarfieldSector;
import au.com.codeka.warworlds.shared.StarfieldSectorResource;
import au.com.codeka.warworlds.shared.util.Pair;

public class StarfieldSectorServerResource extends ServerResource
                implements StarfieldSectorResource {
    private static final Logger log = Logger.getLogger(StarfieldSectorServerResource.class.getName());
    private List<Pair<Long, Long>> mSectorsToFetch;

    @Override
    public void doInit() throws ResourceException {
        mSectorsToFetch = new ArrayList<Pair<Long, Long>>();
        ConcurrentMap<String, Object> attrs = getRequest().getAttributes();

        if (attrs.containsKey("sectorX") && attrs.containsKey("sectorY")) {
            long x = Long.parseLong((String) attrs.get("sectorX"));
            long y = Long.parseLong((String) attrs.get("sectorY"));
            mSectorsToFetch.add(new Pair<Long, Long>(x, y));
        }

        Form query = getRequest().getResourceRef().getQueryAsForm();
        String coords = query.getValues("coords");
        if (coords != null) {
            log.info("Got coords: "+coords);
            for(String coord : coords.split(",")) {
                String[] xy = coord.split(":");
                if (xy.length == 2) {
                    long x = Long.parseLong(xy[0]);
                    long y = Long.parseLong(xy[1]);
                    mSectorsToFetch.add(new Pair<Long, Long>(x, y));
                }
            }
        }
    }

    @Override
    public List<StarfieldSector> getSectors() {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            List<StarfieldSector> sectors = new ArrayList<StarfieldSector>();

            for(Pair<Long, Long> xy : mSectorsToFetch) {
                StarfieldSectorData sector = null;

                try {
                    sector = StarfieldSectorData.getSector(pm, xy.one, xy.two);
                } catch(JDOObjectNotFoundException e) {
                    log.info("No sector at ("+xy.one+", "+xy.two+"), creating a new one.");
                    sector = StarfieldSectorData.generate(xy.one, xy.two);
                }

                sectors.add(sector.toStarfieldSector());
            }

            return sectors;
        } finally {
            pm.close();
        }

    }
}
