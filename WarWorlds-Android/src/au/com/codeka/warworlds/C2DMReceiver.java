/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package au.com.codeka.warworlds;

import com.google.android.c2dm.C2DMBaseReceiver;
import com.google.android.c2dm.C2DMessaging;

import java.io.IOException;
import java.util.concurrent.Callable;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Receive a push message from the Cloud to Device Messaging (C2DM) service.
 * This class should be modified to include functionality specific to your
 * application. This class must have a no-arg constructor and pass the sender id
 * to the superclass constructor.
 */
public class C2DMReceiver extends C2DMBaseReceiver {
	private static String TAG = "C2DMReceiver";
	
	private static Callable<Void> sOnComplete;
	private static Activity sActivity;
	
    public C2DMReceiver() {
        super(Setup.SENDER_ID);
    }

    /**
     * Registers for C2DM notifications. Calls AccountsActivity.registrationComplete() when finished.
     */
    public static void register(Activity activity, String senderID, final Callable<Void> onComplete) {
    	sOnComplete = onComplete;
    	sActivity = activity;
        C2DMessaging.register(activity, Setup.SENDER_ID);
    }

    /**
     * Unregisters ourselves from C2DM notifications.
     */
    public static void unregister(Activity activity, final Callable<Void> onComplete) {
    	sOnComplete = onComplete;
    	sActivity = activity;
    	C2DMessaging.unregister(activity);
    }

    /**
     * Calls the onComplete handler (if there is one), making sure to do so on the main UI thread.
     */
    private static void callOnComplete() {
        if (sOnComplete != null && sActivity != null) {
            sActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        sOnComplete.call();
                        sOnComplete = null;
                    } catch(Exception e) {
                    }
                }
            });
        }
    }

    /**
     * Called when a registration token has been received.
     * 
     * @param context the Context
     * @param registrationId the registration id as a String
     * @throws IOException if registration cannot be performed
     */
    @Override
    public void onRegistered(Context context, String registration) {
    	Log.i(TAG, "Registration complete.");
        DeviceRegistrar.register(context, registration);
        callOnComplete();
    }

    /**
     * Called when the device has been unregistered.
     * 
     * @param context the Context
     */
    @Override
    public void onUnregistered(Context context) {
    	Log.i(TAG, "De-registration complete.");
        SharedPreferences prefs = Util.getSharedPreferences(context);
        String deviceRegistrationID = prefs.getString(Util.DEVICE_REGISTRATION_ID, null);
        DeviceRegistrar.unregister(context, deviceRegistrationID);
        callOnComplete();
    }

    /**
     * Called on registration error. This is called in the context of a Service
     * - no dialog or UI.
     * 
     * @param context the Context
     * @param errorId an error message, defined in {@link C2DMBaseReceiver}
     */
    @Override
    public void onError(Context context, String errorId) {
    	Log.w(TAG, "Error: "+errorId);
        context.sendBroadcast(new Intent(Util.UPDATE_UI_INTENT));
        callOnComplete();
    }

    /**
     * Called when a cloud message has been received.
     */
    @Override
    public void onMessage(Context context, Intent intent) {
        /*
         * Replace this with your application-specific code
         */
    	Log.i(TAG, "Message received.");
        MessageDisplay.displayMessage(context, intent);
    }
}
