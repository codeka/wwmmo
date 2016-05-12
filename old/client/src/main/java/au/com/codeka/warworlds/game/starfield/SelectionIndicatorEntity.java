package au.com.codeka.warworlds.game.starfield;

import org.andengine.engine.Engine;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.Entity;
import org.andengine.entity.primitive.Line;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

/** The selection indicator is an entity which is attached to the currently-select star or fleet. */
public class SelectionIndicatorEntity extends Entity {
    private static final int NUM_POINTS = 16;

    private Entity mClockwise;
    private Entity mAntiClockwise;
    private SelectableEntity mSelectedEntity;

    public SelectionIndicatorEntity(Engine engine, VertexBufferObjectManager vertexBufferObjectManager) {
        super(0.0f, 0.0f, 1.0f, 1.0f);

        mClockwise = new Entity();
        mAntiClockwise = new Entity();
        for (int i = 0; i < NUM_POINTS; i += 2) {
            final float n = (float) (Math.PI * 2.0) / NUM_POINTS * i;
            final float n1 = (float) (Math.PI * 2.0) / NUM_POINTS * (i + 1);

            Line line = new Line(
                        (float) Math.sin(n)*0.5f + 1.0f, (float) Math.cos(n)*0.5f + 1.0f,
                        (float) Math.sin(n1)*0.5f + 1.0f, (float) Math.cos(n1)*0.5f + 1.0f,
                        1.0f, vertexBufferObjectManager
                    );
            mClockwise.attachChild(line);

            line = new Line(
                    (float) Math.sin(n)*0.6f + 1.0f, (float) Math.cos(n)*0.6f + 1.0f,
                    (float) Math.sin(n1)*0.6f + 1.0f, (float) Math.cos(n1)*0.6f + 1.0f,
                    1.0f, vertexBufferObjectManager
                );
            mAntiClockwise.attachChild(line);
        }

        mClockwise.setWidth(1.0f);
        mClockwise.setHeight(1.0f);
        mAntiClockwise.setWidth(1.0f);
        mAntiClockwise.setHeight(1.0f);

        attachChild(mClockwise);
        attachChild(mAntiClockwise);

        engine.registerUpdateHandler(mUpdateHandler);
    }

    public void setSelectedEntity(SelectableEntity entity) {
        if (mSelectedEntity != null) {
            mSelectedEntity.onDeselected(this);
        }
        mSelectedEntity = entity;
        if (mSelectedEntity != null) {
            mSelectedEntity.onSelected(this);
        }
    }

    @Override
    public void setScale(float scale) {
        super.setScale(scale * 2.5f);
        mClockwise.setRotationCenter(1.0f, 1.0f);
        mAntiClockwise.setRotationCenter(1.0f, 1.0f);
    }

    private IUpdateHandler mUpdateHandler = new IUpdateHandler() {
        @Override
        public void onUpdate(float dt) {
            // why is this in degrees, not radians?
            float rotation = mClockwise.getRotation() + 90.0f * dt;
            while (rotation > 360.0f) {
                rotation -= 360.0f;
            }
            mClockwise.setRotation(rotation);

            rotation = mAntiClockwise.getRotation() - 90.0f * dt;
            while (rotation > 360.0f) {
                rotation -= 360.0f;
            }
            mAntiClockwise.setRotation(rotation);
        }

        @Override
        public void reset() {
        }
    };
}
