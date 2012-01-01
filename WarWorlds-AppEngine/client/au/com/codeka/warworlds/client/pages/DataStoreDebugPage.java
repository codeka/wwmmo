package au.com.codeka.warworlds.client.pages;

import java.util.ArrayList;

import org.restlet.client.resource.Result;

import au.com.codeka.warworlds.client.Connector;
import au.com.codeka.warworlds.client.proxy.DebugEntitiesResourceProxy;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public class DataStoreDebugPage extends BasePage {
    public static String TOKEN = "datastore-debug";

    interface DataStoreDebugPageUiBinder extends UiBinder<Widget, DataStoreDebugPage> {
    }
    private static DataStoreDebugPageUiBinder uiBinder = GWT.create(DataStoreDebugPageUiBinder.class);

    @UiField
    ListBox mEntityTypes;

    @UiField
    Button mBulkDelete;

    public DataStoreDebugPage() {
        initWidget(uiBinder.createAndBindUi(this));

        mBulkDelete.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                int selectedIndex = mEntityTypes.getSelectedIndex();
                if (selectedIndex < 0) {
                    return;
                }

                String entityType = mEntityTypes.getItemText(selectedIndex);
                boolean proceed = Window.confirm("Are you sure you want to " +
                        "bulk delete all entities of kind \""+entityType+"\"?");
                if (proceed) {
                    performBulkDelete(entityType);
                }
            }
        });

        refreshEntityList();
    }

    private void performBulkDelete(String entityType) {
        setStatus("Deleting all \""+entityType+"\"...");
        mBulkDelete.setEnabled(false);
        mEntityTypes.setEnabled(false);

        DebugEntitiesResourceProxy proxy = Connector.create(
                GWT.create(DebugEntitiesResourceProxy.class), "/debug/entities/"+entityType);
        proxy.deleteAllEntities(new Result<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                setStatus(caught.getMessage(), true);
            }

            @Override
            public void onSuccess(Void result) {
                setStatus("Success! Check the AppEngine console for progress (it can take a while)...");
                mBulkDelete.setEnabled(true);
                mEntityTypes.setEnabled(true);
            }
        });
    }

    private void refreshEntityList() {
        setStatus("Refreshing...");

        mBulkDelete.setEnabled(false);
        mEntityTypes.setEnabled(false);

        DebugEntitiesResourceProxy proxy = Connector.create(
                GWT.create(DebugEntitiesResourceProxy.class), "/debug/entities");
        proxy.listEntities(new Result<ArrayList<String>>() {
            @Override
            public void onFailure(Throwable caught) {
                mEntityTypes.clear();
                setStatus(caught.getMessage(), true);
            }

            @Override
            public void onSuccess(ArrayList<String> result) {
                setStatus("Success!", 5000);

                mEntityTypes.clear();
                for(String entityName : result) {
                    mEntityTypes.addItem(entityName);
                }
                mEntityTypes.setEnabled(true);
                mBulkDelete.setEnabled(true);
            }
        });
    }
}
