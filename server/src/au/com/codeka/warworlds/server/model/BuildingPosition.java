package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BuildingPosition extends Building {
    private long mSectorX;
    private long mSectorY;
    private int mOffsetX;
    private int mOffsetY;

    public BuildingPosition(ResultSet rs) throws SQLException {
        super(rs);
        mSectorX = rs.getLong("sector_x");
        mSectorY = rs.getLong("sector_y");
        mOffsetX = rs.getInt("offset_x");
        mOffsetY = rs.getInt("offset_y");
    }

    public long getSectorX() {
        return mSectorX;
    }
    public long getSectorY() {
        return mSectorY;
    }
    public int getOffsetX() {
        return mOffsetX;
    }
    public int getOffsetY() {
        return mOffsetY;
    }
}
