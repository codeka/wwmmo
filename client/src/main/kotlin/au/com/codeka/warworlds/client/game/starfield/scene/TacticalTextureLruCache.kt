package au.com.codeka.warworlds.client.game.starfield.scene

import androidx.collection.LruCache
import au.com.codeka.warworlds.common.proto.SectorCoord

class TacticalTextureLruCache(maxSize: Int) : LruCache<SectorCoord, TacticalTexture>(maxSize) {
  override fun entryRemoved (
      evicted: Boolean, key: SectorCoord, oldValue: TacticalTexture, newValue: TacticalTexture?) {
    oldValue.close()
  }
}