package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.warworlds.model.protobuf.Messages;


/**
 * A star is \i basically a container for planets. It shows up on the starfield list.
 */
public class Star extends StarSummary {
    private static Logger log = LoggerFactory.getLogger(Star.class);
    private ArrayList<Colony> mColonies;
    private ArrayList<EmpirePresence> mEmpires;
    private ArrayList<Fleet> mFleets;
    private ArrayList<BuildRequest> mBuildRequests;
    private DateTime mLastSimulation;

    public Star() {
        mColonies = null;
        mEmpires = null;
        mFleets = null;
        mBuildRequests = null;
    }

    public DateTime getLastSimulation() {
        return mLastSimulation;
    }

    public List<Colony> getColonies() {
        return mColonies;
    }
    public List<EmpirePresence> getEmpires() {
        List<EmpirePresence> empires = new ArrayList<EmpirePresence>();
        // note: we make sure there's actually a colony for that empire before we return it
        for (EmpirePresence empire : mEmpires) {
            for (Colony colony : mColonies) {
                if (colony.getEmpireKey() != null && colony.getEmpireKey().equals(empire.getEmpireKey())) {
                    empires.add(empire);
                    break;
                }
            }
        }

        return empires;
    }
    public EmpirePresence getEmpire(String empireKey) {
        for (EmpirePresence ep : mEmpires) {
            if (ep.getEmpireKey().equals(empireKey)) {
                return ep;
            }
        }
        return null;
    }
    public List<Fleet> getFleets() {
        return mFleets;
    }
    public List<BuildRequest> getBuildRequests() {
        return mBuildRequests;
    }

    public void addColony(Colony colony) {
        if (mColonies == null) {
            mColonies = new ArrayList<Colony>();
        }
        mColonies.add(colony);
    }

    public void addFleet(Fleet fleet) {
        if (mFleets == null) {
            mFleets = new ArrayList<Fleet>();
        }
        mFleets.add(fleet);
    }

    public Fleet findFleet(String fleetKey) {
        for (Fleet f : mFleets) {
            if (f.getKey().equals(fleetKey)) {
                return f;
            }
        }
        return null;
    }

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
        mColonies = new ArrayList<Colony>();
        for (int i = 0; i < colonies.length; i++) {
            mColonies.add((Colony) colonies[i]);
        }

        Parcelable[] empires = parcel.readParcelableArray(EmpirePresence.class.getClassLoader());
        mEmpires = new ArrayList<EmpirePresence>();
        for (int i = 0; i < empires.length; i++) {
            mEmpires.add((EmpirePresence) empires[i]);
        }

        Parcelable[] fleets = parcel.readParcelableArray(Fleet.class.getClassLoader());
        mFleets = new ArrayList<Fleet>();
        for (int i = 0; i < fleets.length; i++) {
            mFleets.add((Fleet) fleets[i]);
        }

        Parcelable[] buildRequests = parcel.readParcelableArray(BuildRequest.class.getClassLoader());
        mBuildRequests = new ArrayList<BuildRequest>();
        for (int i = 0; i < buildRequests.length; i++) {
            mBuildRequests.add((BuildRequest) buildRequests[i]);
        }
    }

    @Override
    public void populateFromProtocolBuffer(Messages.Star pb) {
        super.populateFromProtocolBuffer(pb);

        if (pb.getLastSimulation() == 0) {
            mLastSimulation = null;
        } else {
            mLastSimulation = new DateTime(pb.getLastSimulation() * 1000, DateTimeZone.UTC);
        }

        mColonies = new ArrayList<Colony>();
        for(Messages.Colony colony_pb : pb.getColoniesList()) {
            if (colony_pb.getPopulation() < 1.0) {
                // colonies with zero population are dead -- they just don't
                // know it yet.
                continue;
            }
            Colony c = Colony.fromProtocolBuffer(colony_pb);

            for (int i = 0; i < pb.getBuildingsCount(); i++) {
                Messages.Building bpb = pb.getBuildings(i);
                if (bpb.getColonyKey().equals(c.getKey())) {
                    log.info("Adding building: " + bpb.getDesignName());
                    c.getBuildings().add(Building.fromProtocolBuffer(bpb));
                }
            }

            mColonies.add(c);
        }

        mEmpires = new ArrayList<EmpirePresence>();
        for (Messages.EmpirePresence empirePresencePb : pb.getEmpiresList()) {
            mEmpires.add(EmpirePresence.fromProtocolBuffer(empirePresencePb));
        }

        mBuildRequests = new ArrayList<BuildRequest>();
        for (Messages.BuildRequest buildRequestPb : pb.getBuildRequestsList()) {
            mBuildRequests.add(BuildRequest.fromProtocolBuffer(buildRequestPb));
        }

        mFleets = new ArrayList<Fleet>();
        for (Messages.Fleet fleetPb : pb.getFleetsList()) {
            mFleets.add(Fleet.fromProtocolBuffer(fleetPb));
        }
    }

    /**
     * Converts the given Star protocol buffer into a \c Star.
     */
    public static Star fromProtocolBuffer(Messages.Star pb) {
        Star s = new Star();
        s.populateFromProtocolBuffer(pb);
        return s;
    }
}
