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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;
import android.util.Log;
import au.com.codeka.warworlds.shared.Device;
import au.com.codeka.warworlds.shared.DeviceResource;
import au.com.codeka.warworlds.shared.DevicesResource;

/**
 * Register/unregister with the third-party App Engine server using
 * RequestFactory.
 */
public class DeviceRegistrar {
	public static String TAG = "DeviceRegistrar";

    public static final String ACCOUNT_NAME_EXTRA = "AccountName";

    public static final String STATUS_EXTRA = "Status";

    public static final int REGISTERED_STATUS = 1;

    public static final int UNREGISTERED_STATUS = 2;

    public static final int ERROR_STATUS = 3;

    public static void register(final Context context, final String deviceRegistrationID) {
    	registerOrUnregister(context, deviceRegistrationID, true);
    }

    public static void unregister(final Context context, final String deviceRegistrationID) {
    	registerOrUnregister(context, deviceRegistrationID, false);
    }
    
    private static void registerOrUnregister(final Context context,
            final String deviceRegistrationID, final boolean register) {

    	final SharedPreferences settings = Util.getSharedPreferences(context);
        final String accountName = settings.getString(Util.ACCOUNT_NAME, "Unknown");
        final Intent updateUIIntent = new Intent(Util.UPDATE_UI_INTENT);

        String url = "/devices";

    	try {
    		if (register) {
	        	Device d = new Device();
	        	d.setDeviceRegistrationID(deviceRegistrationID);
	        	d.setDeviceID(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));

    			DevicesResource resource = Util.getClientResource(context, url, DevicesResource.class);
	        	resource.register(d);
	        } else {
	        	url += "/" + deviceRegistrationID;

    			DeviceResource resource = Util.getClientResource(context, url, DeviceResource.class);
	    		resource.unregister();
	        }
    	} catch(Exception ex) {
            Log.w(TAG, "Failure, got: " + ex.getMessage());
            // Clean up application state
            Util.clearDeviceRegistration(context);

            updateUIIntent.putExtra(ACCOUNT_NAME_EXTRA, accountName);
            updateUIIntent.putExtra(STATUS_EXTRA, ERROR_STATUS);
            context.sendBroadcast(updateUIIntent);
            return;
    	}
    	
        if (register) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(Util.DEVICE_REGISTRATION_ID, deviceRegistrationID);
            editor.commit();
        } else {
        	Util.clearDeviceRegistration(context);
        }

        updateUIIntent.putExtra(ACCOUNT_NAME_EXTRA, accountName);
        updateUIIntent.putExtra(STATUS_EXTRA, register ? REGISTERED_STATUS : UNREGISTERED_STATUS);
        context.sendBroadcast(updateUIIntent);
    }
}
