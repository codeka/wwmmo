package au.com.codeka.warworlds.shared;

import java.util.List;

import org.restlet.resource.Get;

public interface StarfieldSectorResource {
    @Get
    public List<StarfieldSector> getSectors();
}
