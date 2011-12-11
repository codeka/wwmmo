package au.com.codeka.warworlds.shared;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;

public interface DeviceResource {

	@Get
	public Device retrieve();

	@Delete
	public void unregister();
}
