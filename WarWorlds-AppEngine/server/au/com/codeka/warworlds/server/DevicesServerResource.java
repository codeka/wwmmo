package au.com.codeka.warworlds.server;

import java.util.List;

import org.restlet.resource.ServerResource;

import au.com.codeka.warworlds.server.data.DeviceData;
import au.com.codeka.warworlds.shared.Device;
import au.com.codeka.warworlds.shared.DevicesResource;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

public class DevicesServerResource extends ServerResource implements DevicesResource {
	@Override
	public List<Device> retrieve() {
		return null; // TODO
	}

	@Override
	public void register(Device d) {
		DeviceData data = new DeviceData(d);
		
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		if (user == null) {
			throw new RuntimeException("Should be logged in!");// should never happen...
		}
		data.setUser(user.getEmail());

		DeviceData.store(data);
	}

}
