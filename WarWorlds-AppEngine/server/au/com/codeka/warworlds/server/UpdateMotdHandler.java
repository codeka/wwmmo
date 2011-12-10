package au.com.codeka.warworlds.server;

import au.com.codeka.warworlds.server.data.MessageOfTheDay;

public class UpdateMotdHandler {

	private String message;
	
	public UpdateMotdHandler() {
	}
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	public String update() {
		MessageOfTheDay.updateMotd(this.message);
		return "Success";
	}
}
