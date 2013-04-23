package au.com.codeka.warworlds.model;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import au.com.codeka.common.model.BaseDesignManager;
import au.com.codeka.common.model.DesignKind;

/**
 * This is the base "manager" class that manages designs for ships and buildings.
 */
public class DesignManager extends BaseDesignManager {
    // this will only be non-null for as long as setup() is executing... after that, it's
    // no longer needed and set back to null.
    private Context mContext;

    /**
     * This should be called at the beginning of the game to initialize the
     * design manager. We download the list of designs, parse them and set up the
     * list.
     */
    public static void setup(final Context context) {
        DesignManager dm = new DesignManager();
        dm.mContext = context;
        dm.setup();
        dm.mContext = null;
        BaseDesignManager.i = dm;
    }

    @Override
    protected InputStream open(DesignKind kind) throws IOException {
        if (kind == DesignKind.BUILDING) {
            return mContext.getAssets().open("buildings.xml");
        } else if (kind == DesignKind.SHIP) {
            return mContext.getAssets().open("ships.xml");
        }

        return null;
    }
}
