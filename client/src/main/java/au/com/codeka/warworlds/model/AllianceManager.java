package au.com.codeka.warworlds.model;

import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.eventbus.EventBus;

public class AllianceManager {
  public static AllianceManager i = new AllianceManager();

  public static final EventBus eventBus = new EventBus();

  private AllianceManager() {
  }

  /**
   * Fetches all alliances from the server.
   */
  public void fetchAlliances(
      boolean hideDead, @Nonnull final FetchAlliancesCompleteHandler handler) {
    RequestManager.i.sendRequest(new ApiRequest.Builder(
            String.format(Locale.US, "alliances?hide_dead=%d", hideDead ? 1 : 0), "GET")
        .completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            Messages.Alliances pb = request.body(Messages.Alliances.class);
            ArrayList<Alliance> alliances = new ArrayList<>();
            for (Messages.Alliance alliance_pb : pb.getAlliancesList()) {
              Alliance alliance = new Alliance();
              alliance.fromProtocolBuffer(alliance_pb);
              alliances.add(alliance);
            }
            handler.onAlliancesFetched(alliances);
          }
        })
        .build());
  }

  public void fetchWormholes(
      int allianceID,
      int startIndex,
      int count,
      @Nullable String searchQuery,
      @Nonnull final FetchWormholesCompleteHandler handler) {
    String url = String.format(
        Locale.ENGLISH,
        "alliances/%d/wormholes?startIndex=%d&count=%d",
        allianceID,
        startIndex,
        count);
    if (searchQuery != null) {
      url += "&name=" + searchQuery;
    }
    RequestManager.i.sendRequest(new ApiRequest.Builder(url, "GET")
        .completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            Messages.Stars pb = request.body(Messages.Stars.class);
            if (pb == null) {
              // Something went wrong... TODO: report error
              handler.onWormholesFetched(new ArrayList<Star>());
            }
            ArrayList<Star> wormholes = new ArrayList<>();
            for (Messages.Star star_pb : pb.getStarsList()) {
              Star wormhole = new Star();
              wormhole.fromProtocolBuffer(star_pb);
              wormholes.add(wormhole);
            }
            handler.onWormholesFetched(wormholes);
          }
        })
        .build());
  }

  /**
   * Fetches details, including memberships, of the alliance with the given key.
   */
  public void fetchAlliance(int allianceID, @Nullable final FetchAllianceCompleteHandler handler) {
    String url = String.format("alliances/%d", allianceID);
    RequestManager.i.sendRequest(new ApiRequest.Builder(url, "GET")
        .completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            Messages.Alliance pb = request.body(Messages.Alliance.class);
            Alliance alliance = new Alliance();
            alliance.fromProtocolBuffer(pb);
            if (handler != null) {
              handler.onAllianceFetched(alliance);
            }
            eventBus.publish(alliance);
          }
        })
        .build());
  }

  public void fetchRequests(int allianceID, String cursor,
      @Nonnull final FetchRequestsCompleteHandler handler) {
    String url = String.format("alliances/%d/requests", allianceID);
    if (cursor != null) {
      url += "?cursor=" + cursor;
    }
    RequestManager.i.sendRequest(new ApiRequest.Builder(url, "GET")
        .completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            Messages.AllianceRequests pb = request.body(Messages.AllianceRequests.class);
            ArrayList<AllianceRequest> requests = new ArrayList<>();
            TreeMap<Integer, Empire> empires = new TreeMap<>();
            for (Messages.AllianceRequest request_pb : pb.getRequestsList()) {
              AllianceRequest allianceRequest = new AllianceRequest();
              allianceRequest.fromProtocolBuffer(request_pb);
              requests.add(allianceRequest);
              if (!empires.containsKey(request_pb.getRequestEmpireId())) {
                empires.put(request_pb.getRequestEmpireId(),
                    EmpireManager.i.getEmpire(request_pb.getRequestEmpireId()));
              }
              if (request_pb.hasTargetEmpireId()
                  && !empires.containsKey(request_pb.getTargetEmpireId())) {
                empires.put(request_pb.getTargetEmpireId(),
                    EmpireManager.i.getEmpire(request_pb.getRequestEmpireId()));
              }
            }

            handler.onRequestsFetched(empires, requests, pb.getCursor());
          }
        })
        .build());
  }

  public void requestJoin(final int allianceID, final String message) {
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    Messages.AllianceRequest pb = Messages.AllianceRequest.newBuilder()
        .setRequestType(Messages.AllianceRequest.RequestType.JOIN).setAllianceId(allianceID)
        .setRequestEmpireId(Integer.parseInt(myEmpire.getKey())).setMessage(message).build();
    request(pb);
  }

  public void requestDeposit(final int allianceID, final String message, long amount) {
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    Messages.AllianceRequest pb = Messages.AllianceRequest.newBuilder()
        .setRequestType(Messages.AllianceRequest.RequestType.DEPOSIT_CASH).setAmount(amount)
        .setAllianceId(allianceID).setRequestEmpireId(Integer.parseInt(myEmpire.getKey()))
        .setMessage(message).build();
    request(pb);
  }

  public void requestWithdraw(final int allianceID, final String message, long amount) {
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

  public void requestChangeName(
      final int allianceID,
      final String message,
      final String newName) {
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    Messages.AllianceRequest pb = Messages.AllianceRequest.newBuilder()
        .setRequestType(Messages.AllianceRequest.RequestType.CHANGE_NAME)
        .setNewName(newName)
        .setAllianceId(allianceID)
        .setRequestEmpireId(Integer.parseInt(myEmpire.getKey()))
        .setMessage(message).build();
    request(pb);
  }

  public void requestChangeDescription(
      final int allianceID,
      final String message,
      final String newDescription) {
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    Messages.AllianceRequest pb = Messages.AllianceRequest.newBuilder()
        .setRequestType(Messages.AllianceRequest.RequestType.CHANGE_DESCRIPTION)
        .setNewDescription(newDescription)
        .setAllianceId(allianceID)
        .setRequestEmpireId(Integer.parseInt(myEmpire.getKey()))
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
    String url = String.format("alliances/%d/requests", allianceID);
    RequestManager.i.sendRequest(new ApiRequest.Builder(url, "POST").body(pb)
        .completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            EmpireManager.i.refreshEmpire();
            refreshAlliance(allianceID);
          }
        }).build());
  }

  public void vote(final AllianceRequest allianceRequest, final boolean approve) {
    String url = String.format("alliances/%d/requests/%d", allianceRequest.getAllianceID(),
        allianceRequest.getID());
    Messages.AllianceRequestVote.Builder vote = Messages.AllianceRequestVote.newBuilder()
        .setAllianceId(allianceRequest.getAllianceID())
        .setAllianceRequestId(allianceRequest.getID())
        .setVotes(approve ? 1 : -1);

    RequestManager.i.sendRequest(new ApiRequest.Builder(url, "POST")
        .body(vote.build())
        .completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            refreshAlliance(allianceRequest.getAllianceID());
          }
        }).build());
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
