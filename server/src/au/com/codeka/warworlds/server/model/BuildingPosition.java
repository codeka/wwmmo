package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;

import au.com.codeka.warworlds.server.data.SqlResult;

public class BuildingPosition extends Building {
    private long mSectorX;
    private long mSectorY;
    private int mOffsetX;
    private int mOffsetY;

    public BuildingPosition(SqlResult res) throws SQLException {
        super(res);
        mSectorX = res.getLong("sector_x");
        mSectorY = res.getLong("sector_y");
        mOffsetX = res.getInt("offset_x");
        mOffsetY = res.getInt("offset_y");
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
