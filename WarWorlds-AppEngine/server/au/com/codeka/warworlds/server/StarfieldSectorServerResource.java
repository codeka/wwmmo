package au.com.codeka.warworlds.server;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;

import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.android.c2dm.server.PMF;

import au.com.codeka.warworlds.server.data.StarfieldSectorData;
import au.com.codeka.warworlds.shared.StarfieldSector;
import au.com.codeka.warworlds.shared.StarfieldSectorResource;

public class StarfieldSectorServerResource extends ServerResource
                implements StarfieldSectorResource {
    private long mSectorX;
    private long mSectorY;

    @Override
    public void doInit() throws ResourceException {
        this.mSectorX = Long.parseLong((String) getRequest().getAttributes().get("sectorX"));
        this.mSectorY = Long.parseLong((String) getRequest().getAttributes().get("sectorY"));
    }

    @Override
    public StarfieldSector getSector() {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            StarfieldSectorData sector = null;

            try {
                sector = StarfieldSectorData.getSector(pm, mSectorX, mSectorY);
            } catch(JDOObjectNotFoundException e) {
                sector = StarfieldSectorData.generate(mSectorX, mSectorY);
            }

            return sector.toStarfieldSector();
        } finally {
            pm.close();
        }

    }
}
