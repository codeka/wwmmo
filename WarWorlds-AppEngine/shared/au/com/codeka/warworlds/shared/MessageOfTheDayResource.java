package au.com.codeka.warworlds.shared;

import org.restlet.resource.Get;
import org.restlet.resource.Put;

public interface MessageOfTheDayResource {

	@Get
	public MessageOfTheDay retrieve();
	
	@Put
	public void store(MessageOfTheDay motd);

}
