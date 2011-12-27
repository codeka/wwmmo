package au.com.codeka.warworlds.client.proxy;

import org.restlet.client.resource.ClientProxy;
import org.restlet.client.resource.Result;
import org.restlet.client.resource.Get;

import au.com.codeka.warworlds.shared.StarfieldSector;

public interface StarfieldSectorResourceProxy extends ClientProxy {
    @Get
    public void fetch(Result<StarfieldSector> callback);
}
