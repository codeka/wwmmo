package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

public class CombatReport {
    private String mKey;
    private String mStarKey;
    private DateTime mStartTime;
    private DateTime mEndTime;
    private List<String> mStartEmpires;
    private List<String> mEndEmpires;
    private int mTotalDestroyed;
    private List<CombatRound> mCombatRounds;

    public String getKey() {
        return mKey;
    }
    public String getStarKey() {
        return mStarKey;
    }
    public DateTime getStartTime() {
        return mStartTime;
    }
    public DateTime getEndTime() {
        return mEndTime;
    }
    public List<String> getStartEmpires() {
        return mStartEmpires;
    }
    public List<String> getEndEmpires() {
        return mEndEmpires;
    }
    public int getTotalDestroyed() {
        return mTotalDestroyed;
    }
    public List<CombatRound> getCombatRounds() {
        return mCombatRounds;
    }

    public static CombatReport fromProtocolBuffer(Messages.CombatReport pb) {
        CombatReport combatReport = new CombatReport();
        combatReport.mKey = pb.getKey();
        combatReport.mStarKey = pb.getStarKey();
        combatReport.mStartTime = new DateTime(pb.getStartTime() * 1000, DateTimeZone.UTC);
        combatReport.mEndTime = new DateTime(pb.getEndTime() * 1000, DateTimeZone.UTC);
        // TODO: mStartEmpires
        // TODO: mEndEmpires
        combatReport.mTotalDestroyed = pb.getNumDestroyed();
        combatReport.mCombatRounds = new ArrayList<CombatRound>();
        for (Messages.CombatRound crpb : pb.getRoundsList()) {
            CombatRound combatRound = CombatRound.fromProtocolBuffer(crpb);
            combatReport.mCombatRounds.add(combatRound);
        }
        return combatReport;
    }

    public static class CombatRound {
        private String mStarKey;
        private DateTime mRoundTime;
        private List<FleetSummary> mFleets;
        private List<FleetJoinedRecord> mFleetJoinedRecords;
        private List<FleetTargetRecord> mFleetTargetRecords;
        private List<FleetAttackRecord> mFleetAttackRecords;
        private List<FleetDamagedRecord> mFleetDamagedRecords;

        public String getStarKey() {
            return mStarKey;
        }
        public DateTime getRoundTime() {
            return mRoundTime;
        }
        public List<FleetSummary> getFleets() {
            return mFleets;
        }
        public List<FleetJoinedRecord> getFleetJoinedRecords() {
            return mFleetJoinedRecords;
        }
        public List<FleetTargetRecord> getFleetTargetRecords() {
            return mFleetTargetRecords;
        }
        public List<FleetAttackRecord> getFleetAttackRecords() {
            return mFleetAttackRecords;
        }
        public List<FleetDamagedRecord> getFleetDamagedRecords() {
            return mFleetDamagedRecords;
        }

        public static CombatRound fromProtocolBuffer(Messages.CombatRound pb) {
            CombatRound combatRound = new CombatRound();
            combatRound.mStarKey = pb.getStarKey();
            combatRound.mRoundTime = new DateTime(pb.getRoundTime() * 1000, DateTimeZone.UTC);
            combatRound.mFleets = new ArrayList<FleetSummary>();
            for (Messages.CombatRound.FleetSummary fspb : pb.getFleetsList()) {
                combatRound.mFleets.add(FleetSummary.fromProtocolBuffer(fspb));
            }
            combatRound.mFleetJoinedRecords = new ArrayList<FleetJoinedRecord>();
            for (Messages.CombatRound.FleetJoinedRecord fjrpb : pb.getFleetsJoinedList()) {
                combatRound.mFleetJoinedRecords.add(FleetJoinedRecord.fromProtocolBuffer(combatRound.mFleets, fjrpb));
            }
            combatRound.mFleetTargetRecords = new ArrayList<FleetTargetRecord>();
            for (Messages.CombatRound.FleetTargetRecord ftrpb : pb.getFleetsTargettedList()) {
                combatRound.mFleetTargetRecords.add(FleetTargetRecord.fromProtocolBuffer(combatRound.mFleets, ftrpb));
            }
            combatRound.mFleetAttackRecords = new ArrayList<FleetAttackRecord>();
            for (Messages.CombatRound.FleetAttackRecord farpb : pb.getFleetsAttackedList()) {
                combatRound.mFleetAttackRecords.add(FleetAttackRecord.fromProtocolBuffer(combatRound.mFleets, farpb));
            }
            combatRound.mFleetDamagedRecords = new ArrayList<FleetDamagedRecord>();
            for (Messages.CombatRound.FleetDamagedRecord fdrpb : pb.getFleetsDamagedList()) {
                combatRound.mFleetDamagedRecords.add(FleetDamagedRecord.fromProtocolBuffer(combatRound.mFleets, fdrpb));
            }
            return combatRound;
        }
    }

