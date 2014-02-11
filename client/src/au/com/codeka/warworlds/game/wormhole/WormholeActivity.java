package au.com.codeka.warworlds.game.wormhole;

import java.io.IOException;
import java.util.Locale;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.ZoomCamera;
import org.andengine.entity.scene.Scene;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import au.com.codeka.warworlds.BaseGlActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

public class WormholeActivity extends BaseGlActivity implements StarManager.StarFetchedHandler {
    protected WormholeSceneManager mWormhole;
    protected Star mStar;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        EmpireShieldManager.i.clearTextureCache();

        Bundle extras = getIntent().getExtras();
        String starKey = extras.getString("au.com.codeka.warworlds.StarKey");
        mWormhole = new WormholeSceneManager(WormholeActivity.this, starKey);

        Button renameBtn = (Button) findViewById(R.id.rename_btn);
        renameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RenameDialog dialog = new RenameDialog();
                dialog.setWormhole(mStar);
                dialog.show(getSupportFragmentManager(), "");
            }
        });

        Button destinationBtn = (Button) findViewById(R.id.destination_btn);
        destinationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DestinationDialog dialog = new DestinationDialog();
                dialog.loadWormholes(mStar);
                dialog.show(getSupportFragmentManager(), "");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                Bundle extras = getIntent().getExtras();
                String starKey = extras.getString("au.com.codeka.warworlds.StarKey");

                StarManager.getInstance().requestStar(starKey, false, WormholeActivity.this);
            }
        });
    }

    @Override
    public void onStarFetched(Star s) {
        Bundle extras = getIntent().getExtras();
        String starKey = extras.getString("au.com.codeka.warworlds.StarKey");

        TextView starName  = (TextView) findViewById(R.id.star_name);
        TextView destinationName = (TextView) findViewById(R.id.destination_name);

        if (!s.getKey().equals(starKey)) {
            int starID = Integer.parseInt(s.getKey());
            if (mStar != null && mStar.getWormholeExtra().getDestWormholeID() == starID) {
                destinationName.setText(String.format(Locale.ENGLISH, "→ %s", s.getName()));
            }

            return;
        }

        mStar = s;

        if (destinationName.getText().toString().equals("")) {
            destinationName.setText(Html.fromHtml("→ <i>None</i>"));
            if (mStar.getWormholeExtra().getDestWormholeID() != 0) {
                StarManager.getInstance().requestStar(Integer.toString(mStar.getWormholeExtra().getDestWormholeID()), false, this);
            }
        }
        starName.setText(mStar.getName());
    }

    /** Create the camera, we don't have a zoom factor. */
    @Override
    protected Camera createCamera() {
        ZoomCamera camera = new ZoomCamera(0, 0, mCameraWidth, mCameraHeight);

        return camera;
    }

    @Override
    protected int getRenderSurfaceViewID() {
        return R.id.wormhole;
    }

    @Override
    protected int getLayoutID() {
        return R.layout.wormhole;
    }

    @Override
    protected void onCreateResources() throws IOException {
        mWormhole.onLoadResources();
    }

    @Override
    protected Scene onCreateScene() throws IOException {
        return mWormhole.createScene();
    }

    @Override
    public void onStart() {
        super.onStart();
        mWormhole.onStart();

        Bundle extras = getIntent().getExtras();
        String starKey = extras.getString("au.com.codeka.warworlds.StarKey");

        StarManager.getInstance().addStarUpdatedListener(starKey, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mWormhole.onStop();

        StarManager.getInstance().removeStarUpdatedListener(this);
    }
}
