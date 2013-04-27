package au.com.codeka.warworlds.model;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseDesignManager;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.common.model.DesignKind;

/**
 * Represents a single building on a colony.
 */
public class Building extends BaseBuilding implements Parcelable {
    public BuildingDesign getDesign() {
        return (BuildingDesign) BaseDesignManager.i.getDesign(DesignKind.BUILDING, mDesignID);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mKey);
        parcel.writeString(mColonyKey);
        parcel.writeString(mDesignID);
        parcel.writeInt(mLevel);
    }

    public static final Parcelable.Creator<Building> CREATOR
                = new Parcelable.Creator<Building>() {
        @Override
        public Building createFromParcel(Parcel parcel) {
            Building b = new Building();
            b.mKey = parcel.readString();
            b.mColonyKey = parcel.readString();
            b.mDesignID = parcel.readString();
            b.mLevel = parcel.readInt();
            return b;
        }

        @Override
        public Building[] newArray(int size) {
            return new Building[size];
        }
    };
}
