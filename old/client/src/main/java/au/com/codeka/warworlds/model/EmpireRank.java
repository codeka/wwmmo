package au.com.codeka.warworlds.model;

import au.com.codeka.common.model.BaseEmpireRank;

public class EmpireRank extends BaseEmpireRank {
    public int getEmpireID() {
        return Integer.parseInt(mEmpireKey);
    }
}
