package au.com.codeka.warworlds.model;


public class Empire {
    private String mKey;
    private String mDisplayName;

    public String getKey() {
        return mKey;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public static Empire fromProtocolBuffer(warworlds.Warworlds.Empire pb) {
        Empire empire = new Empire();
        populateFromProtocolBuffer(pb, empire);
        return empire;
    }

    protected static void populateFromProtocolBuffer(warworlds.Warworlds.Empire pb, Empire empire) {
        empire.mKey = pb.getKey();
        empire.mDisplayName = pb.getDisplayName();
    }
}
