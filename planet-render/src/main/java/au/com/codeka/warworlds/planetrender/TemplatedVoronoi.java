package au.com.codeka.warworlds.planetrender;

import java.util.Random;

import au.com.codeka.warworlds.common.Voronoi;

/** A {@link Voronoi} that takes it's parameters from a {@link Template}. */
public class TemplatedVoronoi extends Voronoi {
  public TemplatedVoronoi(Template.VoronoiTemplate tmpl, Random rand) {
    super(new TemplatedPointCloud(tmpl.getParameter(Template.PointCloudTemplate.class), rand));
  }
}
