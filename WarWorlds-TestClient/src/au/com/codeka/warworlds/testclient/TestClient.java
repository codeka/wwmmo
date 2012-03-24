package au.com.codeka.warworlds.testclient;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;

import warworlds.Warworlds.Hello;

import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.api.ChannelClient;
import au.com.codeka.warworlds.api.ClientLoginAuthenticator;
import au.com.codeka.warworlds.api.DevServerAuthenticator;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.api.ChannelClient.ChannelListener;

public class TestClient {
    public static void main(String[] args) {
        try {
            URI uri = configureApiClient(false);

            //Hello hello_pb = ApiClient.putProtoBuf("hello/ahBkZXZ-d2Fyd29ybGRzbW1vchgLEhJEZXZpY2VSZWdpc3RyYXRpb24YDAw", null, Hello.class);
            Hello hello_pb = ApiClient.putProtoBuf("hello/ag5zfndhcndvcmxkc21tb3IaCxISRGV2aWNlUmVnaXN0cmF0aW9uGIukAQw", null, Hello.class);
            String token = hello_pb.getChannelToken();
            System.out.println("Token: "+token);

            ChannelClient cc = ChannelClient.createChannel(uri.resolve("/"), token, new ChannelListener() {
                @Override
                public void onOpen() {
                    System.out.println("* onOpen()");
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("> "+message);
                }

                @Override
                public void onError(int code, String description) {
                    System.out.println("* onError("+code+", \""+description+"\")");
                }

                @Override
                public void onClose() {
                    System.out.println("* onClose()");
                }
            });

            cc.open();
            System.out.println("Press any key to quit...");
            System.in.read();
            cc.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static URI configureApiClient(boolean devServer) throws URISyntaxException, ApiException {
        String cookie = null;
        URI uri = null;
        if (devServer) {
            uri = new URI("http://localhost:8271/api/v1/");
            ApiClient.configure(uri);
            cookie = DevServerAuthenticator.authenticate("warworldstest2@gmail.com", false);
        } else {
            uri = new URI("https://warworldsmmo.appspot.com/api/v1/");
            ApiClient.configure(uri);
            cookie = ClientLoginAuthenticator.authenticate("warworldstest1@gmail.com", "adv18997");
        }

        ApiClient.getCookies().clear();
        ApiClient.getCookies().add(cookie);

        RequestManager.addResponseReceivedHandler(new RequestManager.ResponseReceivedHandler() {
            private void dump(AbstractHttpMessage msg) {
                System.out.println("      DUMP       ");
                if (msg instanceof BasicHttpRequest) {
                    System.out.println(((BasicHttpRequest) msg).getRequestLine().toString());
                } else if (msg instanceof BasicHttpResponse) {
                    System.out.println(((BasicHttpResponse) msg).getStatusLine().toString());
                }
                for (Header h : msg.getAllHeaders()) {
                    System.out.println(h.getName()+": "+h.getValue());
                }
            }

            @Override
            public void onResponseReceived(BasicHttpRequest request, BasicHttpResponse response) {
                // if we get an error we just want to dump it out
                if (response.getStatusLine().getStatusCode() > 299) {
                    dump(request);
                    dump(response);
                }
            }
        });

        return uri;
    }
}
