package au.com.codeka.warworlds.model;

import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Element;

import android.content.Context;
import au.com.codeka.common.model.BaseDesignManager;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.warworlds.model.designeffects.BuildingEffect;
import au.com.codeka.warworlds.model.designeffects.RadarBuildingEffect;
import au.com.codeka.warworlds.model.designeffects.ShipEffect;

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
        BaseDesignManager.i = new DesignManager();
        ((DesignManager) BaseDesignManager.i).mContext = context;
        BaseDesignManager.i.setup();
        ((DesignManager) BaseDesignManager.i).mContext = null;
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
    public Design.Effect createEffect(DesignKind designKind, Element effectElement) {
        final String kind = effectElement.getAttribute("kind");
        if (designKind == DesignKind.SHIP) {
            return new ShipEffect(effectElement);
        } else {
            if (kind.equals("radar")) {
                return new RadarBuildingEffect(effectElement);
            } else {
                return new BuildingEffect(effectElement);
            }
        }
    }
}
