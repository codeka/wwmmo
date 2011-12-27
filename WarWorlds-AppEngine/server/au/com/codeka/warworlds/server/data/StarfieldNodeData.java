package au.com.codeka.warworlds.server.data;

import java.io.Serializable;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import au.com.codeka.warworlds.shared.StarfieldNode;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class StarfieldNodeData implements Serializable {
    private static final long serialVersionUID = 1L;

    @PrimaryKey
    @Persistent
    private Key mKey;

    @Persistent
    private int mNodeX;

    @Persistent
    private int mNodeY;

    @Persistent
    private StarfieldStarData mStar;

    public StarfieldNodeData() {
    }

    public StarfieldNodeData(int nodeX, int nodeY, StarfieldStarData star) {
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

    public StarfieldStarData getStar() {
        return mStar;
    }
    public void setStar(StarfieldStarData star) {
        mStar = star;
    }

    public StarfieldNode toStarfieldNode() {
        StarfieldNode node = new StarfieldNode();
        node.setNodeX(mNodeX);
        node.setNodeY(mNodeY);
        node.setStar(mStar.toStarfieldStar());
        return node;
    }
}
