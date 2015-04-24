package au.com.codeka.warworlds.server.handlers;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

import java.util.ArrayList;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Notification;
import au.com.codeka.common.protobuf.Notifications;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.NotificationController;

/**
 * This is a special handler that makes use of Jetty continuations to implement long-polling. It
 * works in conjunction with {@see NotificationController} to send notifications to connected
 * clients.
 */
public class NotificationHandler extends RequestHandler {
    private static Log log = new Log("NotificationHandler");
    private Continuation mContinuation;
    private int mEmpireID;
    private int mAllianceID;

    @Override
    public void get() throws RequestException {
        if (getSessionNoError() != null) {
            mEmpireID = getSession().getEmpireID();
            mAllianceID = getSession().getAllianceID();
        }

        mContinuation = ContinuationSupport.getContinuation(getRequest());
        NotificationController.Notification notification = (NotificationController.Notification) mContinuation.getAttribute("notification");
        if (notification != null) {
            // if we get a notification message...

            Notifications.Builder notifications_pb = new Notifications.Builder();
            notifications_pb.notifications = new ArrayList<>();
            for (String key : notification.values.keySet()) {
                log.info("Adding notification: "+key);
                notifications_pb.notifications.add(
                    new Notification.Builder().name(key).value(notification.values.get(key))
                        .build());
            }
            setResponseBody(notifications_pb.build());
            return;
        }

        if (mContinuation.isInitial()) {
            // initial state, set a timeout and wait for a notification
            mContinuation.setTimeout(20000);
            mContinuation.suspend();
            new NotificationController().addNotificationHandler(getSession().getEmpireID(), this);
        } else {
            // if we get here, it's because the continuation timed out, this
            // will just cause an empty response to be sent
            return;
        }
    }

    public int getEmpireID() {
        return mEmpireID;
    }
    public int getAllianceID() {
        return mAllianceID;
    }

    /**
     * This is called by the notification controller when a notification is received.
     */
    public void sendNotification(NotificationController.Notification notification) {
        if (mContinuation == null || !mContinuation.isSuspended()) {
            return;
        }
        log.info("Sending notification via NotificationHandler for "+mEmpireID);
        try {
            mContinuation.setAttribute("notification", notification);
            mContinuation.resume();
        } catch (IllegalStateException e) {
            // TODO: have to figure out why this happens
            log.error("Got exception dispatching notification.", e);
        }
    }
}
