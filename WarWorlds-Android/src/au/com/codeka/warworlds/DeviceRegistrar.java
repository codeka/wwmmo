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
import android.content.SharedPreferences;
import android.provider.Settings.Secure;
import android.util.Log;

/**
 * Register/unregister with the third-party App Engine server using
 * RequestFactory.
 */
public class DeviceRegistrar {
	public static String TAG = "DeviceRegistrar";

    public static void register(final Context context, final String deviceRegistrationID) {
    	registerOrUnregister(context, deviceRegistrationID, true);
    }

    public static void unregister(final Context context, final String deviceRegistrationID) {
    	registerOrUnregister(context, deviceRegistrationID, false);
    }
    
    private static void registerOrUnregister(final Context context,
            final String deviceRegistrationID, final boolean register) {

    	final SharedPreferences settings = Util.getSharedPreferences(context);

        String url = "/devices";

    	try {/*
    		if (register) {
	        	Device d = new Device();
	        	d.setDeviceRegistrationID(deviceRegistrationID);
	        	d.setDeviceID(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));

    			DevicesResource resource = Util.getClientResource(url, DevicesResource.class);
	        	resource.register(d);
	        } else {
	        	url += "/" + deviceRegistrationID;

    			DeviceResource resource = Util.getClientResource(url, DeviceResource.class);
	    		resource.unregister();
	        }*/
    	} catch(Exception ex) {
            Log.w(TAG, "Failure, got: " + ex.getMessage());
            // Clean up application state
            Util.clearDeviceRegistration(context);

            return;
    	}
    	
        if (register) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(Util.DEVICE_REGISTRATION_ID, deviceRegistrationID);
            editor.commit();
        } else {
        	Util.clearDeviceRegistration(context);
        }
    }
}
