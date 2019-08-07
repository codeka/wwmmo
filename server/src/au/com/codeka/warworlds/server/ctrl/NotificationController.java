package au.com.codeka.warworlds.server.ctrl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.joda.time.DateTime;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseChatConversationParticipant;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.handlers.NotificationHandler;
import au.com.codeka.warworlds.server.model.ChatConversation;
import au.com.codeka.warworlds.server.model.ChatConversationParticipant;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.common.collect.Lists;

public class NotificationController {
  private final Log log = new Log("NotificationController");
  private static String API_KEY = "AIzaSyADWOC-tWUbzj-SVW13Sz5UuUiGfcmHHDA";

  private static RecentNotificationCache recentNotifications = new RecentNotificationCache();
  private static NotificationHandlerCache handlers = new NotificationHandlerCache();

  /** Send a notification to all participants in the given conversation. */
  public void sendNotificationToConversation(int conversationID, String name, String value)
      throws RequestException {
    ChatConversation conversation = new ChatController().getConversation(conversationID);
    if (conversation == null || conversation.getParticipants() == null) {
      return;
    }

    ArrayList<ChatConversationParticipant> participants =
        new ArrayList<ChatConversationParticipant>();
    for (BaseChatConversationParticipant participant : conversation.getParticipants()) {
      participants.add((ChatConversationParticipant) participant);
    }

    ChatConversationParticipant[] arr = new ChatConversationParticipant[participants.size()];
    participants.toArray(arr);
    sendNotification(arr, new Notification(name, value));
  }

  /** Send a notification to the given empire. */
  public void sendNotificationToEmpire(int empireID, String name, String value)
      throws RequestException {
    sendNotification(new ChatConversationParticipant[] { new ChatConversationParticipant(empireID,
        false) }, new Notification(name, value));
  }

  /** Send a notification to the given empire, but only if they're currently online. */
  public void sendNotificationToOnlineEmpire(int empireID, String name, String value)
      throws RequestException {
    Notification notification = new Notification(name, value);
    if (!handlers.sendNotification(empireID, notification)) {
      recentNotifications.addNotification(empireID, notification);
    }
  }

  /** Send a notification to all empires in the given alliance who are currently online. */
  public void sendNotificationToOnlineAlliance(int allianceID, String name, String value)
      throws RequestException {
    Notification notification = new Notification(name, value);
    handlers.sendNotificationToAlliance(allianceID, notification);
  }

  /** Send a notification to all empires that are currently online. */
  public void sendNotificationToAllOnline(String name, String value) throws RequestException {
    TreeMap<String, String> values = new TreeMap<String, String>();
    values.put(name, value);
    Notification notification = new Notification(name, value);

    handlers.sendNotificationToAll(notification);
  }

  /** Gets a list of all the recent notifications for the given empire. */
  public List<Map<String, String>> getRecentNotifications(int empireID) {
    List<Map<String, String>> notifications = new ArrayList<Map<String, String>>();
    for (Notification n : recentNotifications.getRecentNotifications(empireID)) {
      if (n.isTooOld()) {
        continue;
      }
      notifications.add(n.values);
    }
    return notifications;
  }

  /** Add the given {@link NotificationHandler} for the given empire. */
  public void addNotificationHandler(int empireID, NotificationHandler handler) {
    handlers.addNotificationHandler(empireID, handler);
  }

  /** Returns {@code true} if the given empire is currently connected/online. */
  public boolean isEmpireOnline(int empireID) {
    return handlers.isConnected(empireID);
  }

