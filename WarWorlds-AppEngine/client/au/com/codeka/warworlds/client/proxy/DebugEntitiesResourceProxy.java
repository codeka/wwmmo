package au.com.codeka.warworlds.client.proxy;

import java.util.ArrayList;

import org.restlet.client.resource.ClientProxy;
import org.restlet.client.resource.Delete;
import org.restlet.client.resource.Get;
import org.restlet.client.resource.Result;

public interface DebugEntitiesResourceProxy extends ClientProxy {
    @Get
    public void listEntities(Result<ArrayList<String>> callback);

    @Delete
    public void deleteAllEntities(Result<Void> callback);
}
