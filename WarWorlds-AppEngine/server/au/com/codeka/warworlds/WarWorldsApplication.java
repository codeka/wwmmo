package au.com.codeka.warworlds;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;

import au.com.codeka.warworlds.server.MessageOfTheDayServerResource;

public class WarWorldsApplication extends Application {

	@Override
	public Restlet createInboundRoot() {
		Router router = new Router(getContext());

		router.attachDefault(new Directory(getContext(), "war:///"));
		router.attach("/motd", MessageOfTheDayServerResource.class);

		return router;
	}

}
