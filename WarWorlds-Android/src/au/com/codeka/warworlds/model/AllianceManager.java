package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import android.content.Context;
import android.os.AsyncTask;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class AllianceManager {
    private static AllianceManager sInstance = new AllianceManager();
    public static AllianceManager getInstance() {
        return sInstance;
    }

    private List<AllianceUpdatedHandler> mAllianceUpdatedHandlers;

    private AllianceManager() {
        mAllianceUpdatedHandlers = new ArrayList<AllianceUpdatedHandler>();
    }

    public void addAllianceUpdatedHandler(AllianceUpdatedHandler handler) {
        mAllianceUpdatedHandlers.add(handler);
    }
    public void removeAllianceUpdatedHandler(AllianceUpdatedHandler handler) {
        mAllianceUpdatedHandlers.remove(handler);
    }
    protected void fireAllianceUpdated(Alliance alliance) {
        for (AllianceUpdatedHandler handler : mAllianceUpdatedHandlers) {
            handler.onAllianceUpdated(alliance);
        }
        EmpireManager.getInstance().onAllianceUpdated(alliance);
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
                fireAllianceUpdated(alliance);
            }
        }.execute();
    }

    public void fetchJoinRequests(final Context context,
                                  final String allianceKey,
                                  final FetchJoinRequestsCompleteHandler handler) {
        new AsyncTask<Void, Void, List<AllianceJoinRequest>>() {
            private TreeMap<String, Empire> mEmpires;

            @Override
            protected List<AllianceJoinRequest> doInBackground(Void... params) {
                String url = "alliances/"+allianceKey+"/join-requests";
                try {
                    Messages.AllianceJoinRequests pb = ApiClient.getProtoBuf(url, Messages.AllianceJoinRequests.class);
                    ArrayList<AllianceJoinRequest> joinRequests = new ArrayList<AllianceJoinRequest>();
                    TreeSet<String> empireKeys = new TreeSet<String>();
                    for (Messages.AllianceJoinRequest join_request_pb : pb.getJoinRequestsList()) {
                        joinRequests.add(AllianceJoinRequest.fromProtocolBuffer(join_request_pb));
                        if (!empireKeys.contains(join_request_pb.getEmpireKey())) {
                            empireKeys.add(join_request_pb.getEmpireKey());
                        }
                    }

                    List<Empire> empires = EmpireManager.getInstance().fetchEmpiresSync(context, empireKeys);
                    mEmpires = new TreeMap<String, Empire>();
                    for (Empire empire : empires) {
                        mEmpires.put(empire.getKey(), empire);
                    }

                    return joinRequests;
                } catch(ApiException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<AllianceJoinRequest> joinRequests) {
                if (handler != null && joinRequests != null) {
                    handler.onJoinRequestsFetched(mEmpires, joinRequests);
                }
            }
        }.execute();
    }

    /**
     * Sends a request to join the specified alliance.
     */
    public void requestJoin(final String allianceKey, final String message) {
        final MyEmpire myEmpire = EmpireManager.getInstance().getEmpire();

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                String url = "alliances/"+allianceKey+"/join-requests";
                Messages.AllianceJoinRequest pb = Messages.AllianceJoinRequest.newBuilder()
                                    .setAllianceKey(allianceKey)
                                    .setEmpireKey(myEmpire.getKey())
                                    .setMessage(message)
                                    .build();
                try {
                    ApiClient.postProtoBuf(url, pb);
                    return true;
                } catch(ApiException e) {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    refreshAlliance(allianceKey);
                }
            }
        }.execute();
    }

    public void updateJoinRequest(final AllianceJoinRequest joinRequest) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                String url = "alliances/"+joinRequest.getAllianceKey()+"/join-requests";
                Messages.AllianceJoinRequest pb = Messages.AllianceJoinRequest.newBuilder()
                                    .setKey(joinRequest.getKey())
                                    .setAllianceKey(joinRequest.getAllianceKey())
                                    .setEmpireKey(joinRequest.getEmpireKey())
                                    .setMessage(joinRequest.getMessage())
                                    .setState(Messages.AllianceJoinRequest.RequestState.valueOf(joinRequest.getState().getValue()))
                                    .build();
                try {
                    ApiClient.putProtoBuf(url, pb);
                    return true;
                } catch(ApiException e) {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    refreshAlliance(joinRequest.getAllianceKey());
                }
            }
        }.execute();
    }

    private void refreshAlliance(String allianceKey) {
        fetchAlliance(allianceKey, null);
    }

    public interface FetchAlliancesCompleteHandler {
        void onAlliancesFetched(List<Alliance> alliances);
    }
    public interface FetchAllianceCompleteHandler {
        void onAllianceFetched(Alliance alliance);
    }
    public interface AllianceUpdatedHandler {
        void onAllianceUpdated(Alliance alliance);
    }
    public interface FetchJoinRequestsCompleteHandler {
        void onJoinRequestsFetched(Map<String, Empire> empires, List<AllianceJoinRequest> joinRequests);
    }
}
