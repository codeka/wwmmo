package au.com.codeka.warworlds.shared;

import org.restlet.resource.Get;

public interface StarfieldSectorResource {
    @Get
    public StarfieldSector getSector();
}
