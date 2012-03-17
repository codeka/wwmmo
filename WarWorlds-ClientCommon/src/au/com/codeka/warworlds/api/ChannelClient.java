package au.com.codeka.warworlds.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.api.RequestManager.ResultWrapper;

/**
 * Currently, there's no API for the Channel API. We used a "reverse engineered" one that I found
 * here: http://code.google.com/p/googleappengine/issues/attachmentText?id=4189&aid=-4261190830238754762&name=GaeChannel.java&token=Gm4eKayq8bmy1IODfY5VQ37vEtk%3A1331947938015
 * 
 * Slightly modified to use HttpCore instead of HttpClient, and remove some of the silliness that
 * came from the fact that this was a direct port from JavaScript (and hence has XMLHttpPost stuff!)
 */
public class ChannelClient {
    private static final Logger log = LoggerFactory.getLogger(ChannelClient.class);

    public interface ChannelListener {
        void onOpen();
        void onMessage(String message);
        void onError(int code, String description);
        void onClose();
    }

    private ChannelListener mDefaultChannelListener = new ChannelListener() {
        @Override
        public void onOpen() {
            log.info("ChannelClient: onOpen");
        }

        @Override
        public void onMessage(String message) {
            log.info("ChannelClient: onMessage = " + message);
        }

        @Override
        public void onError(int code, String description) {
            log.info("ChannelClient: onError = " + description + " [" + code + "]");
        }

        @Override
        public void onClose() {
            log.info("ChannelClient: onClose");
        }
    };

    private int POLLING_TIMEOUT_MS = 500;
    private static final String BASE_URL = "/_ah/channel/";

    private String mToken = null;
    private String mClientID = null;
    private ChannelListener mChannelListener = mDefaultChannelListener;
    private boolean mIsRunning = true;

    private Thread mPollThread = null;

    public ChannelClient(String token, ChannelListener channelListener) {
        mToken = token;
        mClientID = null;
        if (channelListener != null) {
            mChannelListener = channelListener;
        }
    }

    public void open() throws ApiException {
        mIsRunning = true;
        connect(request(getUrl("connect")));
    }

    public void close() throws ApiException {
        mIsRunning = false;
        disconnect(request(getUrl("disconnect")));
    }

    private String getUrl(String command) {
        try {
            String url = BASE_URL + "dev?command=" + command + "&channel=";
            url += URLEncoder.encode(mToken, "UTF-8");
            if (mClientID != null) {
                url += "&client=" + URLEncoder.encode(mClientID, "UTF-8");
            }
            return url;
        } catch (UnsupportedEncodingException e) {
            return null; // should never happen
        }
    };

    private void connect(ResultWrapper res) {
        if (!wasError(res)) {
            mClientID = getResponseString(res);
            mChannelListener.onOpen();
            poll();
        } else {
            mChannelListener.onClose();
        }
    }

    private void disconnect(ResultWrapper resp) {
        wasError(resp); // ignore errors, just report
        mChannelListener.onClose();
    }

    private void forwardMessage(ResultWrapper res) {
        if (!wasError(res)) {
            String data = getResponseString(res);
            data = StringUtils.chomp(data);
            if (!StringUtils.isEmpty(data)) {
                mChannelListener.onMessage(data);
            }
        }
    }

    private void poll() {
        if (mPollThread != null) {
            return;
        }

        mPollThread = new Thread(new Runnable() {
           @Override
           public void run() {
               while(mIsRunning) {
                   try {
                       Thread.sleep(POLLING_TIMEOUT_MS);
                   } catch (InterruptedException e) {
                   }

                   try {
                       ResultWrapper res = request(getUrl("poll"));
                       forwardMessage(res);
                   } catch (ApiException e) {
                       log.error("Erroring polling for updates from channel, giving up...");
                       return;
                   }
               }
           }
        });
        mPollThread.start();
    }

    private boolean wasError(ResultWrapper res) {
        HttpResponse resp = res.getResponse();
        if (resp.getStatusLine().getStatusCode() > 299) {
            mChannelListener.onError(resp.getStatusLine().getStatusCode(),
                    resp.getStatusLine().toString());
            return true;
        }
        return false;
    }

    private String getResponseString(ResultWrapper res) {
        HttpResponse resp = res.getResponse();

        StringWriter outw = new StringWriter();
        try {
            BufferedReader inr = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
            char[] buffer = new char[1024];
            int n;
            while ((n = inr.read(buffer)) > 0) {
                outw.write(buffer, 0, n);
            }
        } catch (IOException e) {
        }
        outw.flush();

        return outw.getBuffer().toString();
    }

    private ResultWrapper request(String url) throws ApiException {
        TreeMap<String, List<String>> headers = new TreeMap<String, List<String>>();
        List<String> cookies = ApiClient.getCookies();
        if (!cookies.isEmpty()) {
            headers.put("Cookie", cookies);
        }

        return RequestManager.request("GET", url, headers);
    }
}

