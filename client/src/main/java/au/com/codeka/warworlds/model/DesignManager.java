package au.com.codeka.warworlds.model;

import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Element;

import android.content.Context;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseDesignManager;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.warworlds.model.designeffects.BuildingEffect;
import au.com.codeka.warworlds.model.designeffects.EmptySpaceMoverShipEffect;
import au.com.codeka.warworlds.model.designeffects.RadarBuildingEffect;
import au.com.codeka.warworlds.model.designeffects.ShipEffect;
import au.com.codeka.warworlds.model.designeffects.WormholeDisruptorBuildingEffect;

/**
 * This is the base "manager" class that manages designs for ships and buildings.
 */
public class DesignManager extends BaseDesignManager {
    private final static Log log = new Log("DesignManager");

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
        BaseDesignManager.i.parseDesigns();
        ((DesignManager) BaseDesignManager.i).mContext = null;
    }

    @Override
    protected InputStream open(DesignKind kind) throws IOException {
        String fileName;
        if (kind == DesignKind.BUILDING) {
            fileName = "buildings.xml";
        } else if (kind == DesignKind.SHIP) {
            fileName = "ships.xml";
        } else {
            throw new RuntimeException("Unexpected DesignKind: " + kind);
        }

        InputStream ins = mContext.getAssets().open(fileName);

        // Skip past the first line, which is guaranteed to be a comment.
        int n = 0;
        while (ins.read() != '\n') n++;
        log.info("Skipped %d bytes of %s", n, fileName);

        return ins;
    }

    @Override
    public Design.Effect createEffect(DesignKind designKind, Element effectElement) {
        final String kind = effectElement.getAttribute("kind");
        if (designKind == DesignKind.SHIP) {
            if (kind.equals("empty-space-mover")) {
                return new EmptySpaceMoverShipEffect(effectElement);
            } else {
                return new ShipEffect(effectElement);
            }
        } else {
            if (kind.equals("radar")) {
                return new RadarBuildingEffect(effectElement);
            } else if (kind.equals("wormhole-disruptor")) {
                return new WormholeDisruptorBuildingEffect(effectElement);
            } else {
                return new BuildingEffect(effectElement);
            }
        }
    }
}
