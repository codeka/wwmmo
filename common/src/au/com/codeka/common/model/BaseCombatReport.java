package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.CombatReport;

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

    public void fromProtocolBuffer(CombatReport pb) {
        mKey = pb.key;
        mStarKey = pb.star_key;
        mStartTime = new DateTime(pb.start_time * 1000, DateTimeZone.UTC);
        mEndTime = new DateTime(pb.end_time * 1000, DateTimeZone.UTC);
        // TODO: mStartEmpires
        // TODO: mEndEmpires
        mTotalDestroyed = pb.num_destroyed;
        for (au.com.codeka.common.protobuf.CombatRound crpb : pb.rounds) {
            CombatRound combatRound = CombatRound.fromProtocolBuffer(crpb);
            mCombatRounds.add(combatRound);
            if (mCombatRounds.size() > 50) {
                // don't try to populate more than 50 founds of a combat report...
                break;
            }
        }
    }

    public void toProtocolBuffer(CombatReport.Builder pb) {
        pb.key = mKey;
        pb.star_key = mStarKey;
        if (mStartTime != null) {
            pb.start_time = mStartTime.getMillis() / 1000;
        }
        if (mEndTime != null) {
            pb.end_time = mEndTime.getMillis() / 1000;
        }
        // TODO: mStartEmpires
        // TODO: mEndEmpires
        pb.num_destroyed = mTotalDestroyed;
        pb.rounds = new ArrayList<>();
        for (CombatRound round : mCombatRounds) {
            au.com.codeka.common.protobuf.CombatRound.Builder combat_round_pb =
                    new au.com.codeka.common.protobuf.CombatRound.Builder();
            round.toProtocolBuffer(combat_round_pb);
            pb.rounds.add(combat_round_pb.build());
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
            mFleets = new ArrayList<>();
            mFleetJoinedRecords = new ArrayList<>();
            mFleetTargetRecords = new ArrayList<>();
            mFleetAttackRecords = new ArrayList<>();
            mFleetDamagedRecords = new ArrayList<>();
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

        public static CombatRound fromProtocolBuffer(au.com.codeka.common.protobuf.CombatRound pb) {
            CombatRound combatRound = new CombatRound();
            combatRound.mStarKey = pb.star_key;
            combatRound.mRoundTime = new DateTime(pb.round_time * 1000, DateTimeZone.UTC);
            for (au.com.codeka.common.protobuf.CombatRound.FleetSummary fspb : pb.fleets) {
                combatRound.mFleets.add(FleetSummary.fromProtocolBuffer(fspb));
            }
            for (au.com.codeka.common.protobuf.CombatRound.FleetJoinedRecord fjrpb : pb.fleets_joined) {
                combatRound.mFleetJoinedRecords.add(FleetJoinedRecord.fromProtocolBuffer(combatRound.mFleets, fjrpb));
            }
            for (au.com.codeka.common.protobuf.CombatRound.FleetTargetRecord ftrpb : pb.fleets_targetted) {
                combatRound.mFleetTargetRecords.add(FleetTargetRecord.fromProtocolBuffer(combatRound.mFleets, ftrpb));
            }
            for (au.com.codeka.common.protobuf.CombatRound.FleetAttackRecord farpb : pb.fleets_attacked) {
                combatRound.mFleetAttackRecords.add(FleetAttackRecord.fromProtocolBuffer(combatRound.mFleets, farpb));
            }
            for (au.com.codeka.common.protobuf.CombatRound.FleetDamagedRecord fdrpb : pb.fleets_damaged) {
                combatRound.mFleetDamagedRecords.add(FleetDamagedRecord.fromProtocolBuffer(combatRound.mFleets, fdrpb));
            }
            return combatRound;
        }


        public void toProtocolBuffer(au.com.codeka.common.protobuf.CombatRound.Builder pb) {
            pb.star_key = mStarKey;
            pb.round_time = mRoundTime.getMillis() / 1000;
            pb.fleets = new ArrayList<>();
            for (FleetSummary fleetSummary : mFleets) {
                au.com.codeka.common.protobuf.CombatRound.FleetSummary.Builder fleet_summary_pb =
                        new au.com.codeka.common.protobuf.CombatRound.FleetSummary.Builder();
                fleet_summary_pb.fleet_keys(fleetSummary.mFleetKeys);
                if (fleetSummary.mEmpireKey != null) {
                    fleet_summary_pb.empire_key = fleetSummary.mEmpireKey;
                }
                fleet_summary_pb.design_id = fleetSummary.mDesignID;
                fleet_summary_pb.num_ships = fleetSummary.mNumShips;
                pb.fleets.add(fleet_summary_pb.build());
            }
            pb.fleets_joined = new ArrayList<>();
            for (FleetJoinedRecord fleetJoined : mFleetJoinedRecords) {
                au.com.codeka.common.protobuf.CombatRound.FleetJoinedRecord.Builder fleet_joined_pb =
                    new au.com.codeka.common.protobuf.CombatRound.FleetJoinedRecord.Builder();
                fleet_joined_pb.fleet_index = fleetJoined.mFleetIndex;
                pb.fleets_joined.add(fleet_joined_pb.build());
            }
            pb.fleets_targetted = new ArrayList<>();
            for (FleetTargetRecord fleetTargetted : mFleetTargetRecords) {
                au.com.codeka.common.protobuf.CombatRound.FleetTargetRecord.Builder fleet_targetted_pb =
                        new au.com.codeka.common.protobuf.CombatRound.FleetTargetRecord.Builder();
                fleet_targetted_pb.fleet_index = fleetTargetted.mFleetIndex;
                fleet_targetted_pb.target_index = fleetTargetted.mTargetIndex;
                pb.fleets_targetted.add(fleet_targetted_pb.build());
            }
            pb.fleets_attacked = new ArrayList<>();
            for (FleetAttackRecord fleetTargetted : mFleetAttackRecords) {
                au.com.codeka.common.protobuf.CombatRound.FleetAttackRecord.Builder fleet_attack_pb =
                        new au.com.codeka.common.protobuf.CombatRound.FleetAttackRecord.Builder();
                fleet_attack_pb.fleet_index = fleetTargetted.mFleetIndex;
                fleet_attack_pb.target_index = fleetTargetted.mTargetIndex;
                fleet_attack_pb.damage = fleetTargetted.mDamage;
                pb.fleets_attacked.add(fleet_attack_pb.build());
            }
            pb.fleets_damaged = new ArrayList<>();
            for (FleetDamagedRecord fleetDamaged : mFleetDamagedRecords) {
                au.com.codeka.common.protobuf.CombatRound.FleetDamagedRecord.Builder fleet_damage_pb =
                        new au.com.codeka.common.protobuf.CombatRound.FleetDamagedRecord.Builder();
                fleet_damage_pb.fleet_index = fleetDamaged.mFleetIndex;
                fleet_damage_pb.damage = fleetDamaged.mDamage;
                pb.fleets_damaged.add(fleet_damage_pb.build());
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
            mFleetKeys = new ArrayList<>();
            mFleets = new ArrayList<>();
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
        public void setFleetState(BaseFleet.State state) {
            for (BaseFleet fleet : mFleets) {
                fleet.setState(state, DateTime.now());
            }
        }
        public String getEmpireKey() {
            return mEmpireKey;
        }
        public Integer getEmpireID() {
            if (mEmpireKey == null) {
                return null;
            }
            return Integer.parseInt(mEmpireKey);
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

        public static FleetSummary fromProtocolBuffer(au.com.codeka.common.protobuf.CombatRound.FleetSummary pb) {
            FleetSummary fleetSummary = new FleetSummary();
            fleetSummary.mFleetKeys = pb.fleet_keys;
            fleetSummary.mEmpireKey = pb.empire_key;
            fleetSummary.mDesignID = pb.design_id;
            fleetSummary.mNumShips = pb.num_ships;
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
                au.com.codeka.common.protobuf.CombatRound.FleetJoinedRecord pb) {
            FleetJoinedRecord fjr = new FleetJoinedRecord(fleets);
            fjr.mFleetIndex = pb.fleet_index;
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
                au.com.codeka.common.protobuf.CombatRound.FleetTargetRecord pb) {
            FleetTargetRecord ftr = new FleetTargetRecord(fleets);
            ftr.mFleetIndex = pb.fleet_index;
            ftr.mTargetIndex = pb.target_index;
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
                au.com.codeka.common.protobuf.CombatRound.FleetAttackRecord pb) {
            FleetAttackRecord far = new FleetAttackRecord(fleets);
            far.mFleetIndex = pb.fleet_index;
            far.mTargetIndex = pb.target_index;
            far.mDamage = pb.damage;
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
                au.com.codeka.common.protobuf.CombatRound.FleetDamagedRecord pb) {
            FleetDamagedRecord fdr = new FleetDamagedRecord(fleets);
            fdr.mFleetIndex = pb.fleet_index;
            fdr.mDamage = pb.damage;
            return fdr;
        }
    }
}
