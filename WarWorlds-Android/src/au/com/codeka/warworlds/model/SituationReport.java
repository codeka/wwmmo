package au.com.codeka.warworlds.model;

import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.warworlds.model.BuildRequest.BuildKind;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class SituationReport {
    private String mKey;
    private String mEmpireKey;
    private DateTime mReportTime;
    private String mStarKey;
    private int mPlanetIndex;
    private BuildCompleteRecord mBuildCompleteRecord;
    private MoveCompleteRecord mMoveCompleteRecord;
    private FleetUnderAttackRecord mFleetUnderAttackRecord;
    private FleetDestroyedRecord mFleetDestroyedRecord;
    private FleetVictoriousRecord mFleetVictoriousRecord;

    public String getKey() {
        return mKey;
    }
    public String getEmpireKey() {
        return mEmpireKey;
    }
    public DateTime getReportTime() {
        return mReportTime;
    }
    public String getStarKey() {
        return mStarKey;
    }
    public int getPlanetIndex() {
        return mPlanetIndex;
    }
    public BuildCompleteRecord getBuildCompleteRecord() {
        return mBuildCompleteRecord;
    }
    public MoveCompleteRecord getMoveCompleteRecord() {
        return mMoveCompleteRecord;
    }
    public FleetUnderAttackRecord getFleetUnderAttackRecord() {
        return mFleetUnderAttackRecord;
    }
    public FleetDestroyedRecord getFleetDestroyedRecord() {
        return mFleetDestroyedRecord;
    }
    public FleetVictoriousRecord getFleetVictoriousRecord() {
        return mFleetVictoriousRecord;
    }

    public String getTitle() {
        if (mBuildCompleteRecord != null) {
            return "Build Complete";
        } else if (mMoveCompleteRecord != null) {
            return "Move Complete";
        } else if (mFleetDestroyedRecord != null) {
            return "Fleet Destroyed";
        } else if (mFleetVictoriousRecord != null) {
            return "Fleet Victorious";
        } else if (mFleetUnderAttackRecord != null) {
            return "Fleet Under Attack";
        }

        return "War Worlds";
    }

    public String getDescription(StarSummary starSummary) {
        String msg = "";

        if (mMoveCompleteRecord != null) {
            msg += getFleetLine(mMoveCompleteRecord.getFleetDesignID(), mMoveCompleteRecord.getNumShips());
            msg += String.format(Locale.ENGLISH, " arrived at %s", starSummary.getName());
        }

        if (mBuildCompleteRecord != null) {
            msg = "Construction of ";
            if (mBuildCompleteRecord.getBuildKind().equals(BuildKind.SHIP)) {
                msg += getFleetLine(mBuildCompleteRecord.getDesignID(), 1);
            } else {
                BuildingDesign design = BuildingDesignManager.getInstance().getDesign(mBuildCompleteRecord.getDesignID());
                msg += design.getDisplayName();
            }
            msg += String.format(Locale.ENGLISH, " complete on %s", starSummary.getName());
        }

        if (mFleetUnderAttackRecord != null) {
            if (mMoveCompleteRecord != null) {
                msg += ", and is under attack";
            } else if (mBuildCompleteRecord != null) {
                msg += ", which is under attack";
            } else {
                msg += getFleetLine(mFleetUnderAttackRecord.getFleetDesignID(), mFleetUnderAttackRecord.getNumShips());
                msg += String.format(Locale.ENGLISH, " is under attack at %s", starSummary.getName());
            }
        }

        if (mFleetDestroyedRecord != null) {
            if (mFleetUnderAttackRecord != null) {
                msg += " - it was DESTROYED!";
            } else {
                msg += String.format(Locale.ENGLISH, "%s fleet destroyed on %s",
                        getFleetLine(mFleetDestroyedRecord.getFleetDesignID(), 1),
                        starSummary.getName());
            }
        }

        if (mFleetVictoriousRecord != null) {
            msg += String.format(Locale.ENGLISH, "%s fleet prevailed in battle on %s",
                    getFleetLine(mFleetVictoriousRecord.getFleetDesignID(), mFleetVictoriousRecord.getNumShips()),
                    starSummary.getName());
        }

        if (msg.length() == 0) {
            msg = "We got a situation over here!";
        }

        return msg;
    }

    public Sprite getDesignSprite() {
        String designID = null;
        BuildKind buildKind = BuildKind.SHIP;
        if (mBuildCompleteRecord != null) {
            SituationReport.BuildCompleteRecord bcr = mBuildCompleteRecord;
            buildKind = bcr.getBuildKind();
            designID = bcr.getDesignID();
        } else if (mMoveCompleteRecord != null) {
            designID = mMoveCompleteRecord.getFleetDesignID();
        } else if (mFleetDestroyedRecord != null) {
            designID = mFleetDestroyedRecord.getFleetDesignID();
        } else if (mFleetVictoriousRecord != null) {
            designID = mFleetVictoriousRecord.getFleetDesignID();
        } else if (mFleetUnderAttackRecord != null) {
            designID = mFleetUnderAttackRecord.getFleetDesignID();
        }

        if (designID != null) {
            Design design = DesignManager.getInstance(buildKind).getDesign(designID);
            return design.getSprite();
        } else {
            return null;
        }
    }

    private static String getFleetLine(String designID, float numShips) {
        ShipDesign design = ShipDesignManager.getInstance().getDesign(designID);
        String msg = design.getDisplayName();

        int n = (int)(Math.ceil(numShips));
        if (n > 1) {
            msg += String.format(Locale.ENGLISH, " (× %d)", n);
        }

        return msg;
    }

    public static SituationReport fromProtocolBuffer(Messages.SituationReport pb) {
        SituationReport sitrep = new SituationReport();
        sitrep.mKey = pb.getKey();
        sitrep.mEmpireKey = pb.getEmpireKey();
        sitrep.mReportTime = new DateTime(pb.getReportTime() * 1000, DateTimeZone.UTC);
        sitrep.mStarKey = pb.getStarKey();
        sitrep.mPlanetIndex = pb.getPlanetIndex();

        if (pb.getBuildCompleteRecord() != null &&
            pb.getBuildCompleteRecord().hasDesignId()) {
            sitrep.mBuildCompleteRecord = BuildCompleteRecord.fromProtocolBuffer(pb.getBuildCompleteRecord());
        }

        if (pb.getMoveCompleteRecord() != null &&
            pb.getMoveCompleteRecord().hasFleetKey()) {
            sitrep.mMoveCompleteRecord = MoveCompleteRecord.fromProtocolBuffer(pb.getMoveCompleteRecord());
        }

        if (pb.getFleetUnderAttackRecord() != null &&
            pb.getFleetUnderAttackRecord().hasFleetKey()) {
            sitrep.mFleetUnderAttackRecord = FleetUnderAttackRecord.fromProtocolBuffer(pb.getFleetUnderAttackRecord());
        }

        if (pb.getFleetDestroyedRecord() != null &&
            pb.getFleetDestroyedRecord().hasFleetDesignId()) {
            sitrep.mFleetDestroyedRecord = FleetDestroyedRecord.fromProtocolBuffer(pb.getFleetDestroyedRecord());
        }

        if (pb.getFleetVictoriousRecord() != null &&
            pb.getFleetVictoriousRecord().hasFleetDesignId()) {
            sitrep.mFleetVictoriousRecord = FleetVictoriousRecord.fromProtocolBuffer(pb.getFleetVictoriousRecord());
        }

        return sitrep;
    }

    public static class BuildCompleteRecord {
        private BuildRequest.BuildKind mBuildKind;
        private String mDesignID;

        public BuildRequest.BuildKind getBuildKind() {
            return mBuildKind;
        }
        public String getDesignID() {
            return mDesignID;
        }

        private static BuildCompleteRecord fromProtocolBuffer(Messages.SituationReport.BuildCompleteRecord pb) {
            BuildCompleteRecord bcr = new BuildCompleteRecord();
            bcr.mBuildKind = BuildRequest.BuildKind.fromNumber(pb.getBuildKind().getNumber());
            bcr.mDesignID = pb.getDesignId();
            return bcr;
        }
    }

    public static class MoveCompleteRecord {
        private String mFleetKey;
        private String mFleetDesignID;
        private float mNumShips;
        private String mScoutReportKey;

        public String getFleetKey() {
            return mFleetKey;
        }
        public String getFleetDesignID() {
            return mFleetDesignID;
        }
        public float getNumShips() {
            return mNumShips;
        }
        public String getScoutReportKey() {
            return mScoutReportKey;
        }

        private static MoveCompleteRecord fromProtocolBuffer(Messages.SituationReport.MoveCompleteRecord pb) {
            MoveCompleteRecord mcr = new MoveCompleteRecord();
            mcr.mFleetKey = pb.getFleetKey();
            mcr.mFleetDesignID = pb.getFleetDesignId();
            mcr.mNumShips = pb.getNumShips();
            mcr.mScoutReportKey = pb.getScoutReportKey();
            return mcr;
        }
    }

    public static class FleetUnderAttackRecord {
        private String mFleetKey;
        private String mFleetDesignID;
        private float mNumShips;
        private String mCombatReportKey;

        public String getFleetKey() {
            return mFleetKey;
        }
        public String getFleetDesignID() {
            return mFleetDesignID;
        }
        public float getNumShips() {
            return mNumShips;
        }
        public String getCombatReportKey() {
            return mCombatReportKey;
        }

        private static FleetUnderAttackRecord fromProtocolBuffer(Messages.SituationReport.FleetUnderAttackRecord pb) {
            FleetUnderAttackRecord fuar = new FleetUnderAttackRecord();
            fuar.mFleetKey = pb.getFleetKey();
            fuar.mFleetDesignID = pb.getFleetDesignId();
            fuar.mNumShips = pb.getNumShips();
            fuar.mCombatReportKey = pb.getCombatReportKey();
            return fuar;
        }
    }

    public static class FleetDestroyedRecord {
        private String mFleetDesignID;
        private String mCombatReportKey;

        public String getFleetDesignID() {
            return mFleetDesignID;
        }
        public String getCombatReportKey() {
            return mCombatReportKey;
        }

        private static FleetDestroyedRecord fromProtocolBuffer(Messages.SituationReport.FleetDestroyedRecord pb) {
            FleetDestroyedRecord fdr = new FleetDestroyedRecord();
            fdr.mFleetDesignID = pb.getFleetDesignId();
            fdr.mCombatReportKey = pb.getCombatReportKey();
            return fdr;
        }
    }

    public static class FleetVictoriousRecord {
        private String mFleetKey;
        private String mFleetDesignID;
        private float mNumShips;
        private String mCombatReportKey;

        public String getFleetKey() {
            return mFleetKey;
        }
        public String getFleetDesignID() {
            return mFleetDesignID;
        }
        public float getNumShips() {
            return mNumShips;
        }
        public String getCombatReportKey() {
            return mCombatReportKey;
        }

        private static FleetVictoriousRecord fromProtocolBuffer(Messages.SituationReport.FleetVictoriousRecord pb) {
            FleetVictoriousRecord fvr = new FleetVictoriousRecord();
            fvr.mFleetKey = pb.getFleetKey();
            fvr.mFleetDesignID = pb.getFleetDesignId();
            fvr.mNumShips = pb.getNumShips();
            fvr.mCombatReportKey = pb.getCombatReportKey();
            return fvr;
        }
    }
}
