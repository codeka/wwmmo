package au.com.codeka.warworlds.client.pages;

import java.io.IOException;
import java.util.List;

import org.restlet.client.Client;
import org.restlet.client.Request;
import org.restlet.client.Response;
import org.restlet.client.Uniform;
import org.restlet.client.data.MediaType;
import org.restlet.client.data.Method;
import org.restlet.client.data.Preference;
import org.restlet.client.data.Protocol;
import org.restlet.client.representation.Representation;

import au.com.codeka.warworlds.client.Connector;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

public class StarfieldDebugPage extends BasePage {
    public static String TOKEN = "starfield-debug";

    interface StarfieldDebugPageUiBinder extends UiBinder<Widget, StarfieldDebugPage> {
    }
    private static StarfieldDebugPageUiBinder uiBinder = GWT.create(StarfieldDebugPageUiBinder.class);

    @UiField
    InputElement mSectorX;

    @UiField
    InputElement mSectorY;

    @UiField
    TextAreaElement mSectorData;

    @UiField
    Button mSingleRefresh;

    @UiField
    InputElement mSectorQuery;

    @UiField
    Button mQueryRefresh;

    public StarfieldDebugPage() {
        initWidget(uiBinder.createAndBindUi(this));

        mSectorX.setValue("0");
        mSectorY.setValue("0");

        mSingleRefresh.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                refreshSector(true);
            }
        });
        mQueryRefresh.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                refreshSector(false);
            }
        });
    }

    private void refreshSector(boolean single) {
        setStatus("Refreshing...");
        mSingleRefresh.setEnabled(false);

        String url = "/sectors";
        if (single) {
            url += "/"+mSectorX.getValue()+"/"+mSectorY.getValue();
        } else {
            url += "?coords="+mSectorQuery.getValue();
        }

        Request request = Connector.createRequest(Method.GET, url);

        // make sure we get back JSON...
        List<Preference<MediaType>> mediaTypes = request.getClientInfo().getAcceptedMediaTypes();
        mediaTypes.clear();
        mediaTypes.add(new Preference<MediaType>(MediaType.APPLICATION_JSON, 1.0f));

        new Client(Protocol.HTTP).handle(request, new Uniform() {

            @Override
            public void handle(Request request, Response response) {
                mSingleRefresh.setEnabled(true);
                setStatus("Success", 5000);

                Representation r = response.getEntity();
                try {
                    mSectorData.setValue(r.getText());
                } catch (IOException e) {
                }
            }
        });
    }

    @Override
    public String getTitle() {
        return TOKEN;
    }
}
