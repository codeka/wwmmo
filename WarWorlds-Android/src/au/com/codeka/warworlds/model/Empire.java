package au.com.codeka.warworlds.model;

import android.os.Parcel;
import android.os.Parcelable;


public class Empire implements Parcelable {
    private String mKey;
    private String mDisplayName;

    public String getKey() {
        return mKey;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mKey);
        parcel.writeString(mDisplayName);
    }

    protected void readFromParcel(Parcel parcel) {
        mKey = parcel.readString();
        mDisplayName = parcel.readString();
    }

    public static final Parcelable.Creator<Empire> CREATOR
                = new Parcelable.Creator<Empire>() {
        @Override
        public Empire createFromParcel(Parcel parcel) {
            Empire e = new Empire();
            e.readFromParcel(parcel);
            return e;
        }

        @Override
        public Empire[] newArray(int size) {
            return new Empire[size];
        }
    };

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
