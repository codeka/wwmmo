package au.com.codeka.warworlds.game.starfield;

import java.io.IOException;

import org.andengine.entity.scene.Scene;

import com.google.protobuf.InvalidProtocolBufferException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.BaseGlActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.game.FleetMoveActivity;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;

/** This is the base activity for activities that show a starfield. */
@SuppressLint("Registered") // it's a base class
public abstract class BaseStarfieldActivity extends BaseGlActivity {
    protected StarfieldSceneManager mStarfield;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        EmpireShieldManager.i.clearTextureCache();

        mStarfield = new StarfieldSceneManager(this);
    }

    @Override
    protected int getRenderSurfaceViewID() {
        return R.id.starfield;
    }

    @Override
    protected void onCreateResources() throws IOException {
        mStarfield.onLoadResources();
    }

    @Override
    protected Scene onCreateScene() throws IOException {
        return mStarfield.createScene();
    }

    @Override
    public void onStart() {
        super.onStart();
        mStarfield.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mStarfield.onStop();
    }
}
