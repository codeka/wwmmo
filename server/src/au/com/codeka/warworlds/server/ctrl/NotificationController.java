package au.com.codeka.warworlds.server.ctrl;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.joda.time.DateTime;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

public class NotificationController {

    private static String API_KEY = "AIzaSyADWOC-tWUbzj-SVW13Sz5UuUiGfcmHHDA";

    public void sendNotification(String name, String value) throws RequestException {
        TreeMap<String, String> values = new TreeMap<String, String>();
        values.put(name, value);
        sendNotification(values);
    }

    /**
     * Sends the given \c JSONObject to all connected devices.
     */
    public void sendNotification(Map<String, String> values) throws RequestException {
        Message.Builder msgBuilder = new Message.Builder();
        for (Map.Entry<String, String> value : values.entrySet()) {
            msgBuilder.addData(value.getKey(), value.getValue());
        }

        List<String> devices = new ArrayList<String>();
        String sql = "SELECT gcm_registration_id FROM devices WHERE online_since > ? AND gcm_registration_id IS NOT NULL";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, DateTime.now().minusHours(1));
            ResultSet rs = stmt.select();
            while (rs.next()) {
                devices.add(rs.getString(1));
            }
        } catch(Exception e) {
            throw new RequestException(500, e);
        }

        sendNotification(msgBuilder.build(), devices);
    }

    private void sendNotification(Message msg, List<String> registrationIds) throws RequestException {
        Sender sender = new Sender(API_KEY);
        try {
            List<Result> results = sender.send(msg, registrationIds, 5).getResults();
            for (int i = 0; i < results.size(); i++) {
                Result result = results.get(i);
                String registrationId = registrationIds.get(i);

                if (result.getMessageId() != null) {
                    String canonicalRegistrationId = result.getCanonicalRegistrationId();
                    if (canonicalRegistrationId != null) {
                        handleCanonicalRegistrationId(registrationId, result);
                    }
                } else {
                    String errorCodeName = result.getErrorCodeName();
                    if (errorCodeName.equals(Constants.ERROR_NOT_REGISTERED)) {
                        handleNotRegisteredError(registrationId, result);
                    }
                }
            }
        } catch (IOException e) {
            throw new RequestException(500, e);
        }
    }

    private void handleNotRegisteredError(String registrationId, Result result) throws RequestException {
        String sql = "UPDATE devices SET gcm_registration_id = NULL WHERE gcm_registration_id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setString(1, registrationId);
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(500, e);
        }
    }

    private void handleCanonicalRegistrationId(String registrationId, Result result) throws RequestException {
        String sql = "UPDATE devices SET gcm_registration_id = ? WHERE gcm_registration_id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setString(1, result.getCanonicalRegistrationId());
            stmt.setString(2, registrationId);
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(500, e);
        }
    }
}
