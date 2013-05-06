package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.common.model.BaseAlliance;
import au.com.codeka.common.model.BaseAllianceMember;
import au.com.codeka.common.protobuf.Messages;

public class Alliance extends BaseAlliance implements Parcelable {

    @Override
    protected BaseAllianceMember createAllianceMember(Messages.AllianceMember pb) {
        AllianceMember am = new AllianceMember();
        am.fromProtocolBuffer(pb);
        return am;
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
        //todo: members?
    }

    protected void readFromParcel(Parcel parcel) {
        mKey = parcel.readString();
        mName = parcel.readString();
        mTimeCreated = new DateTime(parcel.readLong(), DateTimeZone.UTC);
        mCreatorEmpireKey = parcel.readString();
        mNumMembers = parcel.readInt();
        //todo: members?
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
}
