package au.com.codeka.warworlds.server;

import java.util.logging.Logger;

import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import au.com.codeka.warworlds.server.data.DeviceData;
import au.com.codeka.warworlds.shared.Device;
import au.com.codeka.warworlds.shared.DeviceResource;

public class DeviceServerResource extends ServerResource implements DeviceResource {
	private static final Logger log = Logger.getLogger(DevicesServerResource.class.getName());

	private String deviceRegistrationID;
	
	@Override
	public void doInit() throws ResourceException {
		this.deviceRegistrationID = (String) getRequest().getAttributes().get("deviceRegistrationID");
	}

	@Override
	public Device retrieve() {
		DeviceData dd = DeviceData.getDeviceForRegistrationID(deviceRegistrationID);
		if (dd == null) {
			return null;
		}

		return dd.toDevice();
	}

	@Override
	public void unregister() {
		log.info("Registering device, deviceRegistrationID="+deviceRegistrationID);

		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();

		DeviceData.remove(deviceRegistrationID, user.getEmail());
	}
}
