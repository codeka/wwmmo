package au.com.codeka.warworlds.model;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;

public class Colony extends BaseColony implements Parcelable {
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mKey);
        parcel.writeString(mStarKey);
        parcel.writeInt(mPlanetIndex);
        parcel.writeFloat(mPopulation);
        parcel.writeString(mEmpireKey);
        parcel.writeFloat(mFarmingFocus);
        parcel.writeFloat(mConstructionFocus);
        parcel.writeFloat(mPopulationFocus);
        parcel.writeFloat(mMiningFocus);
        parcel.writeFloat(mPopulationDelta);
        parcel.writeFloat(mGoodsDelta);
        parcel.writeFloat(mMineralsDelta);
        parcel.writeFloat(mUncollectedTaxes);
        parcel.writeFloat(mMaxPopulation);
        parcel.writeFloat(mDefenceBoost);
        if (mCooldownTimeEnd == null) {
            parcel.writeLong(0);
        } else {
            parcel.writeLong(mCooldownTimeEnd.getMillis());
        }

        Building[] buildings = new Building[mBuildings.size()];
        parcel.writeParcelableArray(mBuildings.toArray(buildings), flags);
    }

    public static final Parcelable.Creator<Colony> CREATOR
                = new Parcelable.Creator<Colony>() {
        @Override
        public Colony createFromParcel(Parcel parcel) {
            Colony c = new Colony();
            c.mKey = parcel.readString();
            c.mStarKey = parcel.readString();
            c.mPlanetIndex = parcel.readInt();
            c.mPopulation = parcel.readFloat();
            c.mEmpireKey = parcel.readString();
            c.mFarmingFocus = parcel.readFloat();
            c.mConstructionFocus = parcel.readFloat();
            c.mPopulationFocus = parcel.readFloat();
            c.mMiningFocus = parcel.readFloat();
            c.mPopulationDelta = parcel.readFloat();
            c.mGoodsDelta = parcel.readFloat();
            c.mMineralsDelta = parcel.readFloat();
            c.mUncollectedTaxes = parcel.readFloat();
            c.mMaxPopulation = parcel.readFloat();
            c.mDefenceBoost = parcel.readFloat();
            long millis = parcel.readLong();
            if (millis > 0) {
                c.mCooldownTimeEnd = new DateTime(millis, DateTimeZone.UTC);
            }

            Parcelable[] buildings = parcel.readParcelableArray(Building.class.getClassLoader());
            c.mBuildings = new ArrayList<BaseBuilding>();
            for (int i = 0; i < buildings.length; i++) {
                c.mBuildings.add((Building) buildings[i]);
            }

            return c;
        }

        @Override
        public Colony[] newArray(int size) {
            return new Colony[size];
        }
    };
}
