package au.com.codeka.warworlds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Realm;
import au.com.codeka.warworlds.model.RealmManager;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Receive a push message from the Google Cloud Messaging.
 */
public class GCMIntentService extends GCMBaseIntentService {
    private static Logger log = LoggerFactory.getLogger(GCMIntentService.class);

    public static String PROJECT_ID = "990931198580";

    public GCMIntentService() {
        super(PROJECT_ID);
    }

    public static void register(Activity activity) {
        GCMRegistrar.register(activity, PROJECT_ID);
    }

    public static void unregister(Activity activity) {
        GCMRegistrar.unregister(activity);
    }

    @Override
    public void onRegistered(Context context, String gcmRegistrationID) {
        log.info("GCM device registration complete, gcmRegistrationID = "+gcmRegistrationID);
        DeviceRegistrar.updateGcmRegistration(context, gcmRegistrationID);
    }

    @Override
    public void onUnregistered(Context context, String deviceRegistrationID) {
        log.info("Unregistered from GCM, deviceRegistrationID = "+deviceRegistrationID);
        DeviceRegistrar.unregister(false);
    }

    /**
     * Called where there's a non-recoverable error.
     */
    @Override
    public void onError(Context context, String errorId) {
        log.error("An error has occured! Error={}", errorId);
    }

    /**
     * Called when there's a \i recoverable error.
     */
    @Override
    public boolean onRecoverableError(Context context, String errorId) {
        log.error("A recoverable error has occured, trying again. Error={}", errorId);
        return true;
    }

    /**
     * Called when a cloud message has been received.
     */
    @Override
    public void onMessage(Context context, Intent intent) {
        // since this can be called when the application is not running, make sure we're
        // set to go still.
        Util.loadProperties();
        Util.setup(context);

        log.debug("GCM message received.");
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for(String key : extras.keySet()) {
                log.debug(String.format("%s = %s", key, extras.get(key)));
            }
            if (extras.containsKey("sitrep")) {
                byte[] blob = Base64.decode(extras.getString("sitrep"), Base64.DEFAULT);

                Messages.SituationReport pb;
                try {
                    pb = Messages.SituationReport.parseFrom(blob);
                } catch (InvalidProtocolBufferException e) {
                    log.error("Could not parse situation report!", e);
                    return;
                }

                // we could currently be in a game, and that game could be running in a different
                // realm to this notification. So we switch this thread temporarily to whatever
                // realm this notification is for.
                Realm thisRealm = RealmManager.i.getRealmByName(pb.getRealm());
                RealmContext.i.setThreadRealm(thisRealm);
                try {
                    // refresh the star this situation report is for, obviously
                    // something happened that we'll want to know about
                    Star star = StarManager.getInstance().refreshStarSync(pb.getStarKey(), true);
                    if (star == null) { // <-- only refresh the star if we have one cached
                        // if we didn't refresh the star, then at least refresh
                        // the sector it was in (could have been a moving
                        // fleet, say)
                        star = SectorManager.getInstance().findStar(pb.getStarKey());
                        if (star != null) {
                            SectorManager.getInstance().refreshSector(star.getSectorX(), star.getSectorY());
                        }
                    } else {
                        StarManager.getInstance().fireStarUpdated(star);
                    }

                    // notify the build manager, in case it's a 'build complete' or something
                    BuildManager.getInstance().notifySituationReport(pb);

                    Notifications.displayNotification(context, pb);
                } finally {
                    RealmContext.i.setThreadRealm(null);
                }
            } else if (extras.containsKey("chat")) {
                byte[] blob = Base64.decode(extras.getString("chat"), Base64.DEFAULT);

                ChatMessage msg;
                try {
                    Messages.ChatMessage pb = Messages.ChatMessage.parseFrom(blob);
                    msg = new ChatMessage();
                    msg.fromProtocolBuffer(pb);
                } catch(InvalidProtocolBufferException e) {
                    log.error("Could not parse chat message!", e);
                    return;
                }

                // don't add our own chats, since they'll have been added automatically
                MyEmpire myEmpire = EmpireManager.i.getEmpire();
                if (myEmpire == null) {
                    return;
                }
                if (msg.getEmpireKey() == null || msg.getEmpireKey().equals(myEmpire.getKey())) {
                    return;
                }

                ChatManager.getInstance().addMessage(msg);
            } else if (extras.containsKey("empire_updated")) {
                EmpireManager.i.refreshEmpire();
            }
        }
    }
}
