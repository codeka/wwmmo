package au.com.codeka.warworlds;

import java.io.IOException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;

import android.util.Log;

public class RequestRetryHandler implements HttpRequestRetryHandler {
	private static String TAG = "RequestRetryHandler";
	
	@Override
	public boolean retryRequest(IOException exception, int executionCount,
			HttpContext context) {
		Log.w(TAG, "Got exception, not retrying...");
		Log.w(TAG, ExceptionUtils.getStackTrace(exception));
		Log.w(TAG, context.toString());
		return false;
	}

}
