package au.com.codeka.warworlds;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;

import au.com.codeka.warworlds.server.*;

public class WarWorldsApplication extends Application {

	@Override
	public Restlet createInboundRoot() {
		Router router = new Router(getContext());

		router.attachDefault(new Directory(getContext(), "war:///"));
		router.attach("/api/v1/motd", MessageOfTheDayServerResource.class);
		router.attach("/api/v1/devices/notifications", DevicesNotificationsServerResource.class);
		router.attach("/api/v1/devices/{deviceRegistrationID}", DeviceServerResource.class);
		router.attach("/api/v1/devices", DevicesServerResource.class);

		return router;
	}

}
