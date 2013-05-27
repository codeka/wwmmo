package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

public class BaseCombatReport {
    protected String mKey;
    protected String mStarKey;
    protected DateTime mStartTime;
    protected DateTime mEndTime;
    protected List<String> mStartEmpires;
    protected List<String> mEndEmpires;
    protected int mTotalDestroyed;
    protected List<CombatRound> mCombatRounds;

    public BaseCombatReport() {
        mCombatRounds = new ArrayList<CombatRound>();
    }

    public String getKey() {
        return mKey;
    }
    public String getStarKey() {
        return mStarKey;
    }
    public DateTime getStartTime() {
        return mStartTime;
    }
    public void setStartTime(DateTime startTime) {
        mStartTime = startTime;
    }
    public DateTime getEndTime() {
        return mEndTime;
    }
    public void setEndTime(DateTime endTime) {
        mEndTime = endTime;
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

    public void fromProtocolBuffer(Messages.CombatReport pb) {
        if (pb.hasKey()) {
            mKey = pb.getKey();
        }
        mStarKey = pb.getStarKey();
        mStartTime = new DateTime(pb.getStartTime() * 1000, DateTimeZone.UTC);
        mEndTime = new DateTime(pb.getEndTime() * 1000, DateTimeZone.UTC);
        // TODO: mStartEmpires
        // TODO: mEndEmpires
        mTotalDestroyed = pb.getNumDestroyed();
        for (Messages.CombatRound crpb : pb.getRoundsList()) {
            CombatRound combatRound = CombatRound.fromProtocolBuffer(crpb);
            mCombatRounds.add(combatRound);
        }
    }

    public void toProtocolBuffer(Messages.CombatReport.Builder pb) {
        if (mKey != null) {
            pb.setKey(mKey);
        }
        pb.setStarKey(mStarKey);
        if (mStartTime != null) {
            pb.setStartTime(mStartTime.getMillis() / 1000);
        }
        if (mEndTime != null) {
            pb.setEndTime(mEndTime.getMillis() / 1000);
        }
        // TODO: mStartEmpires
        // TODO: mEndEmpires
        pb.setNumDestroyed(mTotalDestroyed);
        for (CombatRound round : mCombatRounds) {
            Messages.CombatRound.Builder combat_round_pb = Messages.CombatRound.newBuilder();
            round.toProtocolBuffer(combat_round_pb);
            pb.addRounds(combat_round_pb.build());
        }
    }

    public static class CombatRound {
        private String mStarKey;
        private DateTime mRoundTime;
        private List<FleetSummary> mFleets;
        private List<FleetJoinedRecord> mFleetJoinedRecords;
        private List<FleetTargetRecord> mFleetTargetRecords;
        private List<FleetAttackRecord> mFleetAttackRecords;
        private List<FleetDamagedRecord> mFleetDamagedRecords;

        public CombatRound() {
            mFleets = new ArrayList<FleetSummary>();
            mFleetJoinedRecords = new ArrayList<FleetJoinedRecord>();
            mFleetTargetRecords = new ArrayList<FleetTargetRecord>();
            mFleetAttackRecords = new ArrayList<FleetAttackRecord>();
            mFleetDamagedRecords = new ArrayList<FleetDamagedRecord>();
        }

        public String getStarKey() {
            return mStarKey;
        }
        public void setStarKey(String starKey) {
            mStarKey = starKey;
        }
        public DateTime getRoundTime() {
            return mRoundTime;
        }
        public void setRoundTime(DateTime time) {
            mRoundTime = time;
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
            for (Messages.CombatRound.FleetSummary fspb : pb.getFleetsList()) {
                combatRound.mFleets.add(FleetSummary.fromProtocolBuffer(fspb));
            }
            for (Messages.CombatRound.FleetJoinedRecord fjrpb : pb.getFleetsJoinedList()) {
                combatRound.mFleetJoinedRecords.add(FleetJoinedRecord.fromProtocolBuffer(combatRound.mFleets, fjrpb));
            }
            for (Messages.CombatRound.FleetTargetRecord ftrpb : pb.getFleetsTargettedList()) {
                combatRound.mFleetTargetRecords.add(FleetTargetRecord.fromProtocolBuffer(combatRound.mFleets, ftrpb));
            }
            for (Messages.CombatRound.FleetAttackRecord farpb : pb.getFleetsAttackedList()) {
                combatRound.mFleetAttackRecords.add(FleetAttackRecord.fromProtocolBuffer(combatRound.mFleets, farpb));
            }
            for (Messages.CombatRound.FleetDamagedRecord fdrpb : pb.getFleetsDamagedList()) {
                combatRound.mFleetDamagedRecords.add(FleetDamagedRecord.fromProtocolBuffer(combatRound.mFleets, fdrpb));
            }
            return combatRound;
        }


        public void toProtocolBuffer(Messages.CombatRound.Builder pb) {
            pb.setStarKey(mStarKey);
            pb.setRoundTime(mRoundTime.getMillis() / 1000);
            for (FleetSummary fleetSummary : mFleets) {
                Messages.CombatRound.FleetSummary.Builder fleet_summary_pb = Messages.CombatRound.FleetSummary.newBuilder();
                fleet_summary_pb.addAllFleetKeys(fleetSummary.mFleetKeys);
                if (fleetSummary.mEmpireKey != null) {
                    fleet_summary_pb.setEmpireKey(fleetSummary.mEmpireKey);
                }
                fleet_summary_pb.setDesignId(fleetSummary.mDesignID);
                fleet_summary_pb.setNumShips(fleetSummary.mNumShips);
                pb.addFleets(fleet_summary_pb);
            }
            for (FleetJoinedRecord fleetJoined : mFleetJoinedRecords) {
                Messages.CombatRound.FleetJoinedRecord.Builder fleet_joined_pb = Messages.CombatRound.FleetJoinedRecord.newBuilder();
                fleet_joined_pb.setFleetIndex(fleetJoined.mFleetIndex);
                pb.addFleetsJoined(fleet_joined_pb);
            }
            for (FleetTargetRecord fleetTargetted : mFleetTargetRecords) {
                Messages.CombatRound.FleetTargetRecord.Builder fleet_targetted_pb = Messages.CombatRound.FleetTargetRecord.newBuilder();
                fleet_targetted_pb.setFleetIndex(fleetTargetted.mFleetIndex);
                fleet_targetted_pb.setTargetIndex(fleetTargetted.mTargetIndex);
                pb.addFleetsTargetted(fleet_targetted_pb);
            }
            for (FleetAttackRecord fleetTargetted : mFleetAttackRecords) {
                Messages.CombatRound.FleetAttackRecord.Builder fleet_attack_pb = Messages.CombatRound.FleetAttackRecord.newBuilder();
                fleet_attack_pb.setFleetIndex(fleetTargetted.mFleetIndex);
                fleet_attack_pb.setTargetIndex(fleetTargetted.mTargetIndex);
                fleet_attack_pb.setDamage(fleetTargetted.mDamage);
                pb.addFleetsAttacked(fleet_attack_pb);
            }
            for (FleetDamagedRecord fleetDamaged : mFleetDamagedRecords) {
                Messages.CombatRound.FleetDamagedRecord.Builder fleet_damage_pb = Messages.CombatRound.FleetDamagedRecord.newBuilder();
                fleet_damage_pb.setFleetIndex(fleetDamaged.mFleetIndex);
                fleet_damage_pb.setDamage(fleetDamaged.mDamage);
                pb.addFleetsDamaged(fleet_damage_pb);
            }
        }
    }

    public static class FleetSummary {
        private List<String> mFleetKeys;
        private List<BaseFleet> mFleets;
        private String mEmpireKey;
        private String mDesignID;
        private float mNumShips;
        private int mIndex;

        public FleetSummary() {
            mFleetKeys = new ArrayList<String>();
            mFleets = new ArrayList<BaseFleet>();
        }
        public FleetSummary(BaseFleet fleet) {
            this();
            mFleetKeys.add(fleet.getKey());
            mEmpireKey = fleet.getEmpireKey();
            mDesignID = fleet.getDesignID();
            mNumShips = fleet.getNumShips();
            mFleets.add(fleet);
        }

        public List<String> getFleetKeys() {
            return mFleetKeys;
        }
        public List<BaseFleet> getFleets() {
            return mFleets;
        }
        public BaseFleet.Stance getFleetStance() {
            if (mFleets.size() <= 0) {
                return BaseFleet.Stance.PASSIVE;
            }
            return mFleets.get(0).getStance();
        }
        public BaseFleet.State getFleetState() {
            if (mFleets.size() <= 0) {
                return BaseFleet.State.IDLE;
            }
            return mFleets.get(0).getState();
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
        public void addShips(FleetSummary otherFleet) {
            mFleetKeys.addAll(otherFleet.getFleetKeys());
            mFleets.addAll(otherFleet.getFleets());
            mNumShips += otherFleet.getNumShips();
        }
        public void removeShips(float numShips) {
            mNumShips -= numShips;
            if (mNumShips < 0) {
                mNumShips = 0;
            }
        }

        public int getIndex() {
            return mIndex;
        }
        public void setIndex(int index) {
            mIndex = index;
        }

        public static FleetSummary fromProtocolBuffer(Messages.CombatRound.FleetSummary pb) {
            FleetSummary fleetSummary = new FleetSummary();
            fleetSummary.mFleetKeys = pb.getFleetKeysList();
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
        public FleetJoinedRecord(List<FleetSummary> fleets, int fleetIndex) {
            mFleets = fleets;
            mFleetIndex = fleetIndex;
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
        public FleetTargetRecord(List<FleetSummary> fleets, int fleetIndex, int targetIndex) {
            mFleets = fleets;
            mFleetIndex = fleetIndex;
            mTargetIndex = targetIndex;
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
        public FleetAttackRecord(List<FleetSummary> fleets, int fleetIndex, int targetIndex, float damage) {
            mFleets = fleets;
            mFleetIndex = fleetIndex;
            mTargetIndex = targetIndex;
            mDamage = damage;
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
        public FleetDamagedRecord(List<FleetSummary> fleets, int fleetIndex, float damage) {
            mFleets = fleets;
            mFleetIndex = fleetIndex;
            mDamage = damage;
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
