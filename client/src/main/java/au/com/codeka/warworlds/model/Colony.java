package au.com.codeka.warworlds.model;

import au.com.codeka.common.model.BaseColony;

public class Colony extends BaseColony {
    public Integer getEmpireID() {
        if (mEmpireKey == null) {
            return null;
        }
        return Integer.parseInt(mEmpireKey);
    }
}
