package au.com.codeka.warworlds.shared;

import java.util.List;

import org.restlet.resource.Get;
import org.restlet.resource.Put;

/**
 * A resource for querying devices (e.g. all devices for a user, all users for a device, etc)
 */
public interface DevicesResource {
	@Get
	public List<Device> retrieve();

	@Put
	public void register(Device d);
}
