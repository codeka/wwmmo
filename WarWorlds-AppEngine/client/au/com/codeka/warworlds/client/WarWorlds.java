
package au.com.codeka.warworlds.client;

import au.com.codeka.warworlds.client.pages.DashboardPage;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class WarWorlds implements EntryPoint, ValueChangeHandler<String> {
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		History.addValueChangeHandler(this);
		if (History.getToken().isEmpty()) {
			History.newItem("warworlds");
		}

		Navigation.go(new DashboardPage());
	}

	@Override
	public void onValueChange(ValueChangeEvent<String> event) {
		Navigation.go(History.getToken());
	}
}
