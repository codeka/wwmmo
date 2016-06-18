package au.com.codeka.warworlds.client.build;

import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

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
}
