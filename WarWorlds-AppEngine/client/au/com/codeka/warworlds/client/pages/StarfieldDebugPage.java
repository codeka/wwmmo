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
    Button mRefresh;

    public StarfieldDebugPage() {
        initWidget(uiBinder.createAndBindUi(this));

        mSectorX.setValue("0");
        mSectorY.setValue("0");

        mRefresh.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                refreshSector();
            }
            
        });
    }

    private void refreshSector() {
        setStatus("Refreshing...");
        mRefresh.setEnabled(false);

        String url = "/starfield/sector/"+mSectorX.getValue()+"/"+mSectorY.getValue();

        Request request = Connector.createRequest(Method.GET, url);

        // make sure we get back JSON...
        List<Preference<MediaType>> mediaTypes = request.getClientInfo().getAcceptedMediaTypes();
        mediaTypes.clear();
        mediaTypes.add(new Preference<MediaType>(MediaType.APPLICATION_JSON, 1.0f));

        new Client(Protocol.HTTP).handle(request, new Uniform() {

            @Override
            public void handle(Request request, Response response) {
                mRefresh.setEnabled(true);
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
