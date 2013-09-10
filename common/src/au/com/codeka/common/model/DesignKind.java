package au.com.codeka.common.model;

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

    public static DesignKind fromNumber(int value) {
        for(DesignKind dk : DesignKind.values()) {
            if (dk.getValue() == value) {
                return dk;
            }
        }

        return DesignKind.BUILDING;
    }
}
