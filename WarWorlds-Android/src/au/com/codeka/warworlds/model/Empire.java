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
        empire.populateFromProtocolBuffer(pb);
        return empire;
    }

    protected void populateFromProtocolBuffer(warworlds.Warworlds.Empire pb) {
        mKey = pb.getKey();
        mDisplayName = pb.getDisplayName();
    }
}
