package au.com.codeka.warworlds.shared;

import java.io.Serializable;
import java.util.Date;

public class MessageOfTheDay implements Serializable {

	private static final long serialVersionUID = 1L;

	private String message;
	private Date postDate;

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
