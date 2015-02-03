package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.protobuf.ByteString;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.eventbus.EventBus;

public class AllianceManager {
  public static AllianceManager i = new AllianceManager();

  public static final EventBus eventBus = new EventBus();

  private AllianceManager() {
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
        } catch (ApiException e) {
          return null;
        }
      }

      @Override
      protected void onComplete(List<Alliance> alliances) {
        if (handler != null && alliances != null) {
          handler.onAlliancesFetched(alliances);
        }
      }
    }.execute();
  }

  public void fetchWormholes(final int allianceID, final FetchWormholesCompleteHandler handler) {
    new BackgroundRunner<List<Star>>() {
      @Override
      protected List<Star> doInBackground() {
        ArrayList<Star> wormholes;

        String url = "alliances/" + allianceID + "/wormholes";
        try {
          Messages.Stars pb = ApiClient.getProtoBuf(url, Messages.Stars.class);
          wormholes = new ArrayList<Star>();
          for (Messages.Star star_pb : pb.getStarsList()) {
            Star wormhole = new Star();
            wormhole.fromProtocolBuffer(star_pb);
            wormholes.add(wormhole);
          }
          return wormholes;
        } catch (ApiException e) {
          return null;
        }
      }

      @Override
      protected void onComplete(List<Star> wormholes) {
        if (handler != null && wormholes != null) {
          handler.onWormholesFetched(wormholes);
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
        String url = "alliances/" + allianceID;
        try {
          Messages.Alliance pb = ApiClient.getProtoBuf(url, Messages.Alliance.class);
          Alliance alliance = new Alliance();
          alliance.fromProtocolBuffer(pb);
          return alliance;
        } catch (ApiException e) {
          return null;
        }
      }

      @Override
      protected void onComplete(Alliance alliance) {
        if (handler != null && alliance != null) {
          handler.onAllianceFetched(alliance);
        }
        eventBus.publish(alliance);
      }
    }.execute();
  }

  public void fetchRequests(final String allianceKey, final String cursor,
      final FetchRequestsCompleteHandler handler) {
    new BackgroundRunner<List<AllianceRequest>>() {
      private TreeMap<Integer, Empire> mEmpires;
      private String mCursor;

      @Override
      protected List<AllianceRequest> doInBackground() {
        String url = "alliances/" + allianceKey + "/requests";
        if (cursor != null) {
          url += "?cursor=" + cursor;
        }
        try {
          Messages.AllianceRequests pb =
              ApiClient.getProtoBuf(url, Messages.AllianceRequests.class);
          if (pb == null) {
            return null;
          }
          ArrayList<AllianceRequest> requests = new ArrayList<AllianceRequest>();
          TreeSet<Integer> empireIDs = new TreeSet<Integer>();
          for (Messages.AllianceRequest request_pb : pb.getRequestsList()) {
            AllianceRequest request = new AllianceRequest();
            request.fromProtocolBuffer(request_pb);
            requests.add(request);
            if (!empireIDs.contains(request_pb.getRequestEmpireId())) {
              empireIDs.add(request_pb.getRequestEmpireId());
            }
            if (request_pb.hasTargetEmpireId() && !empireIDs
                .contains(request_pb.getTargetEmpireId())) {
              empireIDs.add(request_pb.getTargetEmpireId());
            }
          }
          mCursor = pb.getCursor();
          if (mCursor != null && mCursor.isEmpty()) {
            mCursor = null;
          }

          List<Empire> empires = EmpireManager.i.refreshEmpiresSync(empireIDs);
          mEmpires = new TreeMap<Integer, Empire>();
          for (Empire empire : empires) {
            mEmpires.put(Integer.parseInt(empire.getKey()), empire);
          }

          return requests;
        } catch (ApiException e) {
          return null;
        }
      }

      @Override
      protected void onComplete(List<AllianceRequest> requests) {
        if (handler != null && requests != null) {
          handler.onRequestsFetched(mEmpires, requests, mCursor);
        }
      }
    }.execute();
  }

  public void requestJoin(final int allianceID, final String message) {
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    Messages.AllianceRequest pb = Messages.AllianceRequest.newBuilder()
        .setRequestType(Messages.AllianceRequest.RequestType.JOIN).setAllianceId(allianceID)
        .setRequestEmpireId(Integer.parseInt(myEmpire.getKey())).setMessage(message).build();
    request(pb);
  }

  public void requestDeposit(final int allianceID, final String message, int amount) {
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    Messages.AllianceRequest pb = Messages.AllianceRequest.newBuilder()
        .setRequestType(Messages.AllianceRequest.RequestType.DEPOSIT_CASH).setAmount(amount)
        .setAllianceId(allianceID).setRequestEmpireId(Integer.parseInt(myEmpire.getKey()))
        .setMessage(message).build();
    request(pb);
  }

  public void requestWithdraw(final int allianceID, final String message, int amount) {
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    Messages.AllianceRequest pb = Messages.AllianceRequest.newBuilder()
        .setRequestType(Messages.AllianceRequest.RequestType.WITHDRAW_CASH).setAmount(amount)
        .setAllianceId(allianceID).setRequestEmpireId(Integer.parseInt(myEmpire.getKey()))
        .setMessage(message).build();
    request(pb);
  }

  public void requestKick(final int allianceID, final String empireKey, final String message) {
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    Messages.AllianceRequest pb = Messages.AllianceRequest.newBuilder()
        .setRequestType(Messages.AllianceRequest.RequestType.KICK)
        .setTargetEmpireId(Integer.parseInt(empireKey)).setAllianceId(allianceID)
        .setRequestEmpireId(Integer.parseInt(myEmpire.getKey())).setMessage(message).build();
    request(pb);
  }

  public void requestLeave() {
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    Messages.AllianceRequest pb = Messages.AllianceRequest.newBuilder()
        .setRequestType(Messages.AllianceRequest.RequestType.LEAVE)
        .setAllianceId(Integer.parseInt(myEmpire.getAlliance().getKey()))
        .setRequestEmpireId(Integer.parseInt(myEmpire.getKey())).setMessage("").build();
    request(pb);
  }

  public void requestChangeName(final int allianceID, final String message, final String newName) {
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    Messages.AllianceRequest pb = Messages.AllianceRequest.newBuilder()
        .setRequestType(Messages.AllianceRequest.RequestType.CHANGE_NAME).setNewName(newName)
        .setAllianceId(allianceID).setRequestEmpireId(Integer.parseInt(myEmpire.getKey()))
        .setMessage(message).build();
    request(pb);
  }

  public void requestChangeImage(final int allianceID, final String message,
      final byte[] pngImage) {
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    Messages.AllianceRequest pb = Messages.AllianceRequest.newBuilder()
        .setRequestType(Messages.AllianceRequest.RequestType.CHANGE_IMAGE)
        .setPngImage(ByteString.copyFrom(pngImage)).setAllianceId(allianceID)
        .setRequestEmpireId(Integer.parseInt(myEmpire.getKey())).setMessage(message).build();
    request(pb);
  }

  private void request(final Messages.AllianceRequest pb) {
    final int allianceID = pb.getAllianceId();

    new BackgroundRunner<Boolean>() {
      @Override
      protected Boolean doInBackground() {
        String url = "alliances/" + allianceID + "/requests";
        try {
          ApiClient.postProtoBuf(url, pb);
          return true;
        } catch (ApiException e) {
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
        String url = "alliances/" + request.getAllianceID() + "/requests/" + request.getID();
        Messages.AllianceRequestVote.Builder pb =
            Messages.AllianceRequestVote.newBuilder().setAllianceId(request.getAllianceID())
                .setAllianceRequestId(request.getID()).setVotes(approve ? 1 : -1);
        try {
          ApiClient.postProtoBuf(url, pb.build());
          return true;
        } catch (ApiException e) {
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

  private void refreshAlliance(int allianceID) {
    fetchAlliance(allianceID, null);
  }

  public interface FetchAlliancesCompleteHandler {
    void onAlliancesFetched(List<Alliance> alliances);
  }

  public interface FetchAllianceCompleteHandler {
    void onAllianceFetched(Alliance alliance);
  }

  public interface FetchRequestsCompleteHandler {
    void onRequestsFetched(Map<Integer, Empire> empires, List<AllianceRequest> requests,
        String cursor);
  }

  public interface FetchWormholesCompleteHandler {
    void onWormholesFetched(List<Star> wormholes);
  }
}
