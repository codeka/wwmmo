package au.com.codeka.warworlds.client.game.starfield;

import android.content.Context;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.squareup.picasso.Picasso;

import java.util.Locale;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.ctrl.PlanetListSimple;
import au.com.codeka.warworlds.client.game.fleets.FleetListSimple;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.common.TimeFormatter;
import au.com.codeka.warworlds.common.proto.ScoutReport;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.StarHelper;

public class ScoutReportBottomPane extends RelativeLayout {
  public interface Callback {
    void onBackClicked();
  }

  private final Star star;
  private final Callback callback;

  private final PlanetListSimple planetList;
  private final FleetListSimple fleetList;
  private final TextView starName;
  private final TextView starKind;
  private final ImageView starIcon;
  private final TextView reportDate;

  public ScoutReportBottomPane(@NonNull Context context, Star star, Callback callback) {
    super(context);
    this.star = star;
    this.callback = callback;

    inflate(context, R.layout.starfield_bottom_pane_scout_report, this);

    planetList = findViewById(R.id.planet_list);
    fleetList = findViewById(R.id.fleet_list);
    starName = findViewById(R.id.star_name);
    starKind = findViewById(R.id.star_kind);
    starIcon = findViewById(R.id.star_icon);
    reportDate = findViewById(R.id.report_date);

    findViewById(R.id.back_btn).setOnClickListener((v) -> callback.onBackClicked());

    if (isInEditMode()) {
      return;
    }

    refresh();
  }

  public void refresh() {
    if (star.scout_reports.size() != 1) {
      // This is an error!
      return;
    }

    ScoutReport scoutReport = star.scout_reports.get(0);

    fleetList.setStar(star, scoutReport.fleets);
    planetList.setStar(star, scoutReport.planets, scoutReport.fleets);

    reportDate.setText(TimeFormatter.create()
        .withTimeInPast(true)
        .format(scoutReport.report_time - System.currentTimeMillis()));
    starName.setText(star.name);
    starKind.setText(String.format(Locale.ENGLISH, "%s %s", star.classification,
        StarHelper.getCoordinateString(star)));
    Picasso.get()
        .load(ImageHelper.getStarImageUrl(getContext(), star, 40, 40))
        .into(starIcon);
  }
}
