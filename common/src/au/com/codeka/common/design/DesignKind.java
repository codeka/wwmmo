package au.com.codeka.common.design;

import au.com.codeka.common.model.BuildRequest;

public enum DesignKind {
    BUILDING(1),
    SHIP(2);

    private int mValue;

    DesignKind(int value) {
        mValue = value;
    }

    public int getValue() {
        return mValue;
    }

    public static DesignKind fromBuildKind(BuildRequest.BUILD_KIND buildKind) {
        return values()[buildKind.ordinal()];
    }
}
