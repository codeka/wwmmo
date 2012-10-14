package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.util.FloatMath;

/**
 * This class represents a report from a single round of combat. Who entered the battle, who
 * left, who was damaged and so on.
 */
public class CombatRound {
    private String mStarKey;
    private DateTime mRoundTime;
    private FleetSummary[] mFleetSummaries;
    private FleetJoinedRecord[] mFleetsJoined;
    private FleetTargetRecord[] mFleetsTargetted;
    private FleetAttackRecord[] mFleetsAttacked;
    private FleetDamageRecord[] mFleetsDamaged;

    public String getStarKey() {
        return mStarKey;
    }
    public DateTime getRoundTime() {
        return mRoundTime;
    }
    public FleetSummary[] getFleetSummaries() {
        return mFleetSummaries;
    }
    public FleetJoinedRecord[] getFleetsJoined() {
        return mFleetsJoined;
    }
    public FleetTargetRecord[] getFleetsTargetted() {
        return mFleetsTargetted;
    }
    public FleetAttackRecord[] getFleetsAttacked() {
        return mFleetsAttacked;
    }
    public FleetDamageRecord[] getFleetsDamaged() {
        return mFleetsDamaged;
    }

    public static CombatRound fromProtocolBuffer(warworlds.Warworlds.CombatRound pb) {
        CombatRound cb = new CombatRound();
        cb.mStarKey = pb.getStarKey();
        cb.mRoundTime = new DateTime(pb.getRoundTime() * 1000, DateTimeZone.UTC);
        cb.mFleetSummaries = new FleetSummary[pb.getFleetsCount()];
        for (int i = 0; i < pb.getFleetsCount(); i++) {
            cb.mFleetSummaries[i] = cb.new FleetSummary();
            cb.mFleetSummaries[i].mFleetKey = pb.getFleets(i).getFleetKey();
            cb.mFleetSummaries[i].mEmpireKey = pb.getFleets(i).getEmpireKey();
            cb.mFleetSummaries[i].mDesignID = pb.getFleets(i).getDesignId();
            cb.mFleetSummaries[i].mNumShips = (int) FloatMath.ceil(pb.getFleets(i).getNumShips());
        }
        cb.mFleetsJoined = new FleetJoinedRecord[pb.getFleetsJoinedCount()];
        for (int i = 0; i < pb.getFleetsJoinedCount(); i++) {
            cb.mFleetsJoined[i] = cb.new FleetJoinedRecord();
            cb.mFleetsJoined[i].mFleetIndex = pb.getFleetsJoined(i).getFleetIndex();
        }
        cb.mFleetsTargetted = new FleetTargetRecord[pb.getFleetsTargettedCount()];
        for (int i = 0; i < pb.getFleetsTargettedCount(); i++) {
            cb.mFleetsTargetted[i] = cb.new FleetTargetRecord();
            cb.mFleetsTargetted[i].mFleetIndex = pb.getFleetsTargetted(i).getFleetIndex();
            cb.mFleetsTargetted[i].mTargetIndex = pb.getFleetsTargetted(i).getTargetIndex();
        }
        cb.mFleetsAttacked = new FleetAttackRecord[pb.getFleetsAttackedCount()];
        for (int i = 0; i < pb.getFleetsAttackedCount(); i++) {
            cb.mFleetsAttacked[i] = cb.new FleetAttackRecord();
            cb.mFleetsAttacked[i].mFleetIndex = pb.getFleetsAttacked(i).getFleetIndex();
            cb.mFleetsAttacked[i].mTargetIndex = pb.getFleetsAttacked(i).getTargetIndex();
            cb.mFleetsAttacked[i].mDamage = pb.getFleetsAttacked(i).getDamage();
        }
        cb.mFleetsDamaged = new FleetDamageRecord[pb.getFleetsDamagedCount()];
        for (int i = 0; i < pb.getFleetsDamagedCount(); i++) {
            cb.mFleetsDamaged[i] = cb.new FleetDamageRecord();
            cb.mFleetsDamaged[i].mFleetIndex = pb.getFleetsDamaged(i).getFleetIndex();
            cb.mFleetsDamaged[i].mDamage = pb.getFleetsDamaged(i).getDamage();
        }
        return cb;
    }

    public class FleetSummary {
        private String mFleetKey;
        private String mEmpireKey;
        private String mDesignID;
        private int mNumShips;

        public String getFleetKey() {
            return mFleetKey;
        }
        public String getEmpireKey() {
            return mEmpireKey;
        }
        public String getDesignID() {
            return mDesignID;
        }
        public int getNumShips() {
            return mNumShips;
        }
    }

    public class FleetJoinedRecord {
        private int mFleetIndex;

        public FleetSummary getFleet() {
            return mFleetSummaries[mFleetIndex];
        }
    }

    public class FleetTargetRecord {
        private int mFleetIndex;
        private int mTargetIndex;

        public FleetSummary getFleet() {
            return mFleetSummaries[mFleetIndex];
        }
        public FleetSummary getTarget() {
            return mFleetSummaries[mTargetIndex];
        }
    }

    public class FleetAttackRecord {
        private int mFleetIndex;
        private int mTargetIndex;
        private float mDamage;

        public FleetSummary getFleet() {
            return mFleetSummaries[mFleetIndex];
        }
        public FleetSummary getTarget() {
            return mFleetSummaries[mTargetIndex];
        }
        public float getDamage() {
            return mDamage;
        }
    }

    public class FleetDamageRecord {
        private int mFleetIndex;
        private float mDamage;

        public FleetSummary getFleet() {
            return mFleetSummaries[mFleetIndex];
        }
        public float getDamage() {
            return mDamage;
        }
    }
}
