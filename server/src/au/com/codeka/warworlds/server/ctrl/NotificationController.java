package au.com.codeka.warworlds.server.ctrl;

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
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.handlers.NotificationHandler;
import au.com.codeka.warworlds.server.model.ChatConversation;
import au.com.codeka.warworlds.server.model.ChatConversationParticipant;

import com.google.common.collect.Lists;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;

import javax.annotation.Nullable;

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
        new ArrayList<>();
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
  public void sendNotificationToOnlineEmpire(int empireID, String name, String value) {
    Notification notification = new Notification(name, value);
    if (!handlers.sendNotification(empireID, notification)) {
      recentNotifications.addNotification(empireID, notification);
    }
  }

  /** Send a notification to all empires in the given alliance who are currently online. */
  public void sendNotificationToOnlineAlliance(
      int allianceID, String name, String value, @Nullable Set<Integer> exclusions) {
    Notification notification = new Notification(name, value);
    handlers.sendNotificationToAlliance(allianceID, notification, exclusions);
  }

  /** Send a notification to all empires that are currently online. */
  public void sendNotificationToAllOnline(
      String name, String value, @Nullable Set<Integer> exclusions) {
    TreeMap<String, String> values = new TreeMap<>();
    values.put(name, value);
    Notification notification = new Notification(name, value);

    handlers.sendNotificationToAll(notification, exclusions);
  }

  /** Gets a list of all the recent notifications for the given empire. */
  public List<Map<String, String>> getRecentNotifications(int empireID) {
    List<Map<String, String>> notifications = new ArrayList<>();
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

  /**
   * Gets the FCM token(s) for the given empire. There can be multiple, if they have more than
   * one registered device.
   */
  public ArrayList<String> getFcmTokensForEmpire(int empireID) throws RequestException {
    ArrayList<String> tokens = new ArrayList<>();

    String sql = "SELECT fcm_token"
        + " FROM devices"
        + " INNER JOIN empires ON devices.user_email = empires.user_email"
        + " WHERE empires.id = " + empireID
        + " AND fcm_token IS NOT NULL";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      while (res.next()) {
        tokens.add(res.getString(1));
      }
    } catch (Exception e) {
      throw new RequestException(e);
    }

    return tokens;
  }

  /** Sends the given {@link Notification} to all the given chat conversation participants. */
  private void sendNotification(ChatConversationParticipant[] participants,
      Notification notification) throws RequestException {
    HashMap<String, String> msgData = new HashMap<>();
    for (Map.Entry<String, String> value : notification.values.entrySet()) {
      msgData.put(value.getKey(), value.getValue());
    }

    // go through attached handlers and mark any in there as already done.
    Set<Integer> doneEmpires = new HashSet<>();
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

    Map<String, String> devices = new TreeMap<>();
    String sql = "SELECT fcm_token, devices.user_email, empires.id AS empire_id"
        + " FROM devices" + " INNER JOIN empires ON devices.user_email = empires.user_email"
        + " WHERE empires.id IN " + BaseDataBase.buildInClause(participants)
        + " AND fcm_token IS NOT NULL";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      while (res.next()) {
        String fcmToken = res.getString(1);
        String email = res.getString(2);
        int empireID = res.getInt(3);

        if (doneEmpires.contains(empireID)) {
          log.info("done empires, not sending: %d", empireID);
          continue;
        }

        ChatConversationParticipant participant = null;
        for (ChatConversationParticipant p : participants) {
          if (p.getEmpireID() == empireID) {
            participant = p;
            break;
          }
        }

        log.info("participant=%s fcmToken=%s email=%s", participant, fcmToken, email);
        if ((participant == null || !participant.isMuted()) && fcmToken != null && email != null) {
          devices.put(fcmToken, email);
        }
      }
    } catch (Exception e) {
      throw new RequestException(e);
    }

    if (!devices.isEmpty()) {
      sendNotification(msgData, devices);
    }
  }

  /** Sends the given "msg" to the given list of devices. */
  private void sendNotification(HashMap<String, String> data, Map<String, String> devices) {
    ArrayList<Message> messages = new ArrayList<>();

    for (Map.Entry<String, String> entry : devices.entrySet()) {
      String fcmToken = entry.getKey();

      Message msg = Message.builder()
          .putAllData(data)
          .setToken(fcmToken)
          .build();
      messages.add(msg);
    }

    // TODO: multicast?
    FirebaseMessaging.getInstance().sendAllAsync(messages);
    // TODO: check status?
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
      handlers = new HashMap<>();
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
          empireHandlers = new ArrayList<>();
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
    public void sendNotificationToAll(
        Notification notification, @Nullable Set<Integer> exclusions) {
      synchronized (handlers) {
        for (List<NotificationHandler> empireHandlers : handlers.values()) {
          if (empireHandlers != null && empireHandlers.size() > 0) {
            for (NotificationHandler handler : empireHandlers) {
              if (exclusions == null || !exclusions.contains(handler.getEmpireID())) {
                handler.sendNotification(notification);
              }
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
    public void sendNotificationToAlliance(
        int allianceID, Notification notification, @Nullable Set<Integer> exclusions) {
      synchronized (handlers) {
        for (List<NotificationHandler> empireHandlers : handlers.values()) {
          if (empireHandlers != null && empireHandlers.size() > 0) {
            boolean sent = false;
            for (NotificationHandler handler : empireHandlers) {
              if (handler.getAllianceID() != allianceID) {
                continue;
              }
              if (exclusions != null && exclusions.contains(handler.getEmpireID())) {
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
      values = new TreeMap<>();
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
