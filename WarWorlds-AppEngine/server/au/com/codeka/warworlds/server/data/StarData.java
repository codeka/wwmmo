package au.com.codeka.warworlds.server.data;

import java.io.Serializable;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import au.com.codeka.warworlds.shared.StarfieldStar;

import com.google.appengine.api.datastore.Key;

/**
 * Represents a single \c Star in the universe. A star has a name, colour,
 * location and is basically a "container" for a collection of planets.
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class StarData implements Serializable {
    private static final long serialVersionUID = 1L;

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key mKey;

    @Persistent
    private String mName;

    @Persistent
    private int mX;

    @Persistent
    private int mY;

    @Persistent
    private int mColour;

    @Persistent
    private int mSize;

    public StarData() {
    }

    public StarData(String name, int x, int y, int colour, int size) {
        mName = name;
        mX = x;
        mY = y;
        mColour = colour;
        mSize = size;
    }

    public Key getKey() {
        return mKey;
    }

    /**
     * Gets the x-offset into the \c StarfieldSector this star appears in.
     */
    public int getX() {
        return mX;
    }

    /**
     * Gets the y-offset into the \c StarfieldSector this star appears in.
     */
    public int getY() {
        return mY;
    }

    public String getName() {
        return mName;
    }

    /**
     * Gets the "colour" of this star, which really doesn't have any meaning
     * it's just a visual thing.
     * @return
     */
    public int getColour() {
        return mColour;
    }

    /**
     * Gets the size of this star, which really doesn't have any meaning
     * it's just a visual thing.
     * @return
     */
    public int getSize() {
        return mSize;
    }

    /**
     * Converts this \c StarData into a \c StarfieldStar, which is basically
     * a summary of the star we use when displaying the starfield.
     * @return
     */
    public StarfieldStar toStarfieldStar() {
        StarfieldStar star = new StarfieldStar();
        star.setName(mName);
        star.setX(mX);
        star.setY(mY);
        star.setColour(mColour);
        star.setSize(mSize);
        return star;
    }
}
