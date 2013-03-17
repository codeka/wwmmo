package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class Alliance implements Parcelable {
    private String mKey;
    private String mName;
    private DateTime mTimeCreated;
    private String mCreatorEmpireKey;
    private int mNumMembers;

    public String getKey() {
        return mKey;
    }
    public String getName() {
        return mName;
    }
    public DateTime getTimeCreated() {
        return mTimeCreated;
    }
    public String getCreatorEmpireKey() {
        return mCreatorEmpireKey;
    }
    public int getNumMembers() {
        return mNumMembers;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mKey);
        parcel.writeString(mName);
        parcel.writeLong(mTimeCreated.getMillis());
        parcel.writeString(mCreatorEmpireKey);
        parcel.writeInt(mNumMembers);
    }

    protected void readFromParcel(Parcel parcel) {
        mKey = parcel.readString();
        mName = parcel.readString();
        mTimeCreated = new DateTime(parcel.readLong(), DateTimeZone.UTC);
        mCreatorEmpireKey = parcel.readString();
        mNumMembers = parcel.readInt();
    }

    public static final Parcelable.Creator<Alliance> CREATOR
                = new Parcelable.Creator<Alliance>() {
        @Override
        public Alliance createFromParcel(Parcel parcel) {
            Alliance a = new Alliance();
            a.readFromParcel(parcel);
            return a;
        }

        @Override
        public Alliance[] newArray(int size) {
            return new Alliance[size];
        }
    };

    public static Alliance fromProtocolBuffer(Messages.Alliance pb) {
        Alliance alliance = new Alliance();
        alliance.mKey = pb.getKey();
        alliance.mName = pb.getName();
        alliance.mTimeCreated = new DateTime(pb.getTimeCreated() * 1000, DateTimeZone.UTC);
        alliance.mCreatorEmpireKey = pb.getCreatorEmpireKey();
        alliance.mNumMembers = pb.getNumMembers();
        return alliance;
    }
}
