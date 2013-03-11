
package au.com.codeka.warworlds;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.protobuf.Messages;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Receive a push message from the Cloud to Device Messaging (C2DM) service.
 */
public class GCMIntentService extends GCMBaseIntentService {
    private static Logger log = LoggerFactory.getLogger(GCMIntentService.class);

    public static String PROJECT_ID = "990931198580";

    public GCMIntentService() {
        super(PROJECT_ID);
    }

    /**
     * Registers for C2DM notifications. Calls
     * AccountsActivity.registrationComplete() when finished.
     */
    public static void register(Activity activity) {
        GCMRegistrar.register(activity, PROJECT_ID);
    }

    /**
     * Unregisters ourselves from C2DM notifications.
     */
    public static void unregister(Activity activity) {
        GCMRegistrar.unregister(activity);
    }

    /**
     * Called when a registration token has been received.
     * 
     * @param context
     *            the Context
     * @param registrationId
     *            the registration id as a String
     * @throws IOException
     *             if registration cannot be performed
     */
    @Override
    public void onRegistered(Context context, String gcmRegistrationID) {
        log.info("GCM device registration complete, gcmRegistrationID = "+gcmRegistrationID);
        DeviceRegistrar.updateGcmRegistration(context, gcmRegistrationID);
    }

    /**
     * Called when the device has been unregistered.
     * 
     * @param context
     *            the Context
     */
    @Override
    public void onUnregistered(Context context, String deviceRegistrationID) {
        log.info("Unregistered from GCM, deviceRegistrationID = "+deviceRegistrationID);
        DeviceRegistrar.unregister(context);
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
        Util.loadProperties(context);
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

                // refresh the star this situation report is for, obviously
                // something happened that we'll want to know about
                if (!StarManager.getInstance().refreshStar(
                            context,
                            pb.getStarKey(),
                            true)) { // <-- only refresh the star if we have one cached
                    // if we didn't refresh the star, then at least refresh
                    // the sector it was in (could have been a moving
                    // fleet, say)
                    Star star = SectorManager.getInstance().findStar(pb.getStarKey());
                    if (star != null) {
                        SectorManager.getInstance().refreshSector(star.getSectorX(), star.getSectorY());
                    }
                    // refresh the empire as well, since stuff has happened...
                    MyEmpire myEmpire = EmpireManager.getInstance().getEmpire();
                    if (myEmpire != null) {
                        myEmpire.setDirty();
                        myEmpire.refreshAllDetails(null);
                    }
                }

                Notifications.displayNotification(context, pb);
            } else if (extras.containsKey("chat")) {
                byte[] blob = Base64.decode(extras.getString("chat"), Base64.DEFAULT);

                ChatMessage msg;
                try {
                    Messages.ChatMessage pb = Messages.ChatMessage.parseFrom(blob);
                    msg = ChatMessage.fromProtocolBuffer(pb);
                } catch(InvalidProtocolBufferException e) {
                    log.error("Could not parse chat message!", e);
                    return;
                }

                // don't add our own chats, since they'll have been added automatically
                MyEmpire myEmpire = EmpireManager.getInstance().getEmpire();
                if (myEmpire == null) {
                    return;
                }
                if (msg.getEmpireKey() == null || msg.getEmpireKey().equals(myEmpire.getKey())) {
                    return;
                }

                ChatManager.getInstance().addMessage(context, msg);
            } else if (extras.containsKey("empire_updated")) {
                EmpireManager.getInstance().refreshEmpire();
            }
        }
    }
}
