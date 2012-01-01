package au.com.codeka.warworlds.shared;

import java.util.ArrayList;

import org.restlet.resource.Get;

public interface StarfieldSectorResource {
    @Get
    public ArrayList<StarfieldSector> getSectors();
}
