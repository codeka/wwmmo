package au.com.codeka.warworlds.model;

import au.com.codeka.common.model.BaseScoutReport;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;

public class ScoutReport extends BaseScoutReport {
    @Override
    protected BaseStar createStar(Messages.Star pb) {
        Star star = new Star();
        if (pb != null) {
            star.fromProtocolBuffer(pb);
        }
        return star;
    }
}
