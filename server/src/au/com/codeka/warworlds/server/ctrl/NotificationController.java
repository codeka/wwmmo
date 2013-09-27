package au.com.codeka.warworlds.server.ctrl;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.handlers.NotificationHandler;
import au.com.codeka.warworlds.server.model.ChatConversation;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.common.collect.Lists;

public class NotificationController {
    private final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private static String API_KEY = "AIzaSyADWOC-tWUbzj-SVW13Sz5UuUiGfcmHHDA";

    private static RecentNotificationCache sRecentNotifications = new RecentNotificationCache();
    private static NotificationHandlerCache sHandlers = new NotificationHandlerCache();

    public void sendNotificationToConversation(int conversationID, String name, String value) throws RequestException {
        ChatConversation conversation = new ChatController().getConversation(conversationID);
        ArrayList<Integer> empireIDs = new ArrayList<Integer>();
        if (conversation == null || conversation.getEmpireIDs() == null) {
            return;
        }

        for (Integer empireID : conversation.getEmpireIDs()) {
            // TODO: muted?
            empireIDs.add(empireID);
        }

        Integer[] arr = new Integer[empireIDs.size()];
        empireIDs.toArray(arr);
        sendNotification(arr, null, new Notification(name, value));
    }

    public void sendNotificationToEmpire(int empireID, String name, String value) throws RequestException {
        sendNotification(new Integer[] {empireID}, null, new Notification(name, value));
    }

    public void sendNotificationToOnlineAlliance(int allianceID, String name, String value) throws RequestException {
        TreeMap<String, String> values = new TreeMap<String, String>();
        values.put(name, value);
        sendNotification(null, allianceID, new Notification(name, value));
    }

    public void sendNotificationToAllOnline(String name, String value) throws RequestException {
        TreeMap<String, String> values = new TreeMap<String, String>();
        values.put(name, value);
        sendNotification(null, null, new Notification(name, value));
    }

    public List<Map<String, String>> getRecentNotifications(int empireID) {
        List<Map<String, String>> notifications = new ArrayList<Map<String, String>>();
        for (Notification n : sRecentNotifications.getRecentNotifications(empireID)) {
            if (n.isTooOld()) {
                continue;
            }
            notifications.add(n.values);
        }
        return notifications;
    }

    public void addNotificationHandler(int empireID, NotificationHandler handler) {
        sHandlers.addNotificationHandler(empireID, handler);
    }

    private void sendNotification(Integer[] empireIDs, Integer allianceID, Notification notification) throws RequestException {
        Message.Builder msgBuilder = new Message.Builder();
        for (Map.Entry<String, String> value : notification.values.entrySet()) {
            msgBuilder.addData(value.getKey(), value.getValue());
        }

        Map<String, String> devices = new TreeMap<String, String>();
        String sql;
        if (empireIDs == null && allianceID == null) {
            sql = "SELECT gcm_registration_id, empires.user_email, empires.id AS empire_id" +
                 " FROM devices" +
                 " INNER JOIN empires ON devices.user_email = empires.user_email" +
                 " WHERE online_since > ?" +
                   " AND gcm_registration_id IS NOT NULL";
        } else if (allianceID != null) {
            sql = "SELECT gcm_registration_id, devices.user_email, empires.id AS empire_id" +
                 " FROM devices" +
                 " INNER JOIN empires ON devices.user_email = empires.user_email" +
                 " WHERE online_since > ?" +
                   " AND gcm_registration_id IS NOT NULL" +
                   " AND alliance_id = ?";
        } else {
            sql = "SELECT gcm_registration_id, devices.user_email, empires.id AS empire_id" +
                 " FROM devices" +
                 " INNER JOIN empires ON devices.user_email = empires.user_email" +
                 " WHERE empires.id IN " + BaseDataBase.buildInClause(empireIDs) +
                   " AND gcm_registration_id IS NOT NULL";
        }
        try (SqlStmt stmt = DB.prepare(sql)) {
            if (empireIDs == null && allianceID == null) {
                stmt.setDateTime(1, DateTime.now().minusHours(1));
            } else if (allianceID != null) {
                stmt.setDateTime(1, DateTime.now().minusHours(1));
                stmt.setInt(2, allianceID);
            } else {
                // nothing to do for empireIDs...
            }

            ResultSet rs = stmt.select();
            Set<Integer> doneEmpires = new HashSet<Integer>();
            while (rs.next()) {
                String registrationId = rs.getString(1);
                String email = rs.getString(2);
                int empireID = rs.getInt(3);

                if (!doneEmpires.contains(empireID)) {
                    doneEmpires.add(empireID);

                    if (sHandlers.sendNotification(empireID, notification)) {
                        // if an attached handler handled it, then we're all done!
                        continue;
                    }

                    sRecentNotifications.addNotification(empireID, notification);
                }

                if (registrationId != null && email != null) {
                    devices.put(registrationId, email);
                }
            }
        } catch(Exception e) {
            throw new RequestException(e);
        }

        sendNotification(msgBuilder.build(), devices);
    }

