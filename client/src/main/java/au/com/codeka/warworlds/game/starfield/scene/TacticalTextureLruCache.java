package au.com.codeka.warworlds.game.starfield.scene;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;

import au.com.codeka.common.Pair;

class TacticalTextureLruCache extends LruCache<Pair<Long, Long>, TacticalTexture> {
  TacticalTextureLruCache(int maxSize) {
    super(maxSize);
  }

  @Override
  public void entryRemoved(
      boolean evicted, @NonNull Pair<Long, Long> key, TacticalTexture oldValue,
      TacticalTexture newValue) {
    oldValue.close();
  }
}