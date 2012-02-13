package au.com.codeka.warworlds.testclient;

import java.net.URI;
import java.net.URISyntaxException;

import warworlds.Warworlds.MessageOfTheDay;
import au.com.codeka.warworlds.api.ApiClient;

public class TestClient {
    public static void main(String[] args) {
        try {
            ApiClient.configure(new URI("http://localhost:8271/api/v1/"));
            //ApiClient.configure(new URI("https://warworldsmmo.appspot.com/api/v1/"));

            MessageOfTheDay motd = ApiClient.getProtoBuf("motd", MessageOfTheDay.class);
            System.out.println(motd.getMessage());

            motd = ApiClient.getProtoBuf("motd", MessageOfTheDay.class);
            System.out.println(motd.getMessage());
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
