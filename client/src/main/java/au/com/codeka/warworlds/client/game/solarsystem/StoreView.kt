package au.com.codeka.warworlds.client.game.solarsystem

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.util.NumberFormatter
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.EmpireStorage
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.sim.StarHelper
import com.google.common.base.Preconditions
import com.squareup.wire.get
import java.util.*

/**
 * [StoreView] displays the current contents of your store.
 */
class StoreView(context: Context?, attributeSet: AttributeSet?)
    : RelativeLayout(context, attributeSet) {
  private val storedGoods: TextView
  private val totalGoods: TextView
  private val deltaGoods: TextView
  private val storedMinerals: TextView
  private val totalMinerals: TextView
  private val deltaMinerals: TextView
  private val storedEnergy: TextView
  private val totalEnergy: TextView
  private val deltaEnergy: TextView
  fun setStar(star: Star) {
    val myEmpire = Preconditions.checkNotNull(EmpireManager.getMyEmpire())
    var storage: EmpireStorage? = null
    for (s in star.empire_stores) {
      if (s.empire_id != null && s.empire_id == myEmpire.id) {
        storage = s
        break
      }
    }
    if (storage == null) {
      log.debug("storage is null")
      visibility = View.GONE
    } else {
      log.debug("storage is not null")
      visibility = View.VISIBLE
      storedGoods.text = NumberFormatter.format(Math.round(storage.total_goods))
      totalGoods.text = String.format(Locale.ENGLISH, "/ %s",
          NumberFormatter.format(Math.round(storage.max_goods)))
      storedMinerals.text = NumberFormatter.format(Math.round(storage.total_minerals))
      totalMinerals.text = String.format(Locale.ENGLISH, "/ %s",
          NumberFormatter.format(Math.round(storage.max_minerals)))
      storedEnergy.text = NumberFormatter.format(Math.round(storage.total_energy))
      totalEnergy.text = String.format(Locale.ENGLISH, "/ %s",
          NumberFormatter.format(Math.round(storage.max_energy)))
      if (get(storage.goods_delta_per_hour, 0.0f) >= 0) {
        deltaGoods.setTextColor(Color.GREEN)
        deltaGoods.text = String.format(Locale.ENGLISH, "+%d/hr",
            Math.round(get(storage.goods_delta_per_hour, 0.0f)))
      } else {
        deltaGoods.setTextColor(Color.RED)
        deltaGoods.text = String.format(Locale.ENGLISH, "%d/hr",
            Math.round(get(storage.goods_delta_per_hour, 0.0f)))
      }
      if (get(storage.minerals_delta_per_hour, 0.0f) >= 0) {
        deltaMinerals.setTextColor(Color.GREEN)
        deltaMinerals.text = String.format(Locale.ENGLISH, "+%d/hr",
            Math.round(
                StarHelper.getDeltaMineralsPerHour(
                    star, myEmpire.id, System.currentTimeMillis())))
      } else {
        deltaMinerals.setTextColor(Color.RED)
        deltaMinerals.text = String.format(Locale.ENGLISH, "%d/hr",
            Math.round(get(storage.minerals_delta_per_hour, 0.0f)))
      }
      if (get(storage.energy_delta_per_hour, 0.0f) >= 0) {
        deltaEnergy.setTextColor(Color.GREEN)
        deltaEnergy.text = String.format(Locale.ENGLISH, "+%d/hr",
            Math.round(get(storage.energy_delta_per_hour, 0.0f)))
      } else {
        deltaEnergy.setTextColor(Color.RED)
        deltaEnergy.text = String.format(Locale.ENGLISH, "%d/hr",
            Math.round(get(storage.energy_delta_per_hour, 0.0f)))
      }
    }
  }

  companion object {
    private val log = Log("StoreView")
  }

  init {
    View.inflate(context, R.layout.solarsystem_store, this)
    storedGoods = findViewById(R.id.stored_goods)
    totalGoods = findViewById(R.id.total_goods)
    deltaGoods = findViewById(R.id.delta_goods)
    storedMinerals = findViewById(R.id.stored_minerals)
    totalMinerals = findViewById(R.id.total_minerals)
    deltaMinerals = findViewById(R.id.delta_minerals)
    storedEnergy = findViewById(R.id.stored_energy)
    totalEnergy = findViewById(R.id.total_energy)
    deltaEnergy = findViewById(R.id.delta_energy)
  }
}