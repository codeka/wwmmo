package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Locale;

import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Star;

public class StarStorageView extends ConstraintLayout {
  private TextView storedGoodsTextView;
  private TextView deltaGoodsTextView;
  private TextView storedMineralsTextView;
  private TextView deltaMineralsTextView;

  public StarStorageView(@NonNull Context context) {
    super(context);
    init(context);
  }

  public StarStorageView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public StarStorageView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  public StarStorageView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(context);
  }

  private void init(Context context) {
    inflate(context, R.layout.star_storage, this);

    storedGoodsTextView = findViewById(R.id.stored_goods);
    deltaGoodsTextView = findViewById(R.id.delta_goods);
    storedMineralsTextView = findViewById(R.id.stored_minerals);
    deltaMineralsTextView = findViewById(R.id.delta_minerals);
  }

  public void refresh(Star star) {
    BaseEmpirePresence ep = star.getEmpire(EmpireManager.i.getEmpire().getKey());
    if (ep == null) {
      setVisibility(View.GONE);
    } else {
      setVisibility(View.VISIBLE);

      String goods = String.format(Locale.ENGLISH, "%d / %d", (int) ep.getTotalGoods(),
          (int) ep.getMaxGoods());
      storedGoodsTextView.setText(goods);

      String minerals = String.format(Locale.ENGLISH, "%d / %d", (int) ep.getTotalMinerals(),
          (int) ep.getMaxMinerals());
      storedMineralsTextView.setText(minerals);

      if (ep.getDeltaGoodsPerHour() >= 0) {
        deltaGoodsTextView.setTextColor(Color.GREEN);
        deltaGoodsTextView.setText(
            String.format(Locale.ENGLISH, "+%d/hr", (int) ep.getDeltaGoodsPerHour()));
      } else {
        deltaGoodsTextView.setTextColor(Color.RED);
        deltaGoodsTextView.setText(
            String.format(Locale.ENGLISH, "%d/hr", (int) ep.getDeltaGoodsPerHour()));
      }
      if (ep.getDeltaMineralsPerHour() >= 0) {
        deltaMineralsTextView.setTextColor(Color.GREEN);
        deltaMineralsTextView.setText(
            String.format(Locale.ENGLISH, "+%d/hr", (int) ep.getDeltaMineralsPerHour()));
      } else {
        deltaMineralsTextView.setTextColor(Color.RED);
        deltaMineralsTextView.setText(
            String.format(Locale.ENGLISH, "%d/hr", (int) ep.getDeltaMineralsPerHour()));
      }
    }
  }
}
