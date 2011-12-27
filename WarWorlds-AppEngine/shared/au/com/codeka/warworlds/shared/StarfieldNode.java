package au.com.codeka.warworlds.shared;

import java.io.Serializable;

public class StarfieldNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private StarfieldStar mStar;
    private int mNodeX;
    private int mNodeY;

    public StarfieldNode() {
    }

    public StarfieldNode(int nodeX, int nodeY, StarfieldStar star) {
        mNodeX = nodeX;
        mNodeY = nodeY;
        mStar = star;
    }

    public int getNodeX() {
        return mNodeX;
    }
    public void setNodeX(int nodeX) {
        mNodeX = nodeX;
    }

    public int getNodeY() {
        return mNodeY;
    }
    public void setNodeY(int nodeY) {
        mNodeY = nodeY;
    }

    public StarfieldStar getStar() {
        return mStar;
    }
    public void setStar(StarfieldStar star) {
        mStar = star;
    }
}
