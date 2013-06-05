package au.com.codeka.warworlds.model;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.warworlds.model.designeffects.RadarBuildingEffect;


/**
 * A star is \i basically a container for planets. It shows up on the starfield list.
 */
public class Star extends StarSummary {
    private Float mRadarRange;

    public static final Parcelable.Creator<Star> CREATOR
                = new Parcelable.Creator<Star>() {
        @Override
        public Star createFromParcel(Parcel parcel) {
            Star s = new Star();
            s.populateFromParcel(parcel);
            return s;
        }

        @Override
        public Star[] newArray(int size) {
            return new Star[size];
        }
    };

    /**
     * If this star has a radar building on it for the given empire, we'll return the range
     * (in parsecs) of the radar. If they don't have one, we'll return 0.
     */
    public float getRadarRange(String empireKey) {
        if (mRadarRange != null) {
            return (float) mRadarRange;
        }

        mRadarRange = 0.0f;
        for (BaseColony baseColony : getColonies()) {
            if (baseColony.getEmpireKey() == null) {
                continue;
            }
            if (baseColony.getEmpireKey().equals(empireKey)) {
                for (BaseBuilding baseBuilding : baseColony.getBuildings()) {
                    Building building = (Building) baseBuilding;
                    BuildingDesign design = building.getDesign();
                    for (RadarBuildingEffect effect : design.getEffects(building.getLevel(), RadarBuildingEffect.class)) {
                        if (mRadarRange < effect.getRange()) {
                            mRadarRange = effect.getRange();
                        }
                    }
                }
            }
        }

        return mRadarRange;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeLong(mLastSimulation.getMillis());

        Colony colonies[] = new Colony[mColonies.size()];
        mColonies.toArray(colonies);
        parcel.writeParcelableArray(colonies, flags);

        EmpirePresence empires[] = new EmpirePresence[mEmpires.size()];
        mEmpires.toArray(empires);
        parcel.writeParcelableArray(empires, flags);

        Fleet fleets[] = new Fleet[mFleets.size()];
        mFleets.toArray(fleets);
        parcel.writeParcelableArray(fleets, flags);

        BuildRequest buildRequests[] = new BuildRequest[mBuildRequests.size()];
        mBuildRequests.toArray(buildRequests);
        parcel.writeParcelableArray(buildRequests, flags);
    }

    protected void populateFromParcel(Parcel parcel) {
        super.populateFromParcel(parcel);
        mLastSimulation = new DateTime(parcel.readLong(), DateTimeZone.UTC);

        Parcelable[] colonies = parcel.readParcelableArray(Colony.class.getClassLoader());
        mColonies = new ArrayList<BaseColony>();
        for (int i = 0; i < colonies.length; i++) {
            mColonies.add((Colony) colonies[i]);
        }

        Parcelable[] empires = parcel.readParcelableArray(EmpirePresence.class.getClassLoader());
        mEmpires = new ArrayList<BaseEmpirePresence>();
        for (int i = 0; i < empires.length; i++) {
            mEmpires.add((EmpirePresence) empires[i]);
        }

        Parcelable[] fleets = parcel.readParcelableArray(Fleet.class.getClassLoader());
        mFleets = new ArrayList<BaseFleet>();
        for (int i = 0; i < fleets.length; i++) {
            mFleets.add((Fleet) fleets[i]);
        }

        Parcelable[] buildRequests = parcel.readParcelableArray(BuildRequest.class.getClassLoader());
        mBuildRequests = new ArrayList<BaseBuildRequest>();
        for (int i = 0; i < buildRequests.length; i++) {
            mBuildRequests.add((BuildRequest) buildRequests[i]);
        }
    }
}
