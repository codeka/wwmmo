package au.com.codeka.warworlds.client.pages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Widget;

public class DashboardPage extends BasePage {
	public static String TOKEN = "dashboard";

	interface DashboardPageUiBinder extends UiBinder<Widget, DashboardPage> {
	}
	private static DashboardPageUiBinder uiBinder = GWT.create(DashboardPageUiBinder.class);

	public DashboardPage() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	@Override
	public String getTitle() {
		return TOKEN;
	}
}