    public static class FleetSummary {
        private String mFleetKey;
        private String mEmpireKey;
        private String mDesignID;
        private float mNumShips;

        public String getFleetKey() {
            return mFleetKey;
        }
        public String getEmpireKey() {
            return mEmpireKey;
        }
        public String getDesignID() {
            return mDesignID;
        }
        public float getNumShips() {
            return mNumShips;
        }

        public static FleetSummary fromProtocolBuffer(Messages.CombatRound.FleetSummary pb) {
            FleetSummary fleetSummary = new FleetSummary();
            fleetSummary.mFleetKey = pb.getFleetKey();
            fleetSummary.mEmpireKey = pb.getEmpireKey();
            fleetSummary.mDesignID = pb.getDesignId();
            fleetSummary.mNumShips = pb.getNumShips();
            return fleetSummary;
        }
    }

    // This record is used when a fleet enters combat.
    public static class FleetJoinedRecord {
        private List<FleetSummary> mFleets;
        private int mFleetIndex;

        public FleetJoinedRecord(List<FleetSummary> fleets) {
            mFleets = fleets;
        }

        public FleetSummary getFleet() {
            return mFleets.get(mFleetIndex);
        }

        public static FleetJoinedRecord fromProtocolBuffer(List<FleetSummary> fleets,
                                                           Messages.CombatRound.FleetJoinedRecord pb) {
            FleetJoinedRecord fjr = new FleetJoinedRecord(fleets);
            fjr.mFleetIndex = pb.getFleetIndex();
            return fjr;
        }
    }

    // This record is used when a fleet choose a target.
    public static class FleetTargetRecord {
        private List<FleetSummary> mFleets;
        private int mFleetIndex;
        private int mTargetIndex;

        public FleetTargetRecord(List<FleetSummary> fleets) {
            mFleets = fleets;
        }

        public FleetSummary getFleet() {
            return mFleets.get(mFleetIndex);
        }
        public FleetSummary getTarget() {
            return mFleets.get(mTargetIndex);
        }

        public static FleetTargetRecord fromProtocolBuffer(List<FleetSummary> fleets,
                                                           Messages.CombatRound.FleetTargetRecord pb) {
            FleetTargetRecord ftr = new FleetTargetRecord(fleets);
            ftr.mFleetIndex = pb.getFleetIndex();
            ftr.mTargetIndex = pb.getTargetIndex();
            return ftr;
        }
    }

    // This record is used when a fleet attacks another fleet
    public static class FleetAttackRecord {
        private List<FleetSummary> mFleets;
        private int mFleetIndex;
        private int mTargetIndex;
        private float mDamage;

        public FleetAttackRecord(List<FleetSummary> fleets) {
            mFleets = fleets;
        }

        public FleetSummary getFleet() {
            return mFleets.get(mFleetIndex);
        }
        public FleetSummary getTarget() {
            return mFleets.get(mTargetIndex);
        }
        public float getDamage() {
            return mDamage;
        }

        public static FleetAttackRecord fromProtocolBuffer(List<FleetSummary> fleets,
                                                           Messages.CombatRound.FleetAttackRecord pb) {
            FleetAttackRecord far = new FleetAttackRecord(fleets);
            far.mFleetIndex = pb.getFleetIndex();
            far.mTargetIndex = pb.getTargetIndex();
            far.mDamage = pb.getDamage();
            return far;
        }
    }

    // This record is used to record the damage done to a fleet
    public static class FleetDamagedRecord {
        private List<FleetSummary> mFleets;
        private int mFleetIndex;
        private float mDamage;

        public FleetDamagedRecord(List<FleetSummary> fleets) {
            mFleets = fleets;
        }

        public FleetSummary getFleet() {
            return mFleets.get(mFleetIndex);
        }
        public float getDamage() {
            return mDamage;
        }

        public static FleetDamagedRecord fromProtocolBuffer(List<FleetSummary> fleets,
                                                            Messages.CombatRound.FleetDamagedRecord pb) {
            FleetDamagedRecord fdr = new FleetDamagedRecord(fleets);
            fdr.mFleetIndex = pb.getFleetIndex();
            fdr.mDamage = pb.getDamage();
            return fdr;
        }
    }
}
