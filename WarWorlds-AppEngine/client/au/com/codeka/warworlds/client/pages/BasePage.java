package au.com.codeka.warworlds.client.pages;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RootPanel;

public class BasePage extends Composite {

	private static final String STATUS_ERROR = "error";
	private static final String STATUS_NONE = "none";
	private static final String STATUS_SUCCESS = "success";

	private Timer statusTimer = new Timer() {
		@Override
		public void run() {
			Element status = (Element) RootPanel.get("status").getElement();
			status.setInnerText("");
			status.setClassName(STATUS_NONE);
		}
	};

	protected void setStatus(String message) {
		setStatus(message, false, 0);
	}
	
	protected void setStatus(String message, int delay) {
		setStatus(message, false, delay);
	}

	protected void setStatus(String message, boolean error) {
		setStatus(message, error, 0);
	}
	
	protected void setStatus(String message, boolean error, int delay) {
		Element status = (Element) RootPanel.get("status").getElement();
		status.setInnerText(message);
		if (error) {
			status.setClassName(STATUS_ERROR);
		} else {
			if (message.length() == 0) {
				status.setClassName(STATUS_NONE);
			} else {
				status.setClassName(STATUS_SUCCESS);
			}
		}

		if (delay != 0) {
			statusTimer.schedule(delay);
		}
	}
}
