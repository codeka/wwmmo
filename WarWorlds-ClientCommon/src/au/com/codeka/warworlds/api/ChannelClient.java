package au.com.codeka.warworlds.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.api.RequestManager.ResultWrapper;

/**
 * This is a client for talking to an App Engine Channel API server. Currently, there's no
 * Java (or anything but JavaScript) API, so we had to come up with our own.
 */
public abstract class ChannelClient {

    public interface ChannelListener {
        void onOpen();
        void onMessage(String message);
        void onError(int code, String description);
        void onClose();
    }

    public static ChannelClient createChannel(String token, ChannelListener listener) {
        if (RequestManager.getBaseUri().toString().indexOf("localhost") >= 0) {
            return new DevChannelClient(token, listener);
        } else {
            return new ProdChannelClient(token, listener);
        }
    }

    public abstract void open() throws ApiException;

    public abstract void close() throws ApiException;

    /**
     * This implementation of ChannelClient talks to the production server. 
     * @author dean@codeka.com.au
     *
     */
    private static class ProdChannelClient extends ChannelClient {
        private static final Logger log = LoggerFactory.getLogger(ProdChannelClient.class);

        private HttpClient mHttpClient;
        private URI mBaseURI;
        private String mToken;
        private ChannelListener mChannelListener;
        private int mRequestID;
        private String mSid;
        private String mSessionID;
        private String mClid;
        private long mMessageID;
        private boolean mIsRunning;
        private Thread mLongPollThread;

        public ProdChannelClient(String token, ChannelListener listener) {
            mToken = token;
            mChannelListener = listener;
            try {
                mBaseURI = new URI("https://talkgadget.google.com/talkgadget/");
            } catch (URISyntaxException e) {
                // won't happen with our hard-coded URI
            }
            mHttpClient = new DefaultHttpClient();
            mRequestID = 0;
            mMessageID = 1;
        }

        @Override
        public void open() throws ApiException {
            initialize();
            fetchSid();
            connect();
            longPoll();
        }

        @Override
        public void close() throws ApiException {
            mIsRunning = false;
            // TODO: do something

            mChannelListener.onClose();
        }

        /**
         * Sets up the initial connection, passes in the token and whatnot.
         */
        @SuppressWarnings("unchecked") // JSONObject
        private void initialize() throws ApiException {
            String url = "warworldsmmo.appspot.com"; // TODO: real thing

            JSONObject xpc = new JSONObject();
            xpc.put("cn", RandomStringUtils.random(10, true, false));
            xpc.put("tp", null);
            xpc.put("lpu", "http://talkgadget.google.com/talkgadget/xpc_blank");
            xpc.put("ppu", "http://"+url+"/_ah/channel/xpc_blank");

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("token", mToken));
            params.add(new BasicNameValuePair("xpc", xpc.toJSONString()));

            URI initUri = mBaseURI.resolve("d?"+URLEncodedUtils.format(params, "utf-8"));
            log.debug("Initializing: "+initUri);

            HttpGet httpGet = new HttpGet(initUri);
            try {
                HttpResponse resp = mHttpClient.execute(httpGet);
                if (resp.getStatusLine().getStatusCode() > 299) {
                    throw new ApiException("Initialize failed: "+resp.getStatusLine());
                }

                // the response we get back is actually a bunch of Javascript, but lucky for us
                // the important bit is nice and easy to parse. What we actually get is like:
                
                // *JUNK*JUNK*JUNK*
                // var a = new chat.WcsDataClient("https://talkgadget.google.com/talkgadget/",
                //         "",
                //         "<CLID>",
                //         "<GSID>",
                //         "<something?>",
                //         "WCX",
                //         "<TOKEN>"
                //         );
                // *JUNK*JUNK*JUNK

                String html = IOUtils.toString(resp.getEntity().getContent(), "utf-8");
                consume(resp.getEntity());

                // OMG my eyes! Java regex support is horrible!
                Pattern p = Pattern.compile("chat\\.WcsDataClient\\(([^\\)]+)\\)",
                       Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
                Matcher m = p.matcher(html);
                if (m.find()) {
                    String fields = m.group(1);
                    p = Pattern.compile("\"([^\"]*?)\"[\\s,]*", Pattern.MULTILINE);
                    m = p.matcher(fields);

                    for(int i = 0; i < 7; i++) {
                        if (!m.find()) {
                            throw new ApiException("Expected iteration #"+i+" to find something.");
                        }
                        if (i == 2) {
                            mClid = m.group(1);
                        } else if (i == 3) {
                            mSessionID = m.group(1);
                        } else if (i == 6) {
                            if (!mToken.equals(m.group(1))) {
                                throw new ApiException("Tokens do not match!");
                            }
                        }
                    }
                }
            } catch(IOException e) {
                throw new ApiException(e);
            }

            log.debug("Initialization complete, [CLID="+mClid+"] [SessionID="+mSessionID+"]");
        }

