package au.com.codeka.warworlds.server;

import org.restlet.resource.ServerResource;

import au.com.codeka.warworlds.server.data.MessageOfTheDayData;
import au.com.codeka.warworlds.shared.MessageOfTheDay;
import au.com.codeka.warworlds.shared.MessageOfTheDayResource;

public class MessageOfTheDayServerResource extends ServerResource implements
		MessageOfTheDayResource {

	@Override
	public MessageOfTheDay retrieve() {
		MessageOfTheDayData data = MessageOfTheDayData.getCurrentMotd();
		if (data == null) {
			return null;
		}

		MessageOfTheDay motd = new MessageOfTheDay();
		motd.setMessage(data.getMessage());
		motd.setPostDate(data.getPostedDate());

		return motd;
	}

	@Override
	public void store(MessageOfTheDay motd) {
		MessageOfTheDayData.updateMotd(motd.getMessage());
	}

}
