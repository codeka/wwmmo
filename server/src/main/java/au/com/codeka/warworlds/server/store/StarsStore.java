package au.com.codeka.warworlds.server.store;

import java.util.ArrayList;
import java.util.Iterator;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.proto.Star;

/**
 * A special {@link ProtobufStore} for storing stars, including some extra indices for special
 * queries that we can do.
 */
public class StarsStore extends ProtobufStore<Star> {
  StarsStore(String fileName) {
    super(fileName, Star.class);
  }

  @Nullable
  public Star nextStarForSimulate() {
    // TODO
    return null;
  }

  public Iterable<Long> getStarsForEmpire(long empireId) {
    return new ArrayList<>();
  }
}
