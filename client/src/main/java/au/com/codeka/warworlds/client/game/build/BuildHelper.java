package au.com.codeka.warworlds.client.game.build;

import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import au.com.codeka.warworlds.common.Time;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

public class BuildHelper {
  public static void setDesignIcon(Design design, ImageView imageView) {
    Picasso.with(imageView.getContext())
        .load("file:///android_asset/sprites/" + design.image_url)
        .into(imageView);
  }

  public static List<BuildRequest> getBuildRequests(Star star) {
    ArrayList<BuildRequest> buildRequests = new ArrayList<>();
    for (Planet planet : star.planets) {
      if (planet.colony != null && planet.colony.build_requests != null) {
        for (BuildRequest buildRequest : planet.colony.build_requests) {
          buildRequests.add(buildRequest);
        }
      }
    }
    return buildRequests;
  }

  public static String formatTimeRemaining(BuildRequest buildRequest) {
    float hours = (float)(buildRequest.end_time - System.currentTimeMillis()) / Time.HOUR;
    float minutes = hours * 60.0f;
    if (minutes < 10.0f) {
      return String.format(Locale.US, "%d mins %02d sec",
          (int) Math.floor(minutes), (int) Math.ceil((minutes - Math.floor(minutes)) * 60.0f));
    } else if (minutes < 60.0f) {
      return String.format(Locale.US, "%d mins", Math.round(minutes));
    } else if (hours < 10.0f) {
      return String.format(Locale.US, "%.0f hrs %.0f mins", Math.floor(hours), minutes - (Math.floor(hours) * 60.0f));
    } else {
      return String.format(Locale.US, "%d hrs", Math.round(hours));
    }
  }
}
