package au.com.codeka.warworlds;

import au.com.codeka.warworlds.model.AllianceShieldManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;

/**
 * This class clears out most of our in-memory caches when the UI is hidden.
 */
public class MemoryTrimmer {
  public static void trimMemory() {
    EmpireShieldManager.i.clearCache();
    AllianceShieldManager.i.clearCache();
    StarImageManager.getInstance().clearCaches();
    PlanetImageManager.getInstance().clearCaches();
    SectorManager.i.clearCache();
    StarManager.i.clearCache();
  }
}
