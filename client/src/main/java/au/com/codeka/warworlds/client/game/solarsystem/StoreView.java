package au.com.codeka.warworlds.client.game.solarsystem;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.squareup.wire.Wire;

import java.util.Locale;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.util.NumberFormatter;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.StarHelper;

/**
 * {@link StoreView} displays the current contents of your store.
 */
public class StoreView extends RelativeLayout {
  private final static Log log = new Log("StoreView");

  private final TextView storedGoods;
  private final TextView totalGoods;
  private final TextView deltaGoods;
  private final TextView storedMinerals;
  private final TextView totalMinerals;
  private final TextView deltaMinerals;
  private final TextView storedEnergy;
  private final TextView totalEnergy;
  private final TextView deltaEnergy;

  public StoreView(Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    inflate(context, R.layout.solarsystem_store, this);

    storedGoods = findViewById(R.id.stored_goods);
    totalGoods = findViewById(R.id.total_goods);
    deltaGoods = findViewById(R.id.delta_goods);
    storedMinerals = findViewById(R.id.stored_minerals);
    totalMinerals = findViewById(R.id.total_minerals);
    deltaMinerals = findViewById(R.id.delta_minerals);
    storedEnergy = findViewById(R.id.stored_energy);
    totalEnergy = findViewById(R.id.total_energy);
    deltaEnergy = findViewById(R.id.delta_energy);
  }

  public void setStar(Star star) {
    Empire myEmpire = Preconditions.checkNotNull(EmpireManager.i.getMyEmpire());
    EmpireStorage storage = null;
    for (EmpireStorage s : star.empire_stores) {
      if (s.empire_id != null && s.empire_id.equals(myEmpire.id)) {
        storage = s;
        break;
      }
    }

    if (storage == null) {
      log.debug("storage is null");
      setVisibility(View.GONE);
    } else {
      log.debug("storage is not null");
      setVisibility(View.VISIBLE);

      storedGoods.setText(NumberFormatter.format(Math.round(storage.total_goods)));
      totalGoods.setText(String.format(Locale.ENGLISH, "/ %s",
          NumberFormatter.format(Math.round(storage.max_goods))));
      storedMinerals.setText(NumberFormatter.format(Math.round(storage.total_minerals)));
      totalMinerals.setText(String.format(Locale.ENGLISH, "/ %s",
          NumberFormatter.format(Math.round(storage.max_minerals))));
      storedEnergy.setText(NumberFormatter.format(Math.round(storage.total_energy)));
      totalEnergy.setText(String.format(Locale.ENGLISH, "/ %s",
          NumberFormatter.format(Math.round(storage.max_energy))));

      if (Wire.get(storage.goods_delta_per_hour, 0.0f) >= 0) {
        deltaGoods.setTextColor(Color.GREEN);
        deltaGoods.setText(String.format(Locale.ENGLISH, "+%d/hr",
            Math.round(Wire.get(storage.goods_delta_per_hour, 0.0f))));
      } else {
        deltaGoods.setTextColor(Color.RED);
        deltaGoods.setText(String.format(Locale.ENGLISH, "%d/hr",
            Math.round(Wire.get(storage.goods_delta_per_hour, 0.0f))));
      }
      if (Wire.get(storage.minerals_delta_per_hour, 0.0f) >= 0) {
        deltaMinerals.setTextColor(Color.GREEN);
        deltaMinerals.setText(String.format(Locale.ENGLISH, "+%d/hr",
            Math.round(
                StarHelper.getDeltaMineralsPerHour(
                    star, myEmpire.id, System.currentTimeMillis()))));
      } else {
        deltaMinerals.setTextColor(Color.RED);
        deltaMinerals.setText(String.format(Locale.ENGLISH, "%d/hr",
            Math.round(Wire.get(storage.minerals_delta_per_hour, 0.0f))));
      }
      if (Wire.get(storage.energy_delta_per_hour, 0.0f) >= 0) {
        deltaEnergy.setTextColor(Color.GREEN);
        deltaEnergy.setText(String.format(Locale.ENGLISH, "+%d/hr",
            Math.round(Wire.get(storage.energy_delta_per_hour, 0.0f))));
      } else {
        deltaEnergy.setTextColor(Color.RED);
        deltaEnergy.setText(String.format(Locale.ENGLISH, "%d/hr",
            Math.round(Wire.get(storage.energy_delta_per_hour, 0.0f))));
      }
    }
  }
}
