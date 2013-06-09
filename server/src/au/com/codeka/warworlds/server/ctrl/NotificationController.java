package au.com.codeka.warworlds.server.ctrl;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

public class NotificationController {
    private final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private static String API_KEY = "AIzaSyADWOC-tWUbzj-SVW13Sz5UuUiGfcmHHDA";

    public void sendNotificationToEmpire(int empireID, String name, String value) throws RequestException {
        TreeMap<String, String> values = new TreeMap<String, String>();
        values.put(name, value);
        sendNotification(empireID, null, values);
    }

    public void sendNotificationToOnlineAlliance(int allianceID, String name, String value) throws RequestException {
        TreeMap<String, String> values = new TreeMap<String, String>();
        values.put(name, value);
        sendNotification(null, allianceID, values);
    }

    public void sendNotificationToAllOnline(String name, String value) throws RequestException {
        TreeMap<String, String> values = new TreeMap<String, String>();
        values.put(name, value);
        sendNotification(null, null, values);
    }

    private void sendNotification(Integer empireID, Integer allianceID, Map<String, String> values) throws RequestException {
        Message.Builder msgBuilder = new Message.Builder();
        for (Map.Entry<String, String> value : values.entrySet()) {
            msgBuilder.addData(value.getKey(), value.getValue());
        }

        Map<String, String> devices = new TreeMap<String, String>();
        String sql;
        if (empireID == null && allianceID == null) {
            sql = "SELECT gcm_registration_id, user_email FROM devices WHERE online_since > ? AND gcm_registration_id IS NOT NULL";
        } else if (allianceID != null) {
            sql = "SELECT gcm_registration_id, devices.user_email" +
                 " FROM devices" +
                 " INNER JOIN empires ON devices.user_email = empires.user_email" +
                 " WHERE online_since > ?" +
                   " AND alliance_id = ?";
        } else {
            sql = "SELECT gcm_registration_id, devices.user_email" +
                 " FROM devices" +
                 " INNER JOIN empires ON devices.user_email = empires.user_email" +
                 " WHERE empires.id = ?";
        }
        try (SqlStmt stmt = DB.prepare(sql)) {
            if (empireID == null && allianceID == null) {
                stmt.setDateTime(1, DateTime.now().minusHours(1));
            } else if (allianceID != null) {
                stmt.setDateTime(1, DateTime.now().minusHours(1));
                stmt.setInt(2, allianceID);
            } else {
                stmt.setInt(1, empireID);
            }

            ResultSet rs = stmt.select();
            while (rs.next()) {
                String registrationId = rs.getString(1);
                String email = rs.getString(2);
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
}