  /** Sends the given {@link Notification} to all the given chat conversation participants. */
  private void sendNotification(ChatConversationParticipant[] participants,
      Notification notification) throws RequestException {
    Message.Builder msgBuilder = new Message.Builder();
    for (Map.Entry<String, String> value : notification.values.entrySet()) {
      msgBuilder.addData(value.getKey(), value.getValue());
    }

    // go through attached handlers and mark any in there as already done.
    Set<Integer> doneEmpires = new HashSet<Integer>();
    for (ChatConversationParticipant participant : participants) {
      if (participant.isMuted()) {
        continue;
      }

      if (!doneEmpires.contains(participant.getEmpireID())) {
        if (handlers.sendNotification(participant.getEmpireID(), notification)) {
          doneEmpires.add(participant.getEmpireID());
        } else {
          recentNotifications.addNotification(participant.getEmpireID(), notification);
        }
      }
    }

    Map<String, String> devices = new TreeMap<String, String>();
    String sql = "SELECT gcm_registration_id, devices.user_email, empires.id AS empire_id"
        + " FROM devices" + " INNER JOIN empires ON devices.user_email = empires.user_email"
        + " WHERE empires.id IN " + BaseDataBase.buildInClause(participants)
        + " AND gcm_registration_id IS NOT NULL";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      while (res.next()) {
        String registrationId = res.getString(1);
        String email = res.getString(2);
        int empireID = res.getInt(3);

        if (doneEmpires.contains(empireID)) {
          continue;
        }

        ChatConversationParticipant participant = null;
        for (ChatConversationParticipant p : participants) {
          if (p.getEmpireID() == empireID) {
            participant = p;
            break;
          }
        }

        if ((participant == null || !participant.isMuted()) && registrationId != null
            && email != null) {
          devices.put(registrationId, email);
        }
      }
    } catch (Exception e) {
      throw new RequestException(e);
    }

