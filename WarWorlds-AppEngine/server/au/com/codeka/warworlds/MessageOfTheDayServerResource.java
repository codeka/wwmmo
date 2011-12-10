package au.com.codeka.warworlds;

import java.util.Date;

import org.restlet.resource.ServerResource;

import au.com.codeka.warworlds.shared.MessageOfTheDay;
import au.com.codeka.warworlds.shared.MessageOfTheDayResource;

public class MessageOfTheDayServerResource extends ServerResource implements
		MessageOfTheDayResource {

	@Override
	public MessageOfTheDay retrieve() {
		MessageOfTheDay motd = new MessageOfTheDay();
		motd.setMessage("Hello World");
		motd.setPostDate(new Date());
		return motd;
	}

	@Override
	public void store(MessageOfTheDay motd) {
		// TODO
	}

}
