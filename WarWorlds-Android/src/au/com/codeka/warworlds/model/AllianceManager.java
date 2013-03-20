package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class AllianceManager {
    private static AllianceManager sInstance = new AllianceManager();

    public static AllianceManager getInstance() {
        return sInstance;
    }

    /**
     * Fetches all alliances from the server.
     */
    public void fetchAlliances(final FetchAlliancesCompleteHandler handler) {
        new AsyncTask<Void, Void, List<Alliance>>() {
            @Override
            protected List<Alliance> doInBackground(Void... params) {
                ArrayList<Alliance> alliances;

                String url = "alliances";
                try {
                    Messages.Alliances pb = ApiClient.getProtoBuf(url, Messages.Alliances.class);
                    alliances = new ArrayList<Alliance>();
                    for (Messages.Alliance alliance_pb : pb.getAlliancesList()) {
                        alliances.add(Alliance.fromProtocolBuffer(alliance_pb));
                    }
                    return alliances;
                } catch(ApiException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Alliance> alliances) {
                if (handler != null) {
                    handler.onAlliancesFetched(alliances);
                }
            }
        }.execute();
    }

    /**
     * Fetches details, including memberships, of the alliance with the given key.
     */
    public void fetchAlliance(final String allianceKey, final FetchAllianceCompleteHandler handler) {
        new AsyncTask<Void, Void, Alliance>() {
            @Override
            protected Alliance doInBackground(Void... params) {
                String url = "alliances/"+allianceKey;
                try {
                    Messages.Alliance pb = ApiClient.getProtoBuf(url, Messages.Alliance.class);
                    return Alliance.fromProtocolBuffer(pb);
                } catch(ApiException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Alliance alliance) {
                if (handler != null && alliance != null) {
                    handler.onAllianceFetched(alliance);
                }
            }
        }.execute();

    }

    public interface FetchAlliancesCompleteHandler {
        void onAlliancesFetched(List<Alliance> alliances);
    }

    public interface FetchAllianceCompleteHandler {
        void onAllianceFetched(Alliance alliances);
    }
}
