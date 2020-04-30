package au.com.codeka.warworlds.planetrender

import au.com.codeka.warworlds.common.Voronoi
import au.com.codeka.warworlds.planetrender.Template.PointCloudTemplate
import au.com.codeka.warworlds.planetrender.Template.VoronoiTemplate
import java.util.*

/** A [Voronoi] that takes it's parameters from a [Template].  */
class TemplatedVoronoi(tmpl: VoronoiTemplate, rand: Random)
  : Voronoi(TemplatedPointCloud(tmpl.getParameter(PointCloudTemplate::class.java)!!, rand))