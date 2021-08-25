package au.com.codeka.warworlds.client.game.solarsystem

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.transition.TransitionManager
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.ctrl.ColonyFocusView
import au.com.codeka.warworlds.client.ctrl.fromHtml
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.common.proto.Colony
import au.com.codeka.warworlds.common.proto.Planet
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.sim.ColonyHelper
import com.squareup.wire.get
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The summary view of a planet that shows up in the bottom-left of the solarsystem screen.
 */
class PlanetSummaryView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
  interface Callbacks {
    fun onViewClick()
  }

  private var callbacks: Callbacks? = null
  private val emptyViewButton: Button
  private val colonyDetailsContainer: View
  private val enemyColonyDetailsContainer: View
  private val populationCountTextView: TextView
  private val colonyFocusView: ColonyFocusView
  fun setCallbacks(callbacks: Callbacks?) {
    this.callbacks = callbacks
  }

  fun setPlanet(planet: Planet) {
    TransitionManager.beginDelayedTransition(this)
    val colony = planet.colony
    if (colony == null) {
      emptyViewButton.visibility = View.VISIBLE
      colonyDetailsContainer.visibility = View.GONE
      enemyColonyDetailsContainer.visibility = View.GONE
      refreshUncolonizedDetails()
    } else {
      emptyViewButton.visibility = View.GONE
      colonyDetailsContainer.visibility = View.GONE
      enemyColonyDetailsContainer.visibility = View.GONE
      if (colony.empire_id == 0L) {
        enemyColonyDetailsContainer.visibility = View.VISIBLE
        refreshNativeColonyDetails(colony)
      } else {
        val colonyEmpire = EmpireManager.getEmpire(colony.empire_id)
        if (colonyEmpire != null) {
          val myEmpire = EmpireManager.getMyEmpire()
          if (myEmpire.id == colonyEmpire.id) {
            colonyDetailsContainer.visibility = View.VISIBLE
            refreshColonyDetails(planet, colony)
          } else {
            enemyColonyDetailsContainer.visibility = View.VISIBLE
            refreshEnemyColonyDetails(colony)
          }
        } else {
          // TODO: wait for the empire to come in.
        }
      }
    }
  }

  private fun refreshUncolonizedDetails() {
    populationCountTextView.text = context.getString(R.string.uncolonized)
  }

  private fun refreshColonyDetails(planet: Planet, colony: Colony) {
    val pop = ("Pop: "
        + colony.population.roundToInt()
        + " <small>"
        + String.format(Locale.US, "(%s%d / hr)",
        if (get(colony.delta_population, 0.0f) < 0) "-" else "+",
        abs(get(colony.delta_population, 0.0f).roundToInt())
    ) + "</small> / "
        + ColonyHelper.getMaxPopulation(planet))
    populationCountTextView.text = fromHtml(pop)
    colonyFocusView.visibility = View.VISIBLE
    colonyFocusView.refresh(colony)
  }

  private fun refreshEnemyColonyDetails(colony: Colony) {
    populationCountTextView.text = String.format(Locale.US, "Population: %d",
      colony.population.roundToInt()
    )

/*    ImageView enemyIcon = (ImageView) mView.findViewById(R.id.enemy_empire_icon);
    TextView enemyName = (TextView) mView.findViewById(R.id.enemy_empire_name);
    TextView enemyDefence = (TextView) mView.findViewById(R.id.enemy_empire_defence);

    int defence = (int) (0.25 * mColony.getPopulation() * mColony.getDefenceBoost());
    if (defence < 1) {
      defence = 1;
    }
    enemyIcon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), empire));
    enemyName.setText(empire.getDisplayName());
    enemyDefence.setText(String.format(Locale.ENGLISH, "Defence: %d", defence));
  */
  }

  private fun refreshNativeColonyDetails(colony: Colony) {
    val pop = ("Pop: " + colony.population.roundToInt())
    populationCountTextView.text = fromHtml(pop)
    colonyFocusView.visibility = View.GONE
  }

  init {
    View.inflate(context, R.layout.solarsystem_planet_summary, this)
    emptyViewButton = findViewById(R.id.empty_view_btn)
    colonyDetailsContainer = findViewById(R.id.solarsystem_colony_details)
    enemyColonyDetailsContainer = findViewById(R.id.enemy_colony_details)
    populationCountTextView = findViewById(R.id.population_count)
    colonyFocusView = findViewById(R.id.colony_focus_view)
    emptyViewButton.setOnClickListener {
      if (callbacks != null) {
        callbacks!!.onViewClick()
      }
    }
  }
}