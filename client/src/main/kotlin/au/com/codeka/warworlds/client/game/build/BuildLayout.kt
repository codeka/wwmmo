package au.com.codeka.warworlds.client.game.build

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.transition.TransitionManager
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.client.game.world.StarManager
import au.com.codeka.warworlds.client.opengl.DimensionResolver
import au.com.codeka.warworlds.client.util.RomanNumeralFormatter.format
import au.com.codeka.warworlds.common.proto.*
import au.com.codeka.warworlds.common.proto.Design.DesignType
import au.com.codeka.warworlds.common.sim.DesignHelper
import com.google.common.base.Preconditions
import com.squareup.picasso.Picasso
import java.util.*

/**
 * Layout for [BuildScreen].
 */
class BuildLayout(
    context: Context,
    private var star: Star,
    private var colonies: List<Colony>,
    initialIndex: Int) : RelativeLayout(context) {
  private val colonyPagerAdapter: ColonyPagerAdapter
  private val viewPager: ViewPager
  private val planetIcon: ImageView
  private val planetName: TextView
  private val buildQueueDescription: TextView
  private val bottomPane: ViewGroup
  private var bottomPaneContentView: BottomPaneContentView? = null

  /** Called when the star is updated. We'll need to refresh our current view.  */
  fun refresh(star: Star, colonies: List<Colony>) {
    this.star = star
    this.colonies = colonies
    colonyPagerAdapter.refresh(star, colonies)
    if (bottomPaneContentView != null) {
      bottomPaneContentView!!.refresh(star)
    }
  }

  /** Show the "build" popup sheet for the given [Design].  */
  fun showBuildSheet(design: Design?) {
    val colony = colonies[viewPager.currentItem]
    bottomPaneContentView = BuildBottomPane(
        context, star, colony, design!!, object : BuildBottomPane.Callback {
      override fun onBuild(designType: DesignType?, count: Int) {
        StarManager.updateStar(star, StarModification(
            type = StarModification.MODIFICATION_TYPE.ADD_BUILD_REQUEST,
            colony_id = colony.id,
            design_type = designType,
            count = count))
        hideBottomSheet()
      }
    })
    TransitionManager.beginDelayedTransition(bottomPane)
    bottomPane.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
    bottomPane.removeAllViews()
    bottomPane.addView(bottomPaneContentView as View?)
  }

  /**
   * Show the "upgrade" sheet for upgrading the given [Building]. If the building is already
   * at max level, then nothing will happen.
   */
  fun showUpgradeSheet(building: Building?) {
    val colony = colonies[viewPager.currentItem]
    val design = DesignHelper.getDesign(building?.design_type!!)
    bottomPaneContentView = UpgradeBottomPane(
        context, star, colony, design, building, object : UpgradeBottomPane.Callback {
      override fun onUpgrade(building: Building?) {
        StarManager.updateStar(star, StarModification(
            type = StarModification.MODIFICATION_TYPE.ADD_BUILD_REQUEST,
            colony_id = colony.id,
            building_id = building?.id,
            design_type = building?.design_type))
        hideBottomSheet()
      }
    })
    TransitionManager.beginDelayedTransition(bottomPane)
    bottomPane.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
    bottomPane.removeAllViews()
    bottomPane.addView(bottomPaneContentView as View?)
  }

  /**
   * Show the "progress" sheet for a currently-in progress fleet build. The fleet will be non-null
   * if we're upgrading.
   */
  fun showProgressSheet(fleet: Fleet?, buildRequest: BuildRequest) {
    bottomPaneContentView = ProgressBottomPane(context, buildRequest, object : ProgressBottomPane.Callback {
      override fun onCancelBuild() {
        // "Cancel" has been clicked.
        StarManager.updateStar(star, StarModification(
            type = StarModification.MODIFICATION_TYPE.DELETE_BUILD_REQUEST,
            build_request_id = buildRequest.id))
        hideBottomSheet()
      }
    })
    TransitionManager.beginDelayedTransition(bottomPane)
    bottomPane.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
    bottomPane.removeAllViews()
    bottomPane.addView(bottomPaneContentView as View?)
  }

  fun hideBottomSheet() {
    bottomPaneContentView = null
    TransitionManager.beginDelayedTransition(bottomPane)
    bottomPane.layoutParams.height = DimensionResolver(context).dp2px(30f).toInt()
    bottomPane.removeAllViews()
  }

  fun refreshColonyDetails(colony: Colony) {
    var planet: Planet? = null
    for (p in star.planets) {
      val c = p.colony ?: continue
      if (c.id == colony.id) {
        planet = p
      }
    }
    Picasso.get()
        .load(ImageHelper.getPlanetImageUrl(context, star, planet!!.index, 64, 64))
        .into(planetIcon)
    planetName.text = String.format(Locale.US, "%s %s", star.name,
        format(planet.index + 1))
    val buildQueueLength = colony.build_requests.size
    if (buildQueueLength == 0) {
      buildQueueDescription.text = "Build queue: idle"
    } else {
      buildQueueDescription.text = String.format(Locale.ENGLISH,
          "Build queue: %d", buildQueueLength)
    }
  }

  private val pageChangeListener: OnPageChangeListener = object : OnPageChangeListener {
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) {
      refreshColonyDetails(colonies[position])
    }

    override fun onPageScrollStateChanged(state: Int) {}
  }

  init {
    View.inflate(context, R.layout.build, this)
    colonyPagerAdapter = ColonyPagerAdapter(context, star, colonies, this)
    viewPager = findViewById(R.id.pager)
    viewPager.adapter = colonyPagerAdapter
    viewPager.currentItem = initialIndex
    planetIcon = findViewById(R.id.planet_icon)
    planetName = findViewById(R.id.planet_name)
    buildQueueDescription = findViewById(R.id.build_queue_description)
    bottomPane = findViewById(R.id.bottom_pane)
    viewPager.addOnPageChangeListener(pageChangeListener)
  }
}