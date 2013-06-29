package au.com.codeka.warworlds.server.ctrl;

public class RealmController {
    private static String sRealmName;

    static {
        sRealmName = System.getProperty("au.com.codeka.warworlds.server.realmName");
        if (sRealmName == null) {
            sRealmName = "Beta";
        }
    }

    public String getRealmName() {
        return sRealmName;
    }
}
