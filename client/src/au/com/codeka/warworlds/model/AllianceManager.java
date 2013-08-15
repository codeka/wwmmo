package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.model.BaseAlliance;
import au.com.codeka.common.protobuf.Messages;
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
                ArrayList<Alliance> alliances;

                String url = "alliances";
                try {
                    Messages.Alliances pb = ApiClient.getProtoBuf(url, Messages.Alliances.class);
                    alliances = new ArrayList<Alliance>();
                    for (Messages.Alliance alliance_pb : pb.getAlliancesList()) {
                        Alliance alliance = new Alliance();
                        alliance.fromProtocolBuffer(alliance_pb);
                        alliances.add(alliance);
                    }
                    return alliances;
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
                    Messages.Alliance pb = ApiClient.getProtoBuf(url, Messages.Alliance.class);
                    Alliance alliance = new Alliance();
                    alliance.fromProtocolBuffer(pb);
                    return alliance;
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
                    Messages.AllianceRequests pb = ApiClient.getProtoBuf(url, Messages.AllianceRequests.class);
                    ArrayList<AllianceRequest> requests = new ArrayList<AllianceRequest>();
                    TreeSet<String> empireKeys = new TreeSet<String>();
                    for (Messages.AllianceRequest request_pb : pb.getRequestsList()) {
                        AllianceRequest request = new AllianceRequest();
                        request.fromProtocolBuffer(request_pb);
                        requests.add(request);
                        if (!empireKeys.contains(Integer.toString(request_pb.getRequestEmpireId()))) {
                            empireKeys.add(Integer.toString(request_pb.getRequestEmpireId()));
                        }
                    }

                    List<Empire> empires = EmpireManager.i.fetchEmpiresSync(empireKeys);
                    mEmpires = new TreeMap<Integer, Empire>();
                    for (Empire empire : empires) {
                        mEmpires.put(Integer.parseInt(empire.getKey()), empire);
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
        final MyEmpire myEmpire = EmpireManager.i.getEmpire();
        Messages.AllianceRequest pb = Messages.AllianceRequest.newBuilder()
                            .setRequestType(Messages.AllianceRequest.RequestType.JOIN)
                            .setAllianceId(allianceID)
                            .setRequestEmpireId(Integer.parseInt(myEmpire.getKey()))
                            .setMessage(message)
                            .build();
        request(pb);
    }

    public void requestDeposit(final int allianceID, final String message, int amount) {
        final MyEmpire myEmpire = EmpireManager.i.getEmpire();
        Messages.AllianceRequest pb = Messages.AllianceRequest.newBuilder()
                            .setRequestType(Messages.AllianceRequest.RequestType.DEPOSIT_CASH)
                            .setAmount(amount)
                            .setAllianceId(allianceID)
                            .setRequestEmpireId(Integer.parseInt(myEmpire.getKey()))
                            .setMessage(message)
                            .build();
        request(pb);
    }

    public void requestWithdraw(final int allianceID, final String message, int amount) {
        final MyEmpire myEmpire = EmpireManager.i.getEmpire();
        Messages.AllianceRequest pb = Messages.AllianceRequest.newBuilder()
                            .setRequestType(Messages.AllianceRequest.RequestType.WITHDRAW_CASH)
                            .setAmount(amount)
                            .setAllianceId(allianceID)
                            .setRequestEmpireId(Integer.parseInt(myEmpire.getKey()))
                            .setMessage(message)
                            .build();
        request(pb);
    }

    private void request(final Messages.AllianceRequest pb) {
        final int allianceID = pb.getAllianceId();

        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                String url = "alliances/"+allianceID+"/requests";
                try {
                    ApiClient.postProtoBuf(url, pb);
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
                String url = "alliances/"+request.getAllianceID()+"/requests/"+request.getID();
                Messages.AllianceRequestVote.Builder pb = Messages.AllianceRequestVote.newBuilder()
                        .setAllianceId(request.getAllianceID())
                        .setAllianceRequestId(request.getID())
                        .setVotes(approve ? 1 : -1);
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
                    refreshAlliance(request.getAllianceID());
                }
            }
        }.execute();
    }

    public void leaveAlliance() {
        final MyEmpire myEmpire = EmpireManager.i.getEmpire();
        if (myEmpire == null) {
            return;
        }
        final BaseAlliance myAlliance = myEmpire.getAlliance();
        if (myAlliance == null) {
            return;
        }

        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                String url = "alliances/"+myAlliance.getKey()+"/requests";
                try {
                    Messages.AllianceRequest pb = Messages.AllianceRequest.newBuilder()
                                                          .setAllianceId(Integer.parseInt(myAlliance.getKey()))
                                                          .setRequestEmpireId(Integer.parseInt(myEmpire.getKey()))
                                                          .setRequestType(Messages.AllianceRequest.RequestType.LEAVE)
                                                          .build();

                    ApiClient.postProtoBuf(url, pb);
                    return true;
                } catch(ApiException e) {
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                if (success) {
                    EmpireManager.i.refreshEmpire();
                    refreshAlliance(((Alliance) myAlliance).getID());
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