        /**
         * Fetches the SID, which is a kind of session ID. The response message looks like this:
         * [0,["c","*SID*",,8]]
         */
        private void fetchSid() throws ApiException {
            log.debug("Fetching SID");
            URI uri = getBindUri(new BasicNameValuePair("CVER", "1"));

            HttpPost httpPost = new HttpPost(uri);

            List<NameValuePair> data = new ArrayList<NameValuePair>();
            data.add(new BasicNameValuePair("count", "0"));
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(data));
            } catch (UnsupportedEncodingException e) {
                //??
            }

            TalkMessageParser parser = null;
            try {
                HttpResponse resp = mHttpClient.execute(httpPost);
                parser = new TalkMessageParser(resp);
                TalkMessage msg = parser.getMessage();

                TalkMessage.TalkMessageEntry entry = msg.getEntries().get(0);
                entry = entry.getMessageValue().getEntries().get(1);
                List<TalkMessage.TalkMessageEntry> entries = entry.getMessageValue().getEntries();
                if (!entries.get(0).getStringValue().equals("c")) {
                    throw new InvalidMessageException("Expected first value to be 'c', found: "+
                                entries.get(0).getStringValue());
                }

                mSid = entries.get(1).getStringValue();
                log.debug("SID = "+mSid);
            } catch (ClientProtocolException e) {
                throw new ApiException(e);
            } catch (IOException e) {
                throw new ApiException(e);
            } catch (InvalidMessageException e) {
                throw new ApiException(e);
            } finally {
                if (parser != null) {
                    parser.close();
                }
            }
        }

        private void connect() throws ApiException {
            URI uri = getBindUri(new BasicNameValuePair("AID", Long.toString(mMessageID)),
                                 new BasicNameValuePair("CVER", "1"));

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("count", "1"));
            params.add(new BasicNameValuePair("ofs", "0"));
            params.add(new BasicNameValuePair("req0_m", "[\"connect-add-client\"]"));
            params.add(new BasicNameValuePair("req0_c", mClid));
            params.add(new BasicNameValuePair("req0__sc", "c"));

            HttpEntity entity = null;
            try {
                entity = new UrlEncodedFormEntity(params);
            } catch(UnsupportedEncodingException e) {
                //??
            }

            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(entity);
            try {
                HttpResponse resp = mHttpClient.execute(httpPost);
                consume(resp.getEntity());
            } catch (ClientProtocolException e) {
                throw new ApiException(e);
            } catch (IOException e) {
                throw new ApiException(e);
            }

            mChannelListener.onOpen();
        }

        /**
         * Gets the URL to the "/bind" endpoint.
         */
        private URI getBindUri(NameValuePair ... extraParams) {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("token", mToken));
            params.add(new BasicNameValuePair("gsessionid", mSessionID));
            params.add(new BasicNameValuePair("clid", mClid));
            params.add(new BasicNameValuePair("prop", "data"));
            params.add(new BasicNameValuePair("zx", RandomStringUtils.random(12, true, false)));
            params.add(new BasicNameValuePair("t", "1"));
            if (mSid != null && mSid != "") {
                params.add(new BasicNameValuePair("SID", mSid));
            }
            for (int i = 0; i < extraParams.length; i++) {
                params.add(extraParams[i]);
            }

            params.add(new BasicNameValuePair("RID", Integer.toString(mRequestID)));
            mRequestID ++;

            return mBaseURI.resolve("dch/bind?VER=8&"+URLEncodedUtils.format(params, "utf-8"));
        }

        private void longPoll() {
            if (mLongPollThread != null) {
                return;
            }

            mLongPollThread = new Thread(new Runnable() {
                private TalkMessageParser repoll() {
                    URI bindUri = getBindUri(new BasicNameValuePair("CI", "0"),
                            new BasicNameValuePair("AID", Long.toString(mMessageID)),
                            new BasicNameValuePair("TYPE", "xmlhttp"),
                            new BasicNameValuePair("RID", "rpc"));

                    HttpGet httpGet = new HttpGet(bindUri);
                    HttpResponse resp = null;
                    try {
                        resp = mHttpClient.execute(httpGet);
                        return new TalkMessageParser(resp);
                    } catch (ClientProtocolException e) {
                    } catch (IOException e) {
                    } catch (ApiException e) {
                    }

                    return null;
                }

                @Override
                public void run() {
                    TalkMessageParser parser = null;
                    while (mIsRunning) {
                        if (parser == null) {
                            parser = repoll();
                        }
                        try {
                            TalkMessage msg = parser.getMessage();
                            if (msg == null) {
                                parser.close();
                                parser = null;
                            } else {
                                handleMessage(msg);
                            }
                        } catch (ApiException e) {
                            mChannelListener.onError(500, e.getMessage());
                            e.printStackTrace();
                            return; // TODO??
                        }
                   }
               }
            });

            mIsRunning = true;
            mLongPollThread.start();
        }

        /**
         * Called each time we receive a message from the server. Determines whether it's actually
         * a message (sometimes we get "noop" or something else) and if so, calls the channel
         * listener.
         */
        private void handleMessage(TalkMessage msg) {
            try {
                List<TalkMessage.TalkMessageEntry> entries = msg.getEntries();
                msg = entries.get(0).getMessageValue();

                entries = msg.getEntries();
                mMessageID = entries.get(0).getNumberValue();

                msg = entries.get(1).getMessageValue();
                entries = msg.getEntries();

                if (entries.get(0).getKind() == TalkMessage.MessageEntryKind.ME_STRING &&
                    entries.get(0).getStringValue().equals("c")) {
                    msg = entries.get(1).getMessageValue();
                    entries = msg.getEntries();

                    String thisSessionID = entries.get(0).getStringValue();
                    if (!thisSessionID.equals(mSessionID)) {
                        mSessionID = thisSessionID;
                    }

                    msg = entries.get(1).getMessageValue();
                    entries = msg.getEntries();

                    if (entries.get(0).getStringValue().equalsIgnoreCase("ae")) {
                        String msgValue = entries.get(1).getStringValue();
                        mChannelListener.onMessage(msgValue);
                    }
                }
            } catch (InvalidMessageException e) {
                e.printStackTrace();
            }
        }

        @SuppressWarnings("deprecation")
        private static void consume(HttpEntity entity) {
            // make sure we've finished with the entity...
            try {
                if (entity != null) {
                    entity.consumeContent();
                }
            } catch (IOException e) {
                // ignore....
            }
        }

        /**
         * Helper class for parsing talk messages. Again, this protocol has been reverse-engineered
         * so it doesn't have a lot of error checking and is generally fairly lenient.
         */
        private static class TalkMessageParser {
            private HttpResponse mHttpResponse;
            private BufferedReader mReader;

            public TalkMessageParser(HttpResponse resp) throws ApiException {
                try {
                    mHttpResponse = resp;
                    InputStream ins = resp.getEntity().getContent();
                    mReader = new BufferedReader(new InputStreamReader(ins));
                } catch (IllegalStateException e) {
                    throw new ApiException(e);
                } catch (IOException e) {
                    throw new ApiException(e);
                }
            }

            public TalkMessage getMessage() throws ApiException {
                String submission = readSubmission();
                if (submission == null) {
                    return null;
                }

                TalkMessage msg = new TalkMessage();

                try {
                    msg.parse(new BufferedReader(new StringReader(submission)));
                } catch (InvalidMessageException e) {
                    throw new ApiException(e);
                }

                return msg;
            }

            public void close() {
                try {
                    mReader.close();
                } catch (IOException e) {
                }

                if (mHttpResponse != null) {
                    consume(mHttpResponse.getEntity());
                }
            }

            private String readSubmission() throws ApiException {
                try {
                    String line = mReader.readLine();
                    if (line == null) {
                        return null;
                    }

                    int numChars = Integer.parseInt(line);
                    char[] chars = new char[numChars];
                    int total = 0;
                    while (total < numChars) {
                        int numRead = mReader.read(chars, total, numChars - total);
                        total += numRead;
                    }
                    return new String(chars);
                } catch (IOException e) {
                    throw new ApiException(e);
                } catch(NumberFormatException e) {
                    throw new ApiException("Submission was not in expected format.", e);
                }
            }
        }

        /**
         * A "talk" message is a data structure containing lists of strings, integers
         * and (recursive) talk messages.
         */
        private static class TalkMessage {
            public enum MessageEntryKind {
                ME_STRING,
                ME_NUMBER,
                ME_EMPTY,
                ME_TALKMESSAGE
            }

            private ArrayList<TalkMessageEntry> mEntries;

            public TalkMessage() {
                mEntries = new ArrayList<TalkMessageEntry>();
            }
            private TalkMessage(ArrayList<TalkMessageEntry> entries) {
                mEntries = entries;
            }

            public List<TalkMessageEntry> getEntries() {
                return mEntries;
            }

            public void parse(BufferedReader reader) throws InvalidMessageException {
                try {
                    if (skipWhitespace(reader) != '[') {
                        throw new InvalidMessageException("Expected initial [");
                    }

                    mEntries = parseMessage(reader);
                } catch (IOException e) {
                    throw new InvalidMessageException(e);
                }
            }

            @Override
            public String toString() {
                String str = "[";
                for(TalkMessageEntry entry : mEntries) {
                    if (str != "[") {
                        str += ",";
                    }
                    str += entry.toString();
                }
                return str + "]";
            }

            private static ArrayList<TalkMessageEntry> parseMessage(BufferedReader reader)
                    throws InvalidMessageException, IOException {
                ArrayList<TalkMessageEntry> entries = new ArrayList<TalkMessageEntry>();

                int ch = skipWhitespace(reader);
                while (ch != ']') {
                    if (ch < 0) {
                        throw new InvalidMessageException("Unexpected end-of-message.");
                    }

                    if (ch == '[') {
                        ArrayList<TalkMessageEntry> childEntries = parseMessage(reader);
                        entries.add(new TalkMessageEntry(MessageEntryKind.ME_TALKMESSAGE,
                                new TalkMessage(childEntries)));
                    } else if (ch == '\"' || ch == '\'') {
                        String stringValue = parseStringValue(reader, (char) ch);
                        entries.add(new TalkMessageEntry(MessageEntryKind.ME_STRING, stringValue));
                    } else if (ch == ',') {
                        // blank entry
                        entries.add(new TalkMessageEntry(MessageEntryKind.ME_EMPTY, null));
                    } else {
                        // we assume it's a number
                        long numValue = parseNumberValue(reader, (char) ch);
                        entries.add(new TalkMessageEntry(MessageEntryKind.ME_NUMBER, numValue));
                    }

                    // we expect a comma next, or the end of the message
                    if (ch != ',') {
                        ch = skipWhitespace(reader);
                    }

                    if (ch != ',' && ch != ']') {
                        throw new InvalidMessageException("Expected , or ], found "+((char) ch));
                    } else if (ch == ',') {
                        ch = skipWhitespace(reader);
                    }
                }

                return entries;
            }

            private static String parseStringValue(BufferedReader reader, char quote)
                    throws IOException {
                String str = "";
                for(int ch = reader.read(); ch > 0 && ch != quote; ch = reader.read()) {
                    if (ch == '\\') {
                        ch = reader.read();
                        if (ch < 0) {
                            break;
                        }
                    }
                    str += (char) ch;
                }

                return str;
            }

            private static long parseNumberValue(BufferedReader reader, char firstChar)
                    throws IOException {
                String str = "";
                for(int ch = firstChar; ch > 0 && Character.isDigit(ch); ch = reader.read()) {
                    str += (char) ch;
                    reader.mark(1);
                }
                reader.reset();

                return Long.parseLong(str);
            }

            private static int skipWhitespace(BufferedReader reader) throws IOException {
                int ch = reader.read();
                while (ch >= 0) {
                    if (!Character.isWhitespace(ch)) {
                        return ch;
                    }
                    ch = reader.read();
                }
                return -1;
            }

            public static class TalkMessageEntry {
                MessageEntryKind mKind;
                Object mValue;

                public TalkMessageEntry(MessageEntryKind kind, Object value) {
                    mKind = kind;
                    mValue = value;
                }

                public MessageEntryKind getKind() {
                    return mKind;
                }
                public String getStringValue() throws InvalidMessageException {
                    if (mKind == MessageEntryKind.ME_STRING) {
                        return (String) mValue;
                    } else {
                        throw new InvalidMessageException("String value expected, found: "+mKind+" ("+mValue+")");
                    }
                }
                public long getNumberValue() throws InvalidMessageException {
                    if (mKind == MessageEntryKind.ME_NUMBER) {
                        return (Long) mValue;
                    } else {
                        throw new InvalidMessageException("Number value expected, found: "+mKind+" ("+mValue+")");
                    }
                }
                public TalkMessage getMessageValue() throws InvalidMessageException {
                    if (mKind == MessageEntryKind.ME_TALKMESSAGE) {
                        return (TalkMessage) mValue;
                    } else {
                        throw new InvalidMessageException("TalkMessage value expected, found: "+mKind+" ("+mValue+")");
                    }
                }

                @Override
                public String toString() {
                    if (mKind == MessageEntryKind.ME_EMPTY) {
                        return "";
                    } else if (mKind == MessageEntryKind.ME_STRING) {
                        return "\""+mValue.toString()+"\"";
                    } else {
                        return mValue.toString();
                    }
                }
            }
        }

        /**
         * This exception will be thrown any time we have an issue parsing a talk message. Probably
         * this means they've changed the protocol on us.
         */
        public static class InvalidMessageException extends Exception {
            private static final long serialVersionUID = 1L;

            public InvalidMessageException(String msg) {
                super(msg);
            }

            public InvalidMessageException(Throwable e) {
                super(e);
            }
        }
    }

    /**
     * This implementation of ChannelClient talks to the development server, using a simple
     * polling mechanism.
     */
    private static class DevChannelClient extends ChannelClient {
        private static final Logger log = LoggerFactory.getLogger(DevChannelClient.class);

        private int POLLING_TIMEOUT_MS = 500;
        private static final String BASE_URL = "/_ah/channel/";

        private String mToken = null;
        private String mClientID = null;
        private ChannelListener mChannelListener = null;
        private boolean mIsRunning = true;
        private Thread mPollThread = null;

        public DevChannelClient(String token, ChannelListener channelListener) {
            mToken = token;
            mClientID = null;
            mChannelListener = channelListener;
        }

        @Override
        public void open() throws ApiException {
            mIsRunning = true;
            connect(request(getUrl("connect")));
        }

        @Override
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
}

