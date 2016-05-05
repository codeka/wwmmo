package au.com.codeka.warworlds.server.world;

import au.com.codeka.warworlds.common.proto.Sector;

/**
 * Manages the sectors we have loaded.
 */
public class SectorManager {
  public static final SectorManager i = new SectorManager();

  public static final int SECTOR_SIZE = 1024;

  public Sector getSector(long id) {
    return null;
  }
}
