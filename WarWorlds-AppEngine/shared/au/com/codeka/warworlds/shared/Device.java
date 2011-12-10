package au.com.codeka.warworlds.shared;

import java.io.Serializable;
import java.util.Date;

public class Device implements Serializable {
	private static final long serialVersionUID = 1L;

	private String id;
	private String user;
	private String deviceID;
	private String deviceRegistrationID;
	private String type;
	private Boolean debug;
	private Date registrationDate;
	
	public String getID() {
		return id;
	}
	public void setID(String id) {
		this.id = id;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getDeviceID() {
		return this.deviceID;
	}
	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}
	public String getDeviceRegistrationID() {
		return deviceRegistrationID;
	}
	public void setDeviceRegistrationID(String deviceRegistrationID) {
		this.deviceRegistrationID = deviceRegistrationID;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Boolean getDebug() {
		return debug;
	}
	public void setDebug(Boolean debug) {
		this.debug = debug;
	}
	public Date getRegistrationDate() {
		return registrationDate;
	}
	public void setRegistrationDate(Date registrationDate) {
		this.registrationDate = registrationDate;
	}
}
