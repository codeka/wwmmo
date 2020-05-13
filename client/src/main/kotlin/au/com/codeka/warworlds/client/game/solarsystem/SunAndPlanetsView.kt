package au.com.codeka.warworlds.client.game.solarsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.ViewCompat
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.build.BuildViewHelper
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator.OnDrawHandler
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator.setBackground
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.Vector2
import au.com.codeka.warworlds.common.proto.Building
import au.com.codeka.warworlds.common.proto.Planet
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.sim.DesignDefinitions
import au.com.codeka.warworlds.common.sim.DesignHelper
import com.squareup.picasso.Picasso
import java.util.*

/**
 * The view which displays the big star and planets, and allows you to click planets to "select"
 * them. You can only select one planet at a time.
 */
class SunAndPlanetsView(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
  companion object {
    private val log = Log("SunAndPlanetsView")
  }

  interface PlanetSelectedHandler {
    fun onPlanetSelected(planet: Planet?)
  }

  private val orbitPaint: Paint = Paint()
  private var star: Star? = null
  private val planetInfos = ArrayList<PlanetInfo>()
  private val selectionIndicator: ImageView
  private var selectedPlanet: Planet? = null
  private var planetSelectedHandler: PlanetSelectedHandler? = null

  init {
    orbitPaint.setARGB(255, 255, 255, 255)
    orbitPaint.style = Paint.Style.STROKE
    selectionIndicator = ImageView(context)
    selectionIndicator.setImageResource(R.drawable.planet_selection)
    selectionIndicator.visibility = View.GONE

    // Set ourselves to be clickable so that touch event don't go through to the starfield under us.
    isClickable = true
  }

  fun setPlanetSelectedHandler(handler: PlanetSelectedHandler?) {
    planetSelectedHandler = handler
  }

  fun getPlanetCentre(planet: Planet): Vector2? {
    return planetInfos[planet.index].centre
  }

  /** Gets the [ImageView] that displays the given planet's icon.  */
  fun getPlanetView(planetIndex: Int): ImageView? {
    return planetInfos[planetIndex].imageView
  }

  fun setStar(star: Star) {
    if (isInEditMode) {
      return
    }
    setBackground(this, onBackgroundDrawHandler, star.id)
    removeAllViews()
    addView(selectionIndicator)
    this.star = star
    planetInfos.clear()
    for (i in star.planets.indices) {
      planetInfos.add(PlanetInfo(star.planets[i], Vector2(0.0, 0.0), 0.0f))
    }
    val sunImageView = ImageView(context)
    val lp = LayoutParams(
        (256 * context.resources.displayMetrics.density).toInt(),
        (256 * context.resources.displayMetrics.density).toInt())
    val yOffset = (20 * context.resources.displayMetrics.density).toInt()
    lp.topMargin = -lp.height / 2 + +yOffset
    lp.leftMargin = -lp.width / 2
    sunImageView.layoutParams = lp
    addView(sunImageView)
    Picasso.get()
        .load(ImageHelper.getStarImageUrl(context, star, 256, 256))
        .into(sunImageView)
    placePlanets()
  }

  /** Selects the planet at the given index.  */
  fun selectPlanet(planetIndex: Int) {
    selectedPlanet = planetInfos[planetIndex].planet
    updateSelection()
  }

  val selectedPlanetIndex: Int
    get() = if (selectedPlanet == null) {
      -1
    } else star!!.planets.indexOf(selectedPlanet)

  private fun getDistanceFromSun(planetIndex: Int): Float {
    var width = width
    if (width == 0) {
      return 0.0f
    }
    width -= (16 * context.resources.displayMetrics.density).toInt()
    val planetStart = 150 * context.resources.displayMetrics.density
    var distanceBetweenPlanets = width - planetStart
    distanceBetweenPlanets /= planetInfos.size.toFloat()
    return planetStart + distanceBetweenPlanets * planetIndex + distanceBetweenPlanets / 2.0f
  }

  private fun placePlanets() {
    val width = width
    if (width == 0) {
      post { placePlanets() }
      return
    }
    val density = context.resources.displayMetrics.density
    for (i in planetInfos.indices) {
      val planetInfo = planetInfos[i]
      val distanceFromSun = getDistanceFromSun(i)
      val y = 0f
      var angle = 0.5f / (planetInfos.size + 1)
      angle = (angle * (planetInfos.size - i - 1) * Math.PI + angle * Math.PI).toFloat()
      val centre = Vector2(distanceFromSun.toDouble(), y.toDouble())
      centre.rotate(angle.toDouble())
      centre.y += (20 * context.resources.displayMetrics.density).toDouble()
      planetInfo.centre = centre
      planetInfo.distanceFromSun = distanceFromSun
      planetInfo.imageView = ImageView(context)
      val lp = LayoutParams(
          (64 * density).toInt(), (64 * density).toInt())
      lp.topMargin = centre.y.toInt() - lp.height / 2
      lp.leftMargin = centre.x.toInt() - lp.width / 2
      planetInfo.imageView.layoutParams = lp
      planetInfo.imageView.tag = planetInfo
      planetInfo.imageView.setOnClickListener(planetOnClickListener)
      ViewCompat.setTransitionName(planetInfo.imageView, "planet_icon_$i")
      addView(planetInfo.imageView)
      Picasso.get()
          .load(ImageHelper.getPlanetImageUrl(context, star!!, i, 64, 64))
          .into(planetInfo.imageView)
      if (planetInfo.planet.colony != null) {
        for (building in planetInfo.planet.colony.buildings) {
          val design = DesignHelper.getDesign(building.design_type)
          if (design.show_in_solar_system) {
            planetInfo.buildings.add(building)
          }
        }

        if (planetInfo.buildings.isNotEmpty()) {
          planetInfo.buildings.sortWith(Comparator {
            lhs: Building, rhs: Building -> lhs.design_type.compareTo(rhs.design_type)
          })

          val angleOffset = (Math.PI / 8.0).toFloat() * (planetInfo.buildings.size - 1) / 2.0f
          for (buildingIndex in planetInfo.buildings.indices) {
            val building = planetInfo.buildings[buildingIndex]
            val design = DesignHelper.getDesign(building.design_type)
            val pt = Vector2(0.0, -56.0 * density)
            pt.rotate(angleOffset - (Math.PI / 8.0) * buildingIndex)

            val buildingImageView =
                if (buildingIndex < planetInfo.buildingImageViews.size)
                  planetInfo.buildingImageViews[buildingIndex]
                else ImageView(context)

            val lpBuilding = LayoutParams(
                (20 * density).toInt(),
                (20 * density).toInt())
            lpBuilding.topMargin = (centre.y + pt.y).toInt()
            lpBuilding.leftMargin = (centre.x + pt.x).toInt() - lpBuilding.width / 2

            buildingImageView.layoutParams = lpBuilding
            BuildViewHelper.setDesignIcon(design, buildingImageView)
            addView(buildingImageView)
          }
        }

        val lpColony = LayoutParams(
            (20 * density).toInt(),
            (20 * density).toInt())
        lpColony.topMargin = (centre.y + lp.height / 2).toInt()
        lpColony.leftMargin = centre.x.toInt() - lpColony.width / 2

        val colonyImageView = ImageView(context)
        planetInfo.colonyImageView = colonyImageView
        colonyImageView.layoutParams = lpColony
        ImageHelper.bindEmpireShield(
            colonyImageView, EmpireManager.getEmpire(planetInfo.planet.colony.empire_id))
        addView(colonyImageView)
      }
    }
    updateSelection()
  }

  private fun updateSelection() {
    val selection = selectedPlanet
    if (selection != null) {
      if (selectionIndicator.width == 0) {
        // If it doesn't have a width, make it visible then re-update the selection once it's width
        // has been calculated.
        selectionIndicator.visibility = View.VISIBLE
        selectionIndicator.post { updateSelection() }
        return
      }

      val params = selectionIndicator.layoutParams as LayoutParams
      params.leftMargin = (planetInfos[selection.index].centre.x - selectionIndicator.width / 2).toInt()
      params.topMargin = (planetInfos[selection.index].centre.y - selectionIndicator.height / 2).toInt()
      selectionIndicator.layoutParams = params
      selectionIndicator.visibility = View.VISIBLE
    } else {
      selectionIndicator.visibility = View.GONE
    }
    planetSelectedHandler?.onPlanetSelected(selection)
  }

  private val planetOnClickListener = OnClickListener { v ->
    val planetInfo = v.tag as PlanetInfo
    selectedPlanet = planetInfo.planet
    updateSelection()
  }

  private val onBackgroundDrawHandler: OnDrawHandler = object : OnDrawHandler {
    override fun onDraw(canvas: Canvas?) {
      for (i in planetInfos.indices) {
        val radius = getDistanceFromSun(i)
        val y = 20.0f * getContext().resources.displayMetrics.density
        canvas!!.drawCircle(0.0f, y, radius, orbitPaint)
      }
    }
  }

  /** This class contains info about the planets we need to render and interact with.  */
  private class PlanetInfo(val planet: Planet, var centre: Vector2, var distanceFromSun: Float) {
    lateinit var imageView: ImageView
    var colonyImageView: ImageView? = null
    var buildings: MutableList<Building> = ArrayList()
    var buildingImageViews: MutableList<ImageView> = ArrayList()
  }
}