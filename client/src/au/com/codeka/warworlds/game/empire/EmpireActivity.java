package au.com.codeka.warworlds.game.empire;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import au.com.codeka.warworlds.ImagePickerHelper;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.TabFragmentActivity;
import au.com.codeka.warworlds.TabManager;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.model.EmpireManager;

/**
 * This dialog shows the status of the empire. You can see all your colonies, all your fleets, etc.
 */
public class EmpireActivity extends TabFragmentActivity {
    Context mContext = this;
    Bundle mExtras = null;
    boolean mFirstRefresh = true;
    boolean mFirstStarsRefresh = true;
    private ImagePickerHelper mImagePickerHelper = new ImagePickerHelper(this);

    public enum EmpireActivityResult {
        NavigateToPlanet(1),
        NavigateToFleet(2);

        private int mValue;

        public static EmpireActivityResult fromValue(int value) {
            for (EmpireActivityResult res : values()) {
                if (res.mValue == value) {
                    return res;
                }
            }

            throw new IllegalArgumentException("value is not a valid EmpireActivityResult");
        }

        public int getValue() {
            return mValue;
        }

        EmpireActivityResult(int value) {
            mValue = value;
        }
    }

    public ImagePickerHelper getImagePickerHelper() {
        return mImagePickerHelper;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TabManager tabManager = getTabManager();
        tabManager.addTab(mContext, new TabInfo(this, "Overview", OverviewFragment.class, null));
        tabManager.addTab(mContext, new TabInfo(this, "Colonies", ColoniesFragment.class, null));
        tabManager.addTab(mContext, new TabInfo(this, "Build", BuildQueueFragment.class, null));
        tabManager.addTab(mContext, new TabInfo(this, "Fleets", FleetsFragment.class, null));
        tabManager.addTab(mContext, new TabInfo(this, "Settings", SettingsFragment.class, null));

        mExtras = getIntent().getExtras();
        if (mExtras != null) {
            String fleetKey = mExtras.getString("au.com.codeka.warworlds.FleetKey");
            if (fleetKey != null) {
                getTabHost().setCurrentTabByTag("Fleets");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                if (!success) {
                    startActivity(new Intent(mContext, WarWorldsActivity.class));
                    return;
                }

                // make sure we have a current empire
                EmpireManager.i.refreshEmpire();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mImagePickerHelper.onActivityResult(requestCode, resultCode, data)) {
            getTabHost().setCurrentTabByTag("Settings");
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
