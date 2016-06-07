package au.com.codeka.warworlds.client.world;

import android.support.annotation.Nullable;

import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.Map;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.store.ProtobufStore;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.ModifyStarPacket;
import au.com.codeka.warworlds.common.proto.Packet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;
import au.com.codeka.warworlds.common.proto.StarUpdatedPacket;
import au.com.codeka.warworlds.common.sim.StarModifier;

/**
 * Manages the {@link Star}s we keep cached and stuff.
 */
public class StarManager {
  private static final Log log = new Log("StarManager");
  public static final StarManager i = new StarManager();

  private final ProtobufStore<Star> stars;
  private final StarModifier starModifier;

  private StarManager() {
    stars = App.i.getDataStore().stars();
    starModifier = new StarModifier(new StarModifier.IdentifierGenerator() {
      @Override
      public long nextIdentifier() {
        // TODO?
        return 0;
      }
    });
  }

  public void create() {
    App.i.getEventBus().register(eventListener);
  }

  /** Gets the star with the given ID. Might return null if don't have that star cached yet. */
  @Nullable
  public Star getStar(long id) {
    // TODO: this probably shouldn't happen on a UI thread...
    return stars.get(id);
  }

  public void updateStar(final Star star, final StarModification modification) {
    App.i.getTaskRunner().runTask(new Runnable() {
      @Override
      public void run() {
        Star.Builder starBuilder = star.newBuilder();
        starModifier.modifyStar(starBuilder, modification);

        // Save the now-modified star.
        Star newStar = starBuilder.build();
        stars.put(star.id, newStar);
        App.i.getEventBus().publish(newStar);

        // Send the modification to the server as well.
        App.i.getServer().send(new Packet.Builder()
            .modify_star(new ModifyStarPacket.Builder()
                .star_id(star.id)
                .modification(Lists.newArrayList(modification))
                .build())
            .build());
      }
    }, Threads.BACKGROUND);
  }

  private final Object eventListener = new Object() {
    /**
     * When the server that a star has been updated, we'll want to update our cached copy of it.
     */
    @EventHandler(thread = Threads.BACKGROUND)
    public void onStarUpdatedPacket(StarUpdatedPacket pkt) {
      log.info("Stars updating, saving to database.");
      long startTime = System.nanoTime();
      Map<Long, Star> values = new HashMap<>();
      for (Star star : pkt.stars) {
        App.i.getEventBus().publish(star);
        values.put(star.id, star);
      }
      stars.putAll(values);
      long endTime = System.nanoTime();
      log.info("Updated %d stars in DB in %d ms", pkt.stars.size(), (endTime - startTime) / 1000000L);
    }
  };
}
