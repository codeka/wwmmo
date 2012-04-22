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

/**
 * This is a client for talking to an App Engine Channel API server. Currently, there's no
 * Java (or anything but JavaScript) API, so we had to come up with our own.
 */
public abstract class ChannelClient {
    /**
     * Implement this interface to get notifications about messages, errors and so on.
     */
    public interface ChannelListener {
        void onOpen();
        void onMessage(String message);
        void onError(int code, String description);
        void onClose();
    }

    /**
     * Creates a new \c ChannelClient. We determine whether you're talking to a dev server or
     * prod server by whether or not your \c appEngineURI includes the string "localhost"
     * (TODO: is there a better way?)
     * 
     * @param appEngineURI The URI to your App Engine app (e.g. https://myapp.appspot.com/)
     * @param token The channel token you got from calling \c create_channel on the server.
     * @param listener Your implementation of \c ChannelListener.
     */
    public static ChannelClient createChannel(URI appEngineURI, String token, ChannelListener listener) {
        if (appEngineURI.toString().indexOf("appspot") < 0) {
            return new DevChannelClient(appEngineURI, token, listener);
        } else {
            return new ProdChannelClient(appEngineURI, token, listener);
        }
    }

    public abstract void open() throws ChannelException;

    public abstract void close() throws ChannelException;

    /**
     * A helper method that cnsumes an HttpEntity so that the HttpClient can be reused. If you're
     * not planning to run on Android, you can use the non-deprecated EntityUtils.consume() method
     * instead.
     */
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
     * This exception is thrown in case of errors.
     */
    public static class ChannelException extends Exception {
        private static final long serialVersionUID = 1L;

        public ChannelException() {
        }

        public ChannelException(Throwable cause) {
            super(cause);
        }

        public ChannelException(String message) {
            super(message);
        }

        public ChannelException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * This implementation of ChannelClient talks to the production server.
     */
    private static class ProdChannelClient extends ChannelClient {
        private static final Logger log = LoggerFactory.getLogger(ProdChannelClient.class);

        private HttpClient mHttpClient;
        private URI mAppEngineURI;
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

        public ProdChannelClient(URI appEngineURI, String token, ChannelListener listener) {
            mToken = token;
            mChannelListener = listener;
            mAppEngineURI = appEngineURI;
            try {
                mBaseURI = new URI("https://talkgadget.google.com/talkgadget/");
            } catch (URISyntaxException e) {
            } // won't happen with our hard-coded URI
            mHttpClient = new DefaultHttpClient();
            mRequestID = 0;
            mMessageID = 1;
        }

        @Override
        public void open() throws ChannelException {
            initialize();
            fetchSid();
            connect();
            longPoll();
        }

        @Override
        public void close() throws ChannelException {
            mIsRunning = false;
            // TODO: wait for it to actually close...
            mChannelListener.onClose();
        }

