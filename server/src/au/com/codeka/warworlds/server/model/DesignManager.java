package au.com.codeka.warworlds.server.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import au.com.codeka.warworlds.server.designeffects.*;
import org.w3c.dom.Element;

import au.com.codeka.common.model.BaseDesignManager;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.warworlds.server.Configuration;

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
      switch (effectKind) {
        case "storage":
          return new StorageBuildingEffect();
        case "defence":
          return new DefenceBuildingEffect();
        case "populationBoost":
          return new PopulationBoostBuildingEffect();
        case "radar":
          return new RadarBuildingEffect();
        case "wormhole-disruptor":
          return new WormholeDisruptorBuildingEffect();
        case "goodsBoost":
          return new GoodsBoostBuildingEffect();
        default:
          throw new RuntimeException("Unknown effectKind: " + effectKind);
      }

    } else {
      switch (effectKind) {
        case "scout":
          return new ScoutShipEffect();
        case "fighter":
          return new FighterShipEffect();
        case "troopcarrier":
          return new TroopCarrierShipEffect();
        case "empty-space-mover":
          return new EmptySpaceMoverShipEffect();
        case "wormhole-generator":
          return new WormholeGeneratorShipEffect();
        default:
          throw new RuntimeException("Unknown effectKind: " + effectKind);
      }
    }
  }
}
