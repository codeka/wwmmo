package au.com.codeka.warworlds.shared;

import org.restlet.resource.Put;

public interface DevicesNotificationsResource {
	@Put
	public void send(Notification n);
}
