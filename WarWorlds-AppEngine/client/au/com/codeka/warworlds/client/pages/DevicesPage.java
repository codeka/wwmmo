package au.com.codeka.warworlds.client.pages;

import java.util.Date;

import org.restlet.client.resource.Result;

import au.com.codeka.warworlds.client.Connector;
import au.com.codeka.warworlds.client.proxy.DevicesNotificationsResourceProxy;
import au.com.codeka.warworlds.shared.Notification;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

public class DevicesPage extends BasePage {
	public static String TOKEN = "devices";

	@UiField
	Button mPostMessage;

	@UiField
	InputElement mUserEmail;
	
	@UiField
	InputElement mMessage;
	
	interface DevicesPageUiBinder extends UiBinder<Widget, DevicesPage> {
	}
	private static DevicesPageUiBinder uiBinder = GWT.create(DevicesPageUiBinder.class);

	public DevicesPage() {
		initWidget(uiBinder.createAndBindUi(this));
		
		mPostMessage.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				DevicesNotificationsResourceProxy proxy = Connector.create(
						GWT.create(DevicesNotificationsResourceProxy.class), "/devices/notifications");
				
				Notification n = new Notification();
				n.setMessage(mMessage.getValue());
				n.setUser(mUserEmail.getValue());
				n.setPostDate(new Date());
				
				mPostMessage.setEnabled(false);
				proxy.send(n, new Result<Void>() {

					@Override
					public void onFailure(Throwable caught) {
						mPostMessage.setEnabled(true);
	            		setStatus(caught.getMessage(), true);
					}

					@Override
					public void onSuccess(Void na) {
						mPostMessage.setEnabled(true);
						setStatus("Message Sent!", 5000);
						
						mMessage.setValue("");
					}
	        	});
			}
		});
	}

	@Override
	public String getTitle() {
		return TOKEN;
	}
}