        /**
         * Sets up the initial connection, passes in the token and whatnot.
         */
        @SuppressWarnings("unchecked") // JSONObject
        private void initialize() throws ChannelException {
            String url = mAppEngineURI.getHost();

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
                    throw new ChannelException("Initialize failed: "+resp.getStatusLine());
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
                            throw new ChannelException("Expected iteration #"+i+" to find something.");
                        }
                        if (i == 2) {
                            mClid = m.group(1);
                        } else if (i == 3) {
                            mSessionID = m.group(1);
                        } else if (i == 6) {
                            if (!mToken.equals(m.group(1))) {
                                throw new ChannelException("Tokens do not match!");
                            }
                        }
                    }
                }
            } catch(IOException e) {
                throw new ChannelException(e);
            }

            log.debug("Initialization complete, [CLID="+mClid+"] [SessionID="+mSessionID+"]");
        }

        /**
         * Fetches and parses the SID, which is a kind of session ID.
         */
        private void fetchSid() throws ChannelException {
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
                throw new ChannelException(e);
            } catch (IOException e) {
                throw new ChannelException(e);
            } catch (InvalidMessageException e) {
                throw new ChannelException(e);
            } finally {
                if (parser != null) {
                    parser.close();
                }
            }
        }

        /**
         * We need to make this "connect" request to set up the binding.
         */
        private void connect() throws ChannelException {
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
                throw new ChannelException(e);
            } catch (IOException e) {
                throw new ChannelException(e);
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

        /**
         * Begins the "long poll" thread, which basically just keeps a connection open to the
         * /bind endpoint, reading messages. If the connection is lost we simply re-connect.
         */
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
                    } catch (ChannelException e) {
                    }

                    return null;
                }

                @Override
                public void run() {
                    TalkMessageParser parser = null;
                    while (mIsRunning) {
                        if (parser == null) {
                            parser = repoll();
                            if (parser == null) {
                                // if we can't connect, wait a few seconds and try again. Maybe
                                // we lost network connectivity or something.
                                log.warn("Could not connect to channel endpoint, trying again.");
                                try {
                                    Thread.sleep(2500);
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                        try {
                            TalkMessage msg = parser.getMessage();
                            if (msg == null) {
                                parser.close();
                                parser = null;
                            } else {
                                handleMessage(msg);
                            }
                        } catch (ChannelException e) {
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

        /**
         * Helper class for parsing talk messages. Again, this protocol has been reverse-engineered
         * so it doesn't have a lot of error checking and is generally fairly lenient.
         */
        private static class TalkMessageParser {
            private HttpResponse mHttpResponse;
            private BufferedReader mReader;

            public TalkMessageParser(HttpResponse resp) throws ChannelException {
                try {
                    mHttpResponse = resp;
                    InputStream ins = resp.getEntity().getContent();
                    mReader = new BufferedReader(new InputStreamReader(ins));
                } catch (IllegalStateException e) {
                    throw new ChannelException(e);
                } catch (IOException e) {
                    throw new ChannelException(e);
                }
            }

            public TalkMessage getMessage() throws ChannelException {
                String submission = readSubmission();
                if (submission == null) {
                    return null;
                }

                TalkMessage msg = new TalkMessage();

                try {
                    msg.parse(new BufferedReader(new StringReader(submission)));
                } catch (InvalidMessageException e) {
                    throw new ChannelException(e);
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

            private String readSubmission() throws ChannelException {
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
                    throw new ChannelException(e);
                } catch(NumberFormatException e) {
                    throw new ChannelException("Submission was not in expected format.", e);
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

        private URI mAppEngineURI;
        private String mToken = null;
        private String mClientID = null;
        private ChannelListener mChannelListener = null;
        private boolean mIsRunning = true;
        private Thread mPollThread = null;
        private HttpClient mHttpClient;

        public DevChannelClient(URI appEngineURI, String token, ChannelListener channelListener) {
            mAppEngineURI = appEngineURI;
            mToken = token;
            mClientID = null;
            mChannelListener = channelListener;
            mHttpClient = new DefaultHttpClient();
        }

        @Override
        public void open() throws ChannelException {
            mIsRunning = true;
            connect(request(getURI("connect")));
        }

        @Override
        public void close() throws ChannelException {
            mIsRunning = false;
            disconnect(request(getURI("disconnect")));
        }

        private URI getURI(String command) {
            try {
                String url = "/_ah/channel/dev?command=" + command + "&channel=";
                url += URLEncoder.encode(mToken, "UTF-8");
                if (mClientID != null) {
                    url += "&client=" + URLEncoder.encode(mClientID, "UTF-8");
                }
                return mAppEngineURI.resolve(url);
            } catch (UnsupportedEncodingException e) {
                return null; // should never happen
            }
        };

        private void connect(HttpResponse res) {
            if (!wasError(res)) {
                mClientID = getResponseString(res);
                mChannelListener.onOpen();
                poll();
            } else {
                mChannelListener.onClose();
            }
        }

        private void disconnect(HttpResponse resp) {
            wasError(resp); // ignore errors, just report
            mChannelListener.onClose();
        }

        private void forwardMessage(HttpResponse res) {
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
                           HttpResponse res = request(getURI("poll"));
                           forwardMessage(res);
                           consume(res.getEntity());
                       } catch (ChannelException e) {
                           log.error("Erroring polling for updates from channel, giving up...");
                           return;
                       }
                   }
               }
            });
            mPollThread.start();
        }

        private boolean wasError(HttpResponse resp) {
            if (resp.getStatusLine().getStatusCode() > 299) {
                mChannelListener.onError(resp.getStatusLine().getStatusCode(),
                        resp.getStatusLine().toString());
                return true;
            }
            return false;
        }

        private String getResponseString(HttpResponse resp) {
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

        private HttpResponse request(URI uri) throws ChannelException {
            HttpGet httpGet = new HttpGet(uri);

            List<String> cookies = ApiClient.getCookies();
            for (String cookie : cookies) {
                httpGet.addHeader("Cookie", cookie);
            }

            try {
                return mHttpClient.execute(httpGet);
            } catch (ClientProtocolException e) {
                throw new ChannelException(e);
            } catch (IOException e) {
                throw new ChannelException(e);
            }
        }
    }
}

