package au.com.codeka.warworlds.model;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import android.content.Context;
import au.com.codeka.common.model.BaseDesignManager;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;

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

    @Override
    protected Design parseDesign(DesignKind kind, Element designElement) {
        if (kind == DesignKind.BUILDING) {
            return new BuildingDesign.Factory(designElement).get();
        } else if (kind == DesignKind.SHIP) {
            return new ShipDesign.Factory(designElement).get();
        }

        return null;
    }
}
