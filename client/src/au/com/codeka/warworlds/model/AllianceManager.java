package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.model.Alliance;
import au.com.codeka.common.model.AllianceRequest;
import au.com.codeka.common.model.AllianceRequestVote;
import au.com.codeka.common.model.AllianceRequests;
import au.com.codeka.common.model.Alliances;
import au.com.codeka.common.model.Empire;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;

public class AllianceManager {
    public static AllianceManager i = new AllianceManager();

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
        EmpireManager.i.onAllianceUpdated(alliance);
    }

    /**
     * Fetches all alliances from the server.
     */
    public void fetchAlliances(final FetchAlliancesCompleteHandler handler) {
        new BackgroundRunner<List<Alliance>>() {
            @Override
            protected List<Alliance> doInBackground() {
                String url = "alliances";
                try {
                    Alliances pb = ApiClient.getProtoBuf(url, Alliances.class);
                    return new ArrayList<Alliance>(pb.alliances);
                } catch(ApiException e) {
                    return null;
                }
            }

            @Override
            protected void onComplete(List<Alliance> alliances) {
                if (handler != null) {
                    handler.onAlliancesFetched(alliances);
                }
            }
        }.execute();
    }

    /**
     * Fetches details, including memberships, of the alliance with the given key.
     */
    public void fetchAlliance(final int allianceID, final FetchAllianceCompleteHandler handler) {
        new BackgroundRunner<Alliance>() {
            @Override
            protected Alliance doInBackground() {
                String url = "alliances/"+allianceID;
                try {
                    return ApiClient.getProtoBuf(url, Alliance.class);
                } catch(ApiException e) {
                    return null;
                }
            }

            @Override
            protected void onComplete(Alliance alliance) {
                if (handler != null && alliance != null) {
                    handler.onAllianceFetched(alliance);
                }
                fireAllianceUpdated(alliance);
            }
        }.execute();
    }

    public void fetchRequests(final String allianceKey,
                              final FetchRequestsCompleteHandler handler) {
        new BackgroundRunner<List<AllianceRequest>>() {
            private TreeMap<Integer, Empire> mEmpires;

            @Override
            protected List<AllianceRequest> doInBackground() {
                String url = "alliances/"+allianceKey+"/requests";
                try {
                    AllianceRequests pb = ApiClient.getProtoBuf(url, AllianceRequests.class);
                    ArrayList<AllianceRequest> requests = new ArrayList<AllianceRequest>();
                    TreeSet<String> empireKeys = new TreeSet<String>();
                    for (AllianceRequest request : pb.requests) {
                        requests.add(request);
                        if (!empireKeys.contains(Integer.toString(request.request_empire_id))) {
                            empireKeys.add(Integer.toString(request.request_empire_id));
                        }
                        if (request.target_empire_id != null && !empireKeys.contains(Integer.toString(request.target_empire_id))) {
                            empireKeys.add(Integer.toString(request.target_empire_id));
                        }
                    }

                    List<Empire> empires = EmpireManager.i.fetchEmpiresSync(empireKeys);
                    mEmpires = new TreeMap<Integer, Empire>();
                    for (Empire empire : empires) {
                        mEmpires.put(Integer.parseInt(empire.key), empire);
                    }

                    return requests;
                } catch(ApiException e) {
                    return null;
                }
            }

            @Override
            protected void onComplete(List<AllianceRequest> requests) {
                if (handler != null && requests != null) {
                    handler.onRequestsFetched(mEmpires, requests);
                }
            }
        }.execute();
    }

    public void requestJoin(final int allianceID, final String message) {
        final Empire myEmpire = EmpireManager.i.getEmpire();
        AllianceRequest pb = new AllianceRequest.Builder()
                .request_type(AllianceRequest.RequestType.JOIN)
                .alliance_id(allianceID)
                .request_empire_id(Integer.parseInt(myEmpire.key))
                .message(message)
                .build();
        request(pb);
    }

    public void requestDeposit(final int allianceID, final String message, int amount) {
        final Empire myEmpire = EmpireManager.i.getEmpire();
        AllianceRequest pb = new AllianceRequest.Builder()
                .request_type(AllianceRequest.RequestType.DEPOSIT_CASH)
                .amount((float) amount)
                .alliance_id(allianceID)
                .request_empire_id(Integer.parseInt(myEmpire.key))
                .message(message)
                .build();
        request(pb);
    }

    public void requestWithdraw(final int allianceID, final String message, int amount) {
        final Empire myEmpire = EmpireManager.i.getEmpire();
        AllianceRequest pb = new AllianceRequest.Builder()
                .request_type(AllianceRequest.RequestType.WITHDRAW_CASH)
                .amount((float) amount)
                .alliance_id(allianceID)
                .request_empire_id(Integer.parseInt(myEmpire.key))
                .message(message)
                .build();
        request(pb);
    }

    public void requestKick(final int allianceID, final String empireKey, final String message) {
        final Empire myEmpire = EmpireManager.i.getEmpire();
        AllianceRequest pb = new AllianceRequest.Builder()
                .request_type(AllianceRequest.RequestType.KICK)
                .target_empire_id(Integer.parseInt(empireKey))
                .alliance_id(allianceID)
                .request_empire_id(Integer.parseInt(myEmpire.key))
                .message(message)
                .build();
        request(pb);
    }

    public void requestLeave() {
        final Empire myEmpire = EmpireManager.i.getEmpire();
        AllianceRequest pb = new AllianceRequest.Builder()
                .request_type(AllianceRequest.RequestType.LEAVE)
                .alliance_id(Integer.parseInt(myEmpire.alliance.key))
                .request_empire_id(Integer.parseInt(myEmpire.key))
                .message("")
                .build();
        request(pb);
    }

    private void request(final AllianceRequest request) {
        final int allianceID = request.alliance_id;

        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                String url = "alliances/"+allianceID+"/requests";
                try {
                    ApiClient.postProtoBuf(url, request);
                    return true;
                } catch(ApiException e) {
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                if (success) {
                    EmpireManager.i.refreshEmpire();
                    refreshAlliance(allianceID);
                }
            }
        }.execute();
    }

    public void vote(final AllianceRequest request, final boolean approve) {
        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                String url = "alliances/"+request.alliance_id+"/requests/"+request.id;
                AllianceRequestVote.Builder pb = new AllianceRequestVote.Builder()
                        .alliance_id(request.alliance_id)
                        .alliance_request_id(request.id)
                        .votes(approve ? 1 : -1);
                try {
                    ApiClient.postProtoBuf(url, pb.build());
                    return true;
                } catch(ApiException e) {
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                if (success) {
                    refreshAlliance(request.alliance_id);
                }
            }
        }.execute();
    }

    private void refreshAlliance(int allianceID) {
        fetchAlliance(allianceID, null);
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
    public interface FetchRequestsCompleteHandler {
        void onRequestsFetched(Map<Integer, Empire> empires, List<AllianceRequest> requests);
    }
}
