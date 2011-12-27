package au.com.codeka.warworlds;

import java.util.logging.Logger;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;

import au.com.codeka.warworlds.server.*;

public class WarWorldsApplication extends Application {
    private static final Logger log = Logger.getLogger(WarWorldsApplication.class.getName());

    @Override
    public Restlet createInboundRoot() {
        log.info("WarWorldsApplication starting up.");

        Router router = new Router(getContext());

        router.attachDefault(new Directory(getContext(), "war:///"));
        router.attach("/api/v1/motd", MessageOfTheDayServerResource.class);
        router.attach("/api/v1/devices/notifications", DevicesNotificationsServerResource.class);
        router.attach("/api/v1/devices/{deviceRegistrationID}", DeviceServerResource.class);
        router.attach("/api/v1/devices", DevicesServerResource.class);
        router.attach("/api/v1/starfield/sector/{sectorX}/{sectorY}", StarfieldSectorServerResource.class);

        return router;
    }
}
