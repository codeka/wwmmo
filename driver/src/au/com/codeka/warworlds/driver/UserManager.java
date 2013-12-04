package au.com.codeka.warworlds.driver;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.ws.http.HTTPException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import au.com.codeka.common.NormalRandom;
import au.com.codeka.common.protobuf.Messages;

import com.google.protobuf.Message;

public class UserManager {
    public static UserManager i = new UserManager();
    private UserManager() {
    }

    private final ThreadFactory mThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);
        public Thread newThread(Runnable r) {
            return new Thread(r, "UserThread #" + mCount.getAndIncrement());
        }
    };
    private final ScheduledThreadPoolExecutor mExecutor = new ScheduledThreadPoolExecutor(10, mThreadFactory);

    private NormalRandom mRand = new NormalRandom();
    private boolean mStopping;
    private int mDelayMs;
    private ArrayList<User> mUsers;
    private String mBaseUrl;

    public void start(String baseUrl, int numUsers, int delayMs) {
        mStopping = false;

        mBaseUrl = baseUrl;
        mDelayMs = delayMs;
        mUsers = new ArrayList<User>(numUsers);

        Random rand = new Random();
        for (int i = 0; i < numUsers; i++) {
            User user = new User(i);
            mUsers.add(user);
            queueUser(user, rand.nextInt(mDelayMs));
        }
    }

    public void stop() {
        mStopping = true;
    }

    private void queueUser(final User user, final int delayMs) {
        mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    user.execute();
                } catch (Exception e) {
                    System.out.println("Exception caught in worker!\n\n"+e);
                }
                if (!mStopping) {
                    queueUser(user, user.getNextExecuteDelay());
                }
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private class User {
        private int mIndex;
        private String mSessionKey;
        private String mUsername;
        private HttpClient mHttpClient;
        private int mHomeStarID;
        private Messages.Sector mHomeSector;

        public User(int index) {
            mIndex = index;
            mHttpClient = new DefaultHttpClient();
        }

        /**
         * Called to execute a request to the server.
         * @throws IOException 
         * @throws ClientProtocolException 
         * @throws URISyntaxException 
         */
        public void execute() throws Exception {
            if (mSessionKey == null) {
                mUsername = String.format("warworldstest%d@gmail.com", mIndex + 1);
                System.out.format("Logging in '%s'...\n", mUsername);
                HttpGet get = new HttpGet(new URI(mBaseUrl).resolve("/login?email="+mUsername));
                HttpResponse resp = mHttpClient.execute(get);

                Header[] cookies = resp.getHeaders("Set-Cookie");
                for (Header header : cookies) {
                    String[] nvp = header.getValue().split("=");
                    if (nvp[0].equals("SESSION")) {
                        mSessionKey = nvp[1];
                    }
                }
                get.releaseConnection();

                Messages.HelloResponse hello_response_pb = putOrPostProtoBuf(
                        new HttpPut(new URI(mBaseUrl).resolve("hello/1234")), 
                        Messages.HelloRequest.newBuilder().build(), Messages.HelloResponse.class);
                if (!hello_response_pb.hasEmpire()) {
                    // we have to create the initial empire...
                    putOrPostProtoBuf(
                            new HttpPut(new URI(mBaseUrl).resolve("empires")),
                            Messages.Empire.newBuilder()
                                           .setDisplayName("Empire "+(mIndex + 1))
                                           .setState(Messages.Empire.EmpireState.ACTIVE)
                                           .build(),
                            null);

                    hello_response_pb = putOrPostProtoBuf(
                            new HttpPut(new URI(mBaseUrl).resolve("hello/1234")), 
                            Messages.HelloRequest.newBuilder().build(), Messages.HelloResponse.class);
                }

                Messages.Star home_star_pb = hello_response_pb.getEmpire().getHomeStar();
                mHomeStarID = Integer.parseInt(home_star_pb.getKey());
                System.out.format("Home Star ID: %d\n", mHomeStarID);

                // get the sector our home star is in. This will let us choose from a bunch of
                // stars for simulating and stuff.
                Messages.Sectors sectors_pb = getProtoBuf(
                        String.format("sectors?coords=%d,%d", home_star_pb.getSectorX(), home_star_pb.getSectorY()),
                        Messages.Sectors.class);
                mHomeSector = sectors_pb.getSectors(0);
            }

            UserAction action = new SimulateStarAction(this);
            action.execute();
            System.out.println("User #"+mIndex+" executing: "+action.getElapsedMillis() + "ms");
        }

        /**
         * Gets the delay, in millis before we want to run again.
         */
        public int getNextExecuteDelay() {
            double r = mRand.next();
            return mDelayMs + (int) (r * (mDelayMs / 4));
        }

        private <T> T getProtoBuf(String url, Class<T> protoBuffFactory) throws Exception {
            HttpGet get = new HttpGet(new URI(mBaseUrl).resolve(url));
            get.addHeader("Cookie", "SESSION="+mSessionKey);
            try {
                HttpResponse resp = mHttpClient.execute(get);
                checkResponse(resp);

                return parseResponseBody(resp, protoBuffFactory);
            } finally {
                get.releaseConnection();
            }
        }

        private <T> T putOrPostProtoBuf(HttpEntityEnclosingRequestBase request, Message pb, Class<T> protoBuffFactory) throws Exception {
            ByteArrayEntity body = null;
            if (pb != null) {
                body = new ByteArrayEntity(pb.toByteArray());
                body.setContentType("application/x-protobuf");
                request.setEntity(body);
            }

            request.addHeader("Cookie", "SESSION="+mSessionKey);
            try {
                HttpResponse resp = mHttpClient.execute(request);
                checkResponse(resp);

                return parseResponseBody(resp, protoBuffFactory);
            } finally {
                request.releaseConnection();
            }
        }

        private void checkResponse(HttpResponse resp) throws Exception {
            int statusCode = resp.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode > 299) {
                throw new HTTPException(statusCode);
            }
        }

        @SuppressWarnings({"unchecked", "deprecation"})
        private <T> T parseResponseBody(HttpResponse resp, Class<T> protoBuffFactory) {
            HttpEntity entity = resp.getEntity();
            if (entity != null) {
                T result = null;

                try {
                    Method m = protoBuffFactory.getDeclaredMethod("parseFrom", InputStream.class);
                    result = (T) m.invoke(null, entity.getContent());

                    entity.consumeContent();
                } catch (Exception e) {
                    return null;
                }

                return result;
            }

            return null;
        }
    }

    /**
     * Base class for actions that users can perform.
     */
    private abstract class UserAction {
        protected User mUser;
        protected long mNanos;

        public UserAction(User user) {
            mUser = user;
        }

        public void execute() {
            long startTime = System.nanoTime();
            try {
                doExecute();
            } catch(Exception e) {
            }
            mNanos = System.nanoTime() - startTime;
        }

        public double getElapsedMillis() {
            return mNanos / 1000000.0;
        }

        protected abstract void doExecute() throws Exception;
    }

    private class SimulateStarAction extends UserAction {
        public SimulateStarAction(User user) {
            super(user);
        }

        @Override
        protected void doExecute() throws Exception {
            int numStars = mUser.mHomeSector.getStarsCount();
            int starIndex = mRand.nextInt(numStars);
            Messages.Star star = mUser.mHomeSector.getStars(starIndex);

            mUser.putOrPostProtoBuf(
                    new HttpPost(new URI(mBaseUrl).resolve("stars/"+star.getKey()+"/simulate?update=0")),
                    null, null);
        }
    }
}
