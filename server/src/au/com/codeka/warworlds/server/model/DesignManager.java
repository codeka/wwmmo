package au.com.codeka.warworlds.server.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Element;

import au.com.codeka.common.model.BaseDesignManager;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.warworlds.server.designeffects.*;

public class DesignManager extends BaseDesignManager {
    private String mBasePath;

    public static void setup(String basePath) {
        DesignManager.i = new DesignManager(basePath);
        DesignManager.i.setup();
    }

    private DesignManager(String basePath) {
        mBasePath = basePath;
    }

    @Override
    protected InputStream open(DesignKind designKind) throws IOException {
        String fileName = mBasePath;
        if (designKind == DesignKind.SHIP) {
            fileName += "../data/designs/ships.xml";
        } else {
            fileName += "../data/designs/buildings.xml";
        }
        return new FileInputStream(fileName);
    }

    @Override
    public Design.Effect createEffect(DesignKind designKind, Element effectElement) {
        Design.Effect effect = createEffect(designKind, effectElement.getAttribute("kind"));
        if (effect == null) {
            return null;
        }

        effect.load(effectElement);
        return effect;
    }

    private Design.Effect createEffect(DesignKind designKind, String effectKind) {
        if (designKind == DesignKind.BUILDING) {
            if (effectKind.equals("storage")) {
                return new StorageBuildingEffect();
            } else if (effectKind.equals("defence")) {
                return new DefenceBuildingEffect();
            } else if (effectKind.equals("populationBoost")) {
                return new PopulationBoostBuildingEffect();
            } else if (effectKind.equals("radar")) {
                return new RadarBuildingEffect();
            }
        } else {
            if (effectKind.equals("scout")) {
                return new ScoutShipEffect();
            } else if (effectKind.equals("fighter")) {
                return new FighterShipEffect();
            } else if (effectKind.equals("troopcarrier")) {
                return new TroopCarrierShipEffect();
            }
        }

        return null;
    }
}