    private void sendNotification(Message msg, Map<String, String> devices) throws RequestException {
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
                        success = false;
                    }
                }
                if (success) {
                    log.info(String.format("Notification successfully sent: user=%s registration=%s",
                             devices.get(registrationId), registrationId));
                }
            }
        } catch (IOException e) {
            throw new RequestException(e);
        }
    }

    private void handleNotRegisteredError(String registrationId, String userEmail, Result result) throws RequestException {
        log.warn(String.format("Could not send notification: DeviceNotRegistered: user=%s registration=%s", userEmail, registrationId));
        String sql = "UPDATE devices SET gcm_registration_id = NULL WHERE gcm_registration_id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setString(1, registrationId);
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    private void handleCanonicalRegistrationId(String registrationId, String userEmail, Result result) throws RequestException {
        log.info(String.format("Notification registration changed: user=%s registration=%s", userEmail, result.getCanonicalRegistrationId()));
        String sql = "UPDATE devices SET gcm_registration_id = ? WHERE gcm_registration_id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setString(1, result.getCanonicalRegistrationId());
            stmt.setString(2, registrationId);
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    /**
     * This class keeps an in-memory cache of "recent" notifications we've generated.
     */
    private static class RecentNotificationCache {
        private Map<Integer, List<Notification>> mCache;
        private DateTime mLastTrimTime;

        // don't keep notifications for more than this
        private static final int MAX_MINUTES = 15;

        public RecentNotificationCache() {
            mCache = new TreeMap<Integer, List<Notification>>();
            mLastTrimTime = DateTime.now();
        }

        public void addNotification(int empireID, Notification notification) {
            synchronized(mCache) {
                List<Notification> notifications = mCache.get(empireID);
                if (notifications == null) {
                    notifications = new ArrayList<Notification>();
                    mCache.put(empireID, notifications);
                }
                notifications.add(notification);
            }

            long timeSinceTrimMillis = DateTime.now().getMillis() - mLastTrimTime.getMillis();
            long timeSinceTrimMinutes = timeSinceTrimMillis / 60000;
            if (timeSinceTrimMinutes > 30) {
                trim();
                mLastTrimTime = DateTime.now();
            }
        }

        public List<Notification> getRecentNotifications(int empireID) {
            synchronized(mCache) {
                List<Notification> notifications = mCache.get(empireID);
                if (notifications != null) {
                    mCache.remove(empireID);
                    return notifications;
                }
             }
            return new ArrayList<Notification>();
        }

        /**
         * Call this every now & them to trim the cache.
         */
        public void trim() {
            synchronized(mCache) {
                List<Integer> empireIDs = Lists.newArrayList(mCache.keySet());
                for (Integer empireID : empireIDs) {
                    List<Notification> notifications = mCache.get(empireID);
                    boolean allTooOld = true;
                    for (Notification n : notifications) {
                        if (!n.isTooOld()) {
                            allTooOld = false;
                            break;
                        }
                    }
                    if (allTooOld) {
                        mCache.remove(empireID);
                    }
                }
            }
        }
    }

    private static class NotificationHandlerCache {
        private HashMap<Integer, List<NotificationHandler>> mHandlers;

        public NotificationHandlerCache() {
            mHandlers = new HashMap<Integer, List<NotificationHandler>>();
        }

        public void addNotificationHandler(int empireID, NotificationHandler handler) {
            synchronized(mHandlers) {
                List<NotificationHandler> handlers = mHandlers.get(empireID);
                if (handlers == null) {
                    handlers = new ArrayList<NotificationHandler>();
                    mHandlers.put(empireID, handlers);
                }
                handlers.add(handler);
            }
        }

        public boolean sendNotification(int empireID, Notification notification) {
            synchronized(mHandlers) {
                List<NotificationHandler> handlers = mHandlers.get(empireID);
                if (handlers != null && handlers.size() > 0) {
                    for (NotificationHandler handler : handlers) {
                        handler.sendNotification(notification);
                    }

                    // once a handler has processed a notification, it's finished and
                    // the client is expected to re-establish it.
                    handlers.clear();
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * A wrapper around the data we need for a notification.
     */
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
