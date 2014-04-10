package au.com.codeka.warworlds.model;

import au.com.codeka.common.model.BaseAlliance;
import au.com.codeka.common.model.BaseEmpire;
import au.com.codeka.common.model.BaseEmpireRank;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;


public class Empire extends BaseEmpire {
    //TODO: we can store the ID and have getKey() return a string....
    public int getID() {
        return Integer.parseInt(mKey);
    }

    @Override
    protected BaseEmpireRank createEmpireRank(Messages.EmpireRank pb) {
        EmpireRank er = new EmpireRank();
        if (pb != null) {
            er.fromProtocolBuffer(pb);
        }
        return er;
    }

    @Override
    protected BaseStar createStar(Messages.Star pb) {
        StarSummary s = new StarSummary();
        if (pb != null) {
            s.fromProtocolBuffer(pb);
        }
        return s;
    }

    @Override
    protected BaseAlliance createAlliance(Messages.Alliance pb) {
        Alliance a = new Alliance();
        if (pb != null) {
            a.fromProtocolBuffer(pb);
        }
        return a;
    }

    @Override
    public void toProtocolBuffer(Messages.Empire.Builder pb) {
        super.toProtocolBuffer(pb);
    }

    public Messages.Empire toProtocolBuffer() {
        Messages.Empire.Builder pb = Messages.Empire.newBuilder();
        toProtocolBuffer(pb);
        return pb.build();
    }
}
