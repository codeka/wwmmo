package au.com.codeka.warworlds.server.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Element;

import au.com.codeka.common.model.BaseDesignManager;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.designeffects.DefenceBuildingEffect;
import au.com.codeka.warworlds.server.designeffects.EmptySpaceMoverShipEffect;
import au.com.codeka.warworlds.server.designeffects.FighterShipEffect;
import au.com.codeka.warworlds.server.designeffects.PopulationBoostBuildingEffect;
import au.com.codeka.warworlds.server.designeffects.RadarBuildingEffect;
import au.com.codeka.warworlds.server.designeffects.ScoutShipEffect;
import au.com.codeka.warworlds.server.designeffects.StorageBuildingEffect;
import au.com.codeka.warworlds.server.designeffects.TroopCarrierShipEffect;
import au.com.codeka.warworlds.server.designeffects.WormholeGeneratorShipEffect;

public class DesignManager extends BaseDesignManager {
    public static void setup() {
        DesignManager.i = new DesignManager();
        DesignManager.i.parseDesigns();
    }

    @Override
    protected InputStream open(DesignKind designKind) throws IOException {
        File file = Configuration.i.getDataDirectory();
        if (designKind == DesignKind.SHIP) {
            file = new File(file, "designs/ships.xml");
        } else {
            file = new File(file, "designs/buildings.xml");
        }
        return new FileInputStream(file);
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
            } else if (effectKind.equals("empty-space-mover")) {
                return new EmptySpaceMoverShipEffect();
            } else if (effectKind.equals("wormhole-generator")) {
                return new WormholeGeneratorShipEffect();
            }
        }

        return null;
    }
}
