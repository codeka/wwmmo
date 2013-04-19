package au.com.codeka.warworlds.model;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.common.model.BasePlanet;


public class Planet extends BasePlanet
                    implements Parcelable {

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mIndex);
        parcel.writeInt(mPlanetType.getIndex());
        parcel.writeInt(mSize);
        parcel.writeInt(mPopulationCongeniality);
        parcel.writeInt(mFarmingCongeniality);
        parcel.writeInt(mMiningCongeniality);
    }

    public static final Parcelable.Creator<Planet> CREATOR
                = new Parcelable.Creator<Planet>() {
        @Override
        public Planet createFromParcel(Parcel parcel) {
            Planet p = new Planet();
            p.mIndex = parcel.readInt();
            p.mPlanetType = sPlanetTypes[parcel.readInt()];
            p.mSize = parcel.readInt();
            p.mPopulationCongeniality = parcel.readInt();
            p.mFarmingCongeniality = parcel.readInt();
            p.mMiningCongeniality = parcel.readInt();
            return p;
        }

        @Override
        public Planet[] newArray(int size) {
            return new Planet[size];
        }
    };
}
