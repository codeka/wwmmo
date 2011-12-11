package au.com.codeka.warworlds.client.pages;

import java.util.Date;

import org.restlet.client.resource.Result;

import au.com.codeka.warworlds.client.Connector;
import au.com.codeka.warworlds.client.proxy.MessageOfTheDayResourceProxy;
import au.com.codeka.warworlds.shared.MessageOfTheDay;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

public class MotdPage extends BasePage {
	public static String TOKEN = "motd";

	interface MotdPageUiBinder extends UiBinder<Widget, MotdPage> {
	}
	private static MotdPageUiBinder uiBinder = GWT.create(MotdPageUiBinder.class);

	@UiField
	Button saveMotd;
	
	@UiField
	TextAreaElement newMotd;
	
	@UiField
	DivElement currMotd;
	
	public MotdPage() {
		initWidget(uiBinder.createAndBindUi(this));

	    saveMotd.addClickHandler(new ClickHandler() {
	        public void onClick(ClickEvent event) {
	        	saveMotd.setEnabled(false);
	        	setStatus("Saving...");

	        	MessageOfTheDay motd = new MessageOfTheDay();
	        	motd.setMessage(newMotd.getValue());
	        	motd.setPostDate(new Date());

	        	MessageOfTheDayResourceProxy proxy = GWT.create(MessageOfTheDayResourceProxy.class);
	        	proxy.getClientResource().setReference("/api/v1/motd");
	        	proxy.store(motd, new Result<Void>() {

					@Override
					public void onFailure(Throwable caught) {
	            		saveMotd.setEnabled(true);
	            		setStatus(caught.getMessage(), true);
					}

					@Override
					public void onSuccess(Void na) {
            			refreshMotd();
					}
	        	});
	        }
	    });

	    refreshMotd();
	}
	
	private void refreshMotd() {
		setStatus("Refreshing...");
		saveMotd.setEnabled(false);

    	MessageOfTheDayResourceProxy proxy = Connector.create(MessageOfTheDayResourceProxy.class, "/motd");
    	proxy.retrieve(new Result<MessageOfTheDay>() {

			@Override
			public void onFailure(Throwable caught) {
				saveMotd.setEnabled(true);
				setStatus(caught.getMessage(), true);
			}

			@Override
			public void onSuccess(MessageOfTheDay motd) {
				saveMotd.setEnabled(true);
				setStatus("Success", 5000);
				currMotd.setInnerHTML(motd.getMessage());
				newMotd.setValue(motd.getMessage());
			}
    	});
	}

	@Override
	public String getTitle() {
		return TOKEN;
	}

}
