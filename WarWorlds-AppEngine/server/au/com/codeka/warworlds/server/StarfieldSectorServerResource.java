package au.com.codeka.warworlds.server;

import javax.jdo.JDOObjectNotFoundException;

import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

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
    public StarfieldSector fetch() {
        StarfieldSectorData sector = null;
        try {
            sector = StarfieldSectorData.getSector(mSectorX, mSectorY);
        } catch(JDOObjectNotFoundException e) {
            sector = StarfieldSectorData.generate(mSectorX, mSectorY);
        }

        return sector.toStarfieldSector();
    }

}
