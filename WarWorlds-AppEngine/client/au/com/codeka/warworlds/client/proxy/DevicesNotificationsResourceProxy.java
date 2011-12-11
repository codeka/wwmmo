package au.com.codeka.warworlds.client.proxy;

import org.restlet.client.resource.ClientProxy;
import org.restlet.client.resource.Put;
import org.restlet.client.resource.Result;

import au.com.codeka.warworlds.shared.Notification;

public interface DevicesNotificationsResourceProxy extends ClientProxy {
    @Put
    public void send(Notification n, Result<Void> callback);
}
