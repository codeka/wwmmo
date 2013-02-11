package au.com.codeka.planetrender;

import java.util.Random;

import au.com.codeka.common.Voronoi;

public class TemplatedVoronoi extends Voronoi {
    public TemplatedVoronoi(Template.VoronoiTemplate tmpl, Random rand) {
        super(new TemplatedPointCloud(tmpl.getParameter(Template.PointCloudTemplate.class), rand));
    }
}
