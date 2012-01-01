package au.com.codeka.warworlds.shared;

import java.util.ArrayList;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;

public interface DebugEntitiesResource {
    @Get
    public ArrayList<String> listEntities();

    @Delete
    public void deleteAllEntities();
}
