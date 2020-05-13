package au.com.codeka.warworlds.client.game.solarsystem

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.fleets.FleetListSimple
import au.com.codeka.warworlds.client.game.fleets.FleetListSimple.FleetSelectedHandler
import au.com.codeka.warworlds.client.util.RomanNumeralFormatter
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Fleet
import au.com.codeka.warworlds.common.proto.Planet
import au.com.codeka.warworlds.common.proto.Star

/**
 * The layout for the [SolarSystemScreen].
 */
// TODO: it probably makes sense to split these into a bunch of sub-views.
class SolarSystemLayout(
    context: Context?, callbacks: Callbacks, star: Star, startPlanetIndex: Int)
    : RelativeLayout(context) {
  interface Callbacks {
    fun onBuildClick(planetIndex: Int)
    fun onFocusClick(planetIndex: Int)
    fun onSitrepClick()
    fun onViewColonyClick(planetIndex: Int)
    fun onFleetClick(fleetId: Long)
  }

  private val sunAndPlanets: SunAndPlanetsView
  private val congeniality: CongenialityView
  private val planetSummary: PlanetSummaryView
  private val store: StoreView
  private val planetName: TextView
  private val fleetList: FleetListSimple

  // private final StarSearchListAdapter searchListAdapter;
  private var star: Star
  private var planetIndex: Int

  /**
   * Gets the [View] that's showing the planet with the given index.
   *
   * @param planetIndex The index of the planet whose [View] you want.
   * @return The [View] (actually, [ImageView] that the planet is being displayed in.
   */
  fun getPlanetView(planetIndex: Int): View? {
    return sunAndPlanets.getPlanetView(planetIndex)
  }

  fun refreshStar(star: Star) {
    this.star = star

    //   searchListAdapter.addToLastStars(star);
    fleetList.setStar(star)
    sunAndPlanets.setStar(star)
    store.setStar(star)
    if (planetIndex >= 0) {
      planetName.text = this.star.name
    }
    if (planetIndex >= 0) {
      log.debug("Selecting planet #%d", planetIndex)
      sunAndPlanets.selectPlanet(planetIndex)
    } else {
      log.debug("No planet selected")
    }
    refreshSelectedPlanet()
  }

  private fun refreshSelectedPlanet() {
    if (planetIndex < 0) {
      return
    }
    val planet = star.planets[planetIndex]
    val planetCentre = sunAndPlanets.getPlanetCentre(planet)
    val name = star.name + " " + RomanNumeralFormatter.format(star.planets.indexOf(planet) + 1)
    planetName.text = name
    if (planetCentre == null) {
      // this is probably because the SolarSystemView probably hasn't rendered yet. We'll
      // just ignore this then cause it'll fire an onPlanetSelected when it finishes
      // drawing.
      congeniality.visibility = View.GONE
    } else {
      val pixelScale = context.resources.displayMetrics.density
      val x = planetCentre.x
      val y = planetCentre.y

      // hard-coded size of the congeniality container: 85x64 dp
      var offsetX = (85 + 20) * pixelScale
      var offsetY = (64 + 20) * pixelScale
      if (x - offsetX < 0) {
        offsetX = -(20 * pixelScale)
      }
      if (y - offsetY < 20) {
        offsetY = -(20 * pixelScale)
      }
      val params = congeniality.layoutParams as LayoutParams
      params.leftMargin = (x - offsetX).toInt()
      params.topMargin = (y - offsetY).toInt()
      if (params.topMargin < 40 * pixelScale) {
        params.topMargin = (40 * pixelScale).toInt()
      }
      congeniality.layoutParams = params
      congeniality.visibility = View.VISIBLE
    }
    congeniality.setPlanet(planet)
    planetSummary.setPlanet(star, planet)
  }

  companion object {
    private val log = Log("SolarSystemLayout")
  }

  /**
   * Constructs a new [SolarSystemLayout].
   *
   * @param context The [Context].
   * @param star The [Star] to display initially.
   * @param startPlanetIndex The index of the planet to have initially select (or -1 for no planet).
   */
  init {
    View.inflate(context, R.layout.solarsystem, this)
    this.star = star
    planetIndex = startPlanetIndex
    sunAndPlanets = findViewById(R.id.solarsystem_view)
    congeniality = findViewById(R.id.congeniality)
    store = findViewById(R.id.store)
    planetSummary = findViewById(R.id.planet_summary)
    planetName = findViewById(R.id.planet_name)
    fleetList = findViewById(R.id.fleet_list)
    sunAndPlanets.setPlanetSelectedHandler(object : SunAndPlanetsView.PlanetSelectedHandler {
      override fun onPlanetSelected(planet: Planet?) {
        planetIndex = if (planet == null) {
          -1
        } else {
          planet.index
        }
        refreshSelectedPlanet()
      }
    })
    planetSummary.setCallbacks( object : PlanetSummaryView.Callbacks {
      override fun onViewClick() {
        callbacks.onViewColonyClick(planetIndex)
      }
    })
    val buildButton = findViewById<Button>(R.id.solarsystem_colony_build)
    val focusButton = findViewById<Button>(R.id.solarsystem_colony_focus)
    val sitrepButton = findViewById<Button>(R.id.sitrep_btn)
    val planetViewButton = findViewById<Button>(R.id.enemy_empire_view)
    buildButton.setOnClickListener { v: View? -> callbacks.onBuildClick(planetIndex) }
    focusButton.setOnClickListener { v: View? -> callbacks.onFocusClick(planetIndex) }
    sitrepButton.setOnClickListener { v: View? -> callbacks.onSitrepClick() }
    planetViewButton.setOnClickListener { v: View? -> callbacks.onViewColonyClick(planetIndex) }
    fleetList.setFleetSelectedHandler(object : FleetSelectedHandler {
      override fun onFleetSelected(fleet: Fleet?) {
        if (fleet == null) {
          return
        }

        callbacks.onFleetClick(fleet.id)
      }
    })
    /*
    ListView searchList = findViewById(R.id.search_result);
    searchListAdapter = new StarSearchListAdapter(
        LayoutInflater.from(getContext()));
    searchList.setAdapter(searchListAdapter);

    searchList.setOnItemClickListener((parent, v, position, id) -> {
      Star s = (Star) searchListAdapter.getItem(position);
      if (s != null) {
        refreshStar(s);
      }
    });
*/refreshStar(star)
  }
}