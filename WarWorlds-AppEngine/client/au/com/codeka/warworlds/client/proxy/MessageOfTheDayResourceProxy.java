package au.com.codeka.warworlds.client.proxy;

import org.restlet.client.resource.ClientProxy;
import org.restlet.client.resource.Get;
import org.restlet.client.resource.Put;
import org.restlet.client.resource.Result;

import au.com.codeka.warworlds.shared.MessageOfTheDay;

public interface MessageOfTheDayResourceProxy extends ClientProxy {

    @Get
    public void retrieve(Result<MessageOfTheDay> callback);

    @Put
    public void store(MessageOfTheDay motd, Result<Void> callback);

}