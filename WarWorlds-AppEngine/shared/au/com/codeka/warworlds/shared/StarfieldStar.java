package au.com.codeka.warworlds.shared;

import java.io.Serializable;

public class StarfieldStar implements Serializable {
    private static final long serialVersionUID = 1L;

    private int mOffsetX;
    private int mOffsetY;
    private int mColour;
    private int mSize;

    public StarfieldStar() {
    }
    
    public StarfieldStar(int offsetX, int offsetY, int colour, int size) {
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
}
