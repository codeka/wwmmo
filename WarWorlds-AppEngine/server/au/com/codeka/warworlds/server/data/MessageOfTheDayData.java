package au.com.codeka.warworlds.server.data;

import java.util.Date;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.android.c2dm.server.PMF;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

/**
 * MessageOfTheDay is the HTML we send to the client when it first starts up.
 * 
 * @author dean@codeka.com.au
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class MessageOfTheDayData {

	/**
	 * There's only ever one MessageOfTheDay, but this makes it easy
	 * to query/update.
	 */
	@PrimaryKey
	@Persistent
	private Key key;

	@Persistent
	private Text message;
	
	@Persistent
	private Date postedDate;

	public MessageOfTheDayData(Key key) {
		this.key = key;
	}
	
	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public String getMessage() {
		return message.getValue();
	}

	public void setMessage(String message) {
		this.message = new Text(message);
	}

	public Date getPostedDate() {
		return postedDate;
	}

	public void setPostedDate(Date postedDate) {
		this.postedDate = postedDate;
	}
	
	/**
	 * Fetches the current MOTD from the data store. (TODO: cache)
	 */
	public static MessageOfTheDayData getCurrentMotd() {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Key key = KeyFactory.createKey(MessageOfTheDayData.class.getSimpleName(), 1);
			MessageOfTheDayData motd = pm.getObjectById(MessageOfTheDayData.class, key);
			return motd;
		} catch (JDOObjectNotFoundException e) {
			return null;
		} finally {
			pm.close();
		}
	}
	
	/**
	 * Saves the given message as the current MOTD.
	 */
	public static void updateMotd(String message) {
		MessageOfTheDayData motd = getCurrentMotd();
		if (motd == null) {
			Key key = KeyFactory.createKey(MessageOfTheDayData.class.getSimpleName(), 1);
			motd = new MessageOfTheDayData(key);
		}
		motd.setMessage(message);
		motd.setPostedDate(new Date());

		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			pm.makePersistent(motd);
		} finally {
			pm.close();
		}
	}
	
	@Override
	public String toString() {
		return "MessageOfTheDayData[key="+key+", message="+message.toString().substring(0,100)+"..."
		      +", postedDate="+postedDate+"]";
	}
}
