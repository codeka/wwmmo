package au.com.codeka.warworlds.server;

import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.restlet.resource.ServerResource;

import com.google.android.c2dm.server.C2DMessaging;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import au.com.codeka.warworlds.server.data.DeviceData;
import au.com.codeka.warworlds.shared.DevicesNotificationsResource;
import au.com.codeka.warworlds.shared.Notification;

public class DevicesNotificationsServerResource extends ServerResource implements DevicesNotificationsResource {
	private static final Logger log = Logger.getLogger(DevicesServerResource.class.getName());

	@Override
	public void send(Notification n) {
		log.info("Sending notification to \""+n.getUser()+"\": "+n.getMessage());

		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		String sender = "nobody";
		if (user != null) {
			sender = user.getEmail();
		}

		// right??
		ServletContext servletContext = (ServletContext) 
		        getContext().getServerDispatcher().getContext() 
		        .getAttributes().get("org.restlet.ext.servlet.ServletContext");
		C2DMessaging push = C2DMessaging.get(servletContext);

	 	List<DeviceData> registrations = DeviceData.getDevicesForUser(n.getUser());

	 	int numSendAttempts = 0;
	 	for (DeviceData device : registrations) {
//            if (!"ac2dm".equals(device.getType())) {
// 	              continue; // hmm, they should all be ac2dm..?
//            }

	 		doSend(n, device, sender, push);
	        numSendAttempts++;
	 	}

	 	if (numSendAttempts == 0) {
	 		log.warning("No user \""+n.getUser()+"\" registered, no message sent.");
	 	}
	}
	
	private boolean doSend(Notification n, DeviceData device, String sender, C2DMessaging push) {
	    String msg = n.getMessage();
	    if (msg.length() > 1000) {
	    	msg = msg.substring(0, 1000) + "[...]";
	    }

	    return push.sendNoRetry(device.getDeviceRegistrationID(), ""+msg.hashCode(),
	    		"sender", sender,
	    		"message", msg);

	}

}
