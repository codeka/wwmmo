package au.com.codeka.warworlds.server.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import au.com.codeka.warworlds.shared.Device;

import com.google.android.c2dm.server.PMF;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * DeviceData is the details of a device that's registered with us. Useful for sending notifications
 * to, etc.
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class DeviceData {
	@PrimaryKey
	@Persistent
	private Key id;

	@Persistent
	private String user;

	@Persistent
	private String deviceID;
	
	@Persistent
	private String deviceRegistrationID;

	@Persistent
	private String type;

	@Persistent
	private Boolean debug;

	@Persistent
	private Date registrationDate;

	public DeviceData() {
	}
	
	public DeviceData(Device d) {
		if (d.getID() != null && !"".equals(d.getID())) {
			this.id = KeyFactory.createKey(DeviceData.class.getSimpleName(), d.getID());
		}

		this.user = d.getUser();
		this.deviceID = d.getDeviceID();
		this.deviceRegistrationID = d.getDeviceRegistrationID();
		this.type = d.getType();
		this.debug = d.getDebug();
		this.registrationDate = d.getRegistrationDate();
	}
	
	public Key getID() {
		return id;
	}

	public void setID(Key id) {
		this.id = id;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getDeviceID() {
		return deviceID;
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

	public Device toDevice() {
		Device d = new Device();
		d.setID(this.id.toString());
		d.setDeviceID(this.deviceID);
		d.setDeviceRegistrationID(this.deviceRegistrationID);
		d.setUser(this.user);
		d.setType(this.type);
		d.setDebug(this.debug);
		d.setRegistrationDate(this.registrationDate);
		return d;
	}

	private void ensureID() {
    	if (id == null || "".equals(id)) {
    		id = KeyFactory.createKey(DeviceData.class.getSimpleName(), user+"$"+deviceID);
    	}
	}

	/**
	 * Stores the given \c DeviceData object in the persistence store.
	 * @param d
	 */
	public static void store(DeviceData d) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            d.ensureID();
            pm.makePersistent(d);
        } finally {
            pm.close();
        }
	}

	/**
	 * Removes the given registered device.
	 */
	public static void remove(String deviceRegistrationID, String user) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
        	DeviceData dd = getDeviceForRegistrationID(pm, deviceRegistrationID);
        	if (dd != null && dd.getUser().equals(user)) {
        		pm.deletePersistent(dd);
        	}
        } finally {
        	pm.close();
        }
	}

	/**
	 * Gets a \c DeviceData for the given \c deviceRegistrationID (or \c null if no such
	 * registration can be found).
	 */
	public static DeviceData getDeviceForRegistrationID(String deviceRegistrationID) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
        	return getDeviceForRegistrationID(pm, deviceRegistrationID);
        } finally {
        	pm.close();
        }
	}
	
	private static DeviceData getDeviceForRegistrationID(PersistenceManager pm, String deviceRegistrationID) {
    	Query query = pm.newQuery(DeviceData.class);
    	query.setFilter("deviceRegistrationID == '"+deviceRegistrationID+"'");

    	@SuppressWarnings("unchecked")
        List<DeviceData> qresult = (List<DeviceData>) query.execute();
    	
    	DeviceData result = null;
    	for (DeviceData d : qresult) {
    		result = d;
    	}
    	query.closeAll();

    	return result;
	}
	
	/**
	 * Gets a list of \c DeviceData objects that represents the device(s) registered
	 * for a given user.
	 * 
	 * @param user The username (email address) of the user you want all the devices for.
	 */
    public static List<DeviceData> getDevicesForUser(String user) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
          user = user.toLowerCase(Locale.ENGLISH);

          Query query = pm.newQuery(DeviceData.class);
          query.setFilter("user == '" + user + "'");

          @SuppressWarnings("unchecked")
          List<DeviceData> qresult = (List<DeviceData>) query.execute();

          List<DeviceData> result = new ArrayList<DeviceData>();
          for (DeviceData d : qresult) {
            result.add(d);
          }
          query.closeAll();

          return result;
        } finally {
          pm.close();
        }
    }

}
