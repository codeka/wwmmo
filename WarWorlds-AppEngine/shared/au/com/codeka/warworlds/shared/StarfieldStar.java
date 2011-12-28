package au.com.codeka.warworlds.shared;

import java.io.Serializable;

public class StarfieldStar implements Serializable {
    private static final long serialVersionUID = 1L;

    private int mX;
    private int mY;
    private int mColour;
    private int mSize;

    public StarfieldStar() {
    }

    /**
     * Constructs a new \c StarfieldStar which is basically a "summary" of a
     * \c Star and just contains information required to render the starfield.
     * @param x The x-offset into the \c StarfieldSector that this star appears.
     * @param y The y-offset into the \c StarfieldSector that this star appears.
     * @param colour The colour, as an RGB triplet of this star.
     * @param size The "size" of the star, as it should appear in the starfield.
     */
    public StarfieldStar(int x, int y, int colour, int size) {
        mX = x;
        mY = y;
        mColour = colour;
        mSize = size;
    }

    public int getX() {
        return mX;
    }
    public void setX(int x) {
        mX = x;
    }

    public int getY() {
        return mY;
    }
    public void setY(int y) {
        mY = y;
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
}
