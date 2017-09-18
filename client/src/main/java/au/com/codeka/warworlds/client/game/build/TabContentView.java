package au.com.codeka.warworlds.client.game.build;

import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Interface implemented by the build tabs ({@link BuildingsView}, {@link ShipsView}) etc.
 */
public interface TabContentView {
  void refresh(Star star, Colony colony);
}
