package au.com.codeka.warworlds.game.starfield;

import org.andengine.entity.Entity;

public abstract class SelectableEntity extends Entity {

    public SelectableEntity(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    public abstract Entity getTouchEntity();

}
