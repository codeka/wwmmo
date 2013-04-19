package au.com.codeka.warworlds.model;

import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.common.protobuf.Messages;

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
    private ColonyDestroyedRecord mColonyDestroyedRecord;

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
    public ColonyDestroyedRecord getColonyDestroyedRecord() {
        return mColonyDestroyedRecord;
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
        } else if (mColonyDestroyedRecord != null) {
            return "Colony Destroyed";
        }

        return "War Worlds";
    }

    /**
     * Gets an HTML summary of this notification, useful for notification messages.
     */
    public String getSummaryLine(StarSummary starSummary) {
        String msg;
        if (mPlanetIndex >= 0) {
            msg = String.format(Locale.ENGLISH, "<b>%s %s:</b>",
                                starSummary.getName(), RomanNumeralFormatter.format(mPlanetIndex));
        } else {
            msg = String.format(Locale.ENGLISH, "<b>%s:</b> ", starSummary.getName());
        }

        if (mMoveCompleteRecord != null) {
            msg += getFleetLine(mMoveCompleteRecord.getFleetDesignID(), mMoveCompleteRecord.getNumShips());
            msg += " arrived";
        }

        if (mBuildCompleteRecord != null) {
            if (mBuildCompleteRecord.getDesignKind().equals(DesignKind.SHIP)) {
                msg += getFleetLine(mBuildCompleteRecord.getDesignID(), 1);
            } else {
                Design design = DesignManager.i.getDesign(DesignKind.BUILDING, mBuildCompleteRecord.getDesignID());
                msg += design.getDisplayName();
            }
            msg += " built";
        }

        if (mFleetUnderAttackRecord != null) {
            if (mMoveCompleteRecord != null || mBuildCompleteRecord != null) {
                msg += ", and attacked!";
            } else {
                msg += getFleetLine(mFleetUnderAttackRecord.getFleetDesignID(), mFleetUnderAttackRecord.getNumShips());
                msg += " attacked!";
            }
        }

        if (mFleetDestroyedRecord != null) {
            if (mFleetUnderAttackRecord != null) {
                msg += " and <i>destroyed</i>";
            } else {
                msg += String.format(Locale.ENGLISH, "%s <i>destroyed</i>",
                        getFleetLine(mFleetDestroyedRecord.getFleetDesignID(), 1));
            }
        }

        if (mFleetVictoriousRecord != null) {
            msg += String.format(Locale.ENGLISH, "%s <i>victorious</i>",
                    getFleetLine(mFleetVictoriousRecord.getFleetDesignID(), mFleetVictoriousRecord.getNumShips()));
        }

        if (mColonyDestroyedRecord != null) {
            msg += "Colony <em>destroyed!</em>";
        }

        if (msg.length() == 0) {
            msg = "Unknown situation";
        }

        return msg;
    }

    public String getDescription(StarSummary starSummary) {
        String msg = "";

        if (mMoveCompleteRecord != null) {
            msg += getFleetLine(mMoveCompleteRecord.getFleetDesignID(), mMoveCompleteRecord.getNumShips());
            msg += String.format(Locale.ENGLISH, " arrived at %s", starSummary.getName());
        }

        if (mBuildCompleteRecord != null) {
            msg = "Construction of ";
            if (mBuildCompleteRecord.getDesignKind().equals(DesignKind.SHIP)) {
                msg += getFleetLine(mBuildCompleteRecord.getDesignID(), 1);
            } else {
                Design design = DesignManager.i.getDesign(DesignKind.BUILDING, mBuildCompleteRecord.getDesignID());
                msg += design.getDisplayName();
            }
            msg += String.format(Locale.ENGLISH, " complete on %s %s",
                    starSummary.getName(), RomanNumeralFormatter.format(mPlanetIndex));
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

        if (mColonyDestroyedRecord != null) {
            msg += String.format(Locale.ENGLISH, "Colony on %s %s destroyed",
                    starSummary.getName(), RomanNumeralFormatter.format(mPlanetIndex));
        }

        if (msg.length() == 0) {
            msg = "We got a situation over here!";
        }

        return msg;
    }

    public Sprite getDesignSprite() {
        String designID = null;
        DesignKind designKind = DesignKind.SHIP;
        if (mBuildCompleteRecord != null) {
            SituationReport.BuildCompleteRecord bcr = mBuildCompleteRecord;
            designKind = bcr.getDesignKind();
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
            Design design = DesignManager.i.getDesign(designKind, designID);
            return SpriteManager.i.getSprite(design.getSpriteName());
        } else {
            return null;
        }
    }

    private static String getFleetLine(String designID, float numShips) {
        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, designID);
        int n = (int)(Math.ceil(numShips));
        return String.format(Locale.ENGLISH, "%d × %s",
                n, design.getDisplayName(n > 1));
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

        if (pb.getColonyDestroyedRecord() != null &&
            pb.getColonyDestroyedRecord().hasColonyKey()) {
            sitrep.mColonyDestroyedRecord = ColonyDestroyedRecord.fromProtocolBuffer(pb.getColonyDestroyedRecord());
        }

        return sitrep;
    }

    public static class BuildCompleteRecord {
        private DesignKind mDesignKind;
        private String mDesignID;

        public DesignKind getDesignKind() {
            return mDesignKind;
        }
        public String getDesignID() {
            return mDesignID;
        }

        private static BuildCompleteRecord fromProtocolBuffer(Messages.SituationReport.BuildCompleteRecord pb) {
            BuildCompleteRecord bcr = new BuildCompleteRecord();
            bcr.mDesignKind = DesignKind.fromNumber(pb.getBuildKind().getNumber());
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

    public static class ColonyDestroyedRecord {
        private String mColonyKey;
        private String mEnemyEmpireKey;

        public String getColonyKey() {
            return mColonyKey;
        }
        public String getEnemyEmpireKey() {
            return mEnemyEmpireKey;
        }

        private static ColonyDestroyedRecord fromProtocolBuffer(Messages.SituationReport.ColonyDestroyedRecord pb) {
            ColonyDestroyedRecord cdr = new ColonyDestroyedRecord();
            cdr.mColonyKey = pb.getColonyKey();
            cdr.mEnemyEmpireKey = pb.getEnemyEmpireKey();
            if (cdr.mEnemyEmpireKey != null && cdr.mEnemyEmpireKey.length() == 0) {
                cdr.mEnemyEmpireKey = null;
            }
            return cdr;
        }
    }
}