    sendNotification(msgBuilder.build(), devices);
  }

  /** Sends the given {@link Message} to the given list of devices. */
  private void sendNotification(Message msg, Map<String, String> devices) throws RequestException {
    // Temporarily disabled as this endpoint is deprecated.
    /*
    Sender sender = new Sender(API_KEY);
    try {
      List<String> registrationIds = new ArrayList<String>();
      for (String registrationId : devices.keySet()) {
        registrationIds.add(registrationId);
      }
      if (registrationIds.size() == 0) {
        return;
      }

      List<Result> results = sender.send(msg, registrationIds, 5).getResults();
      for (int i = 0; i < results.size(); i++) {
        Result result = results.get(i);
        String registrationId = registrationIds.get(i);

        boolean success = true;
        if (result.getMessageId() != null) {
          String canonicalRegistrationId = result.getCanonicalRegistrationId();
          if (canonicalRegistrationId != null) {
            handleCanonicalRegistrationId(registrationId, devices.get(registrationId), result);
            success = false;
          }
        } else {
          String errorCodeName = result.getErrorCodeName();
          if (errorCodeName.equals(Constants.ERROR_NOT_REGISTERED)) {
            handleNotRegisteredError(registrationId, devices.get(registrationId), result);
          } else {
            handleOtherError(registrationId, devices.get(registrationId), result);
          }
          success = false;
        }
        if (success) {
          log.info(String.format("Notification successfully sent: %s user=%s registration=%s",
              Configuration.i.getRealmName(), devices.get(registrationId), registrationId));
        }
      }
    } catch (IOException e) {
      log.error("Error caught sending notification.", e);
    }
    */
  }

  private void handleNotRegisteredError(String registrationId, String userEmail, Result result)
      throws RequestException {
    log.warning("Could not send notification: DeviceNotRegistered: user=%s registration=%s",
        userEmail, registrationId);
    String sql = "UPDATE devices SET gcm_registration_id = NULL WHERE gcm_registration_id = ?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setString(1, registrationId);
      stmt.update();
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }

  private void handleOtherError(String registrationId, String userEmail, Result result)
      throws RequestException {
    log.warning("Could not send notification: %s: user=%s registration=%s",
        result.getErrorCodeName(), userEmail, registrationId);
  }

  private void handleCanonicalRegistrationId(String registrationId, String userEmail, Result result)
      throws RequestException {
    log.info("Notification registration changed: user=%s registration=%s", userEmail,
        result.getCanonicalRegistrationId());
    String sql = "UPDATE devices SET gcm_registration_id = ? WHERE gcm_registration_id = ?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setString(1, result.getCanonicalRegistrationId());
      stmt.setString(2, registrationId);
      stmt.update();
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }

  /**
   * This class keeps an in-memory cache of "recent" notifications we've generated, which is used
   * to re-send notification if the client disconnects briefly.
   */
  private static class RecentNotificationCache {
    private Map<Integer, List<Notification>> cache;
    private DateTime lastTrimTime;

    // don't keep notifications for more than this
    private static final int MAX_MINUTES = 15;

    public RecentNotificationCache() {
      cache = new TreeMap<Integer, List<Notification>>();
      lastTrimTime = DateTime.now();
    }

    public void addNotification(int empireID, Notification notification) {
      synchronized (cache) {
        List<Notification> notifications = cache.get(empireID);
        if (notifications == null) {
          notifications = new ArrayList<Notification>();
          cache.put(empireID, notifications);
        }
        notifications.add(notification);
      }

      long timeSinceTrimMillis = DateTime.now().getMillis() - lastTrimTime.getMillis();
      long timeSinceTrimMinutes = timeSinceTrimMillis / 60000;
      if (timeSinceTrimMinutes > 30) {
        trim();
        lastTrimTime = DateTime.now();
      }
    }

    public List<Notification> getRecentNotifications(int empireID) {
      synchronized (cache) {
        List<Notification> notifications = cache.get(empireID);
        if (notifications != null) {
          cache.remove(empireID);
          return notifications;
        }
      }
      return new ArrayList<Notification>();
    }

    /** Call this every now & them to trim the cache. */
    public void trim() {
      synchronized (cache) {
        List<Integer> empireIDs = Lists.newArrayList(cache.keySet());
        for (Integer empireID : empireIDs) {
          List<Notification> notifications = cache.get(empireID);
          boolean allTooOld = true;
          for (Notification n : notifications) {
            if (!n.isTooOld()) {
              allTooOld = false;
              break;
            }
          }
          if (allTooOld) {
            cache.remove(empireID);
          }
        }
      }
    }
  }

  /** Holds the collection of {@link NotficationHandler} instances for all connected empires. */
  private static class NotificationHandlerCache {
    private HashMap<Integer, List<NotificationHandler>> handlers;

    public NotificationHandlerCache() {
      handlers = new HashMap<Integer, List<NotificationHandler>>();
    }

    /** Returns {@code true} if the given empire is currently connected. */
    public boolean isConnected(int empireID) {
      List<NotificationHandler> empireHandlers = handlers.get(empireID);
      if (empireHandlers == null) {
        return false;
      }

      if (empireHandlers.isEmpty()) {
        return false;
      }

      return true;
    }

    public void addNotificationHandler(int empireID, NotificationHandler handler) {
      synchronized (handlers) {
        List<NotificationHandler> empireHandlers = handlers.get(empireID);
        if (empireHandlers == null) {
          empireHandlers = new ArrayList<NotificationHandler>();
          handlers.put(empireID, empireHandlers);
        }
        empireHandlers.add(handler);
      }
    }

    public boolean sendNotification(int empireID, Notification notification) {
      synchronized (handlers) {
        List<NotificationHandler> empireHandlers = handlers.get(empireID);
        if (empireHandlers != null && empireHandlers.size() > 0) {
          for (NotificationHandler handler : empireHandlers) {
            handler.sendNotification(notification);
          }

          // once a handler has processed a notification, it's finished and
          // the client is expected to re-establish it.
          empireHandlers.clear();
          return true;
        }
      }

      return false;
    }

    /* Sends the given notification to all attached handlers at once. */
    public void sendNotificationToAll(Notification notification) {
      synchronized (handlers) {
        for (List<NotificationHandler> empireHandlers : handlers.values()) {
          if (empireHandlers != null && empireHandlers.size() > 0) {
            for (NotificationHandler handler : empireHandlers) {
              handler.sendNotification(notification);
            }

            // once a handler has processed a notification, it's finished and
            // the client is expected to re-establish it.
            empireHandlers.clear();
          }
        }
      }
    }

    /*
     * Sends the given notification to all attached handlers at once, as long as
     * they match the given alliance.
     */
    public void sendNotificationToAlliance(int allianceID, Notification notification) {
      synchronized (handlers) {
        for (List<NotificationHandler> empireHandlers : handlers.values()) {
          if (empireHandlers != null && empireHandlers.size() > 0) {
            boolean sent = false;
            for (NotificationHandler handler : empireHandlers) {
              if (handler.getAllianceID() != allianceID) {
                continue;
              }
              handler.sendNotification(notification);
              sent = true;
            }

            // once a handler has processed a notification, it's finished and
            // the client is expected to re-establish it.
            if (sent) {
              empireHandlers.clear();
            }
          }
        }
      }
    }
  }

  /** A wrapper around the data we need for a notification. */
  public static class Notification {
    public DateTime creation;
    public Map<String, String> values;

    public Notification(String name, String value) {
      values = new TreeMap<String, String>();
      values.put(name, value);
      creation = DateTime.now();
    }

    public boolean isTooOld() {
      long diffInMillis = DateTime.now().getMillis() - creation.getMillis();
      long diffInMinutes = diffInMillis / 60000;
      if (diffInMinutes > RecentNotificationCache.MAX_MINUTES) {
        return true;
      }
      return false;
    }
  }
}
