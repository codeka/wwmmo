package au.com.codeka.warworlds.server.data;

import java.io.Serializable;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import au.com.codeka.warworlds.shared.StarfieldStar;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class StarfieldStarData implements Serializable {
    private static final long serialVersionUID = 1L;

    @PrimaryKey
    @Persistent
    private Key mKey;

    @Persistent
    private int mOffsetX;

    @Persistent
    private int mOffsetY;

    @Persistent
    private int mColour;

    @Persistent
    private int mSize;

    public StarfieldStarData() {
    }
    
    public StarfieldStarData(Key key, int offsetX, int offsetY, int colour, int size) {
        mKey = key;
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        mColour = colour;
        mSize = size;
    }

    public int getOffsetX() {
        return mOffsetX;
    }
    public void setOffsetX(int offsetX) {
        mOffsetX = offsetX;
    }

    public int getOffsetY() {
        return mOffsetY;
    }
    public void setOffsetY(int offsetY) {
        mOffsetY = offsetY;
    }

    public int getColour() {
        return mColour;
    }
    public void setColour(int colour) {
        mColour = colour;
    }

    public int getSize() {
        return mSize;
    }
    public void setSize(int size) {
        mSize = size;
    }

    public StarfieldStar toStarfieldStar() {
        StarfieldStar star = new StarfieldStar();
        star.setOffsetX(mOffsetX);
        star.setOffsetY(mOffsetY);
        star.setColour(mColour);
        star.setSize(mSize);
        return star;
    }
}
