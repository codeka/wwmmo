package au.com.codeka.warworlds.shared;

import java.io.Serializable;
import java.util.Date;

public class Notification implements Serializable {
	private static final long serialVersionUID = 1L;

	private String user;
	private String message;
	private Date postDate;

	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Date getPostDate() {
		return postDate;
	}
	public void setPostDate(Date postDate) {
		this.postDate = postDate;
	}
	
}
