package au.com.codeka.warworlds.client.game.solarsystem

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.fleets.FleetListSimple
import au.com.codeka.warworlds.client.game.fleets.FleetListSimple.FleetFilter
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.client.util.NumberFormatter
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator.setBackground
import au.com.codeka.warworlds.common.proto.*
import java.util.*
import kotlin.math.roundToInt

/**
 * Layout for [PlanetDetailsScreen].
 */
@SuppressLint("ViewConstructor") // Must be constructed in code.
class PlanetDetailsLayout(
    context: Context?,
    star: Star,
    planet: Planet,
    callbacks: Callbacks)
  : RelativeLayout(context) {

  interface Callbacks {
    fun onSaveFocusClick(
        farmingFocus: Float, miningFocus: Float, energyFocus: Float, constructionFocus: Float)

    fun onAttackClick()
    fun onColonizeClick()
  }

  private val focusLocks = booleanArrayOf(false, false, false, false)
  private val focusValues = floatArrayOf(0.25f, 0.25f, 0.25f, 0.25f)
  private val star: Star
  private val planet: Planet
  private val congeniality: CongenialityView
  private val planetIcon: ImageView
  private val empireIcon: ImageView
  private val focusContainer: View
  private val focusSeekBars: Array<SeekBar>
  private val focusTextViews: Array<TextView>
  private val focusMinusButtons: Array<Button>
  private val focusPlusButtons: Array<Button>
  private val focusLockButtons: Array<ImageButton>
  private val empireName: TextView
  private val empireDefence: TextView
  private val note: TextView
  private val fleetList: FleetListSimple
  private val attackBtn: Button
  private val colonizeBtn: Button

  private fun refresh() {
    var empire: Empire? = null
    val colony = planet.colony
    if (colony?.empire_id != null) {
      empire = EmpireManager.getEmpire(colony.empire_id)
    }
    ImageHelper.bindPlanetIcon(planetIcon, star, planet)
    ImageHelper.bindEmpireShield(empireIcon, empire)
    when {
      empire != null -> {
        empireName.text = empire.display_name
      }
      planet.colony != null -> {
        empireName.setText(R.string.native_colony)
      }
      else -> {
        empireName.setText(R.string.uncolonized)
      }
    }
    congeniality.setPlanet(planet)
    if (colony != null) {
      empireDefence.text = String.format(
          Locale.ENGLISH,
          "Defence: %.0f",
          colony.defence_bonus!! * colony.population)
    }
    when {
      EmpireManager.isMyEmpire(empire) -> {
        // It's our colony.
        focusContainer.visibility = View.VISIBLE
        attackBtn.visibility = View.GONE
        colonizeBtn.visibility = View.GONE
        note.setText(R.string.focus_hint)
        focusValues[FARMING_INDEX] = colony?.focus?.farming ?: 0.25f
        focusValues[MINING_INDEX] = colony?.focus?.mining ?: 0.25f
        focusValues[ENERGY_INDEX] = colony?.focus?.energy ?: 0.25f
        focusValues[CONSTRUCTION_INDEX] = colony?.focus?.construction ?: 0.25f
        refreshFocus()
      }
      planet.colony != null -> {
        // It's an enemy colony (could be native or another player).
        focusContainer.visibility = View.GONE
        attackBtn.visibility = View.VISIBLE
        colonizeBtn.visibility = View.GONE
        fleetList.setStar(star, object : FleetFilter {
          override fun showFleet(fleet: Fleet?): Boolean {
            return fleet?.design_type == Design.DesignType.TROOP_CARRIER
          }
        })
        note.setText(
          if (fleetList.numFleets > 0) R.string.attack_hint else R.string.attack_hint_no_ships)
        attackBtn.isEnabled = fleetList.numFleets > 0
      }
      else -> {
        // It's uncolonized.
        focusContainer.visibility = View.GONE
        attackBtn.visibility = View.GONE
        colonizeBtn.visibility = View.VISIBLE
        fleetList.setStar(star, object: FleetFilter {
          override fun showFleet(fleet: Fleet?): Boolean {
            return fleet?.design_type == Design.DesignType.COLONY_SHIP
          }
        })
        note.setText(
          if (fleetList.numFleets > 0) R.string.colonize_hint else R.string.colonize_hint_no_ships)
        colonizeBtn.isEnabled = fleetList.numFleets > 0
      }
    }
  }

  private fun refreshFocus() {
    val colony = planet.colony ?: return
    val focus = colony.focus
    for (i in 0..3) {
      focusLockButtons[i].setImageResource(
          if (focusLocks[i]) R.drawable.lock_closed else R.drawable.lock_opened)
      focusSeekBars[i].progress = (focusValues[i] * 1000.0f).toInt()
      focusTextViews[i].text = NumberFormatter.format(Math.round(focusValues[i] * 100.0f))
    }
  }

  private fun onFocusLockClick(view: View) {
    for (i in 0..3) {
      if (focusLockButtons[i] === view) {
        focusLocks[i] = !focusLocks[i]
      }
    }
    refreshFocus()
  }

  private fun onFocusPlusClick(view: View) {
    for (i in 0..3) {
      if (view === focusPlusButtons[i]) {
        val newValue = 0.0f.coerceAtLeast(focusValues[i] + 0.01f)
        focusSeekBars[i].progress = (newValue * focusSeekBars[i].max).roundToInt()
        redistribute(i, newValue)
        break
      }
    }
    refreshFocus()
  }

  private fun onFocusMinusClick(view: View) {
    for (i in 0..3) {
      if (view === focusMinusButtons[i]) {
        val newValue = Math.max(0.0f, focusValues[i] - 0.01f)
        focusSeekBars[i].progress = Math.round(newValue * focusSeekBars[i].max)
        redistribute(i, newValue)
        break
      }
    }
    refreshFocus()
  }

  fun onFocusProgressChanged(
      changedSeekBar: SeekBar, progressValue: Int, fromUser: Boolean) {
    if (!fromUser) {
      return
    }
    for (i in 0..3) {
      if (focusSeekBars[i] === changedSeekBar) {
        redistribute(i, progressValue.toFloat() / changedSeekBar.max)
      }
    }
    refreshFocus()
  }

  private fun redistribute(changedIndex: Int, newValue: Float) {
    var otherValuesTotal = 0.0f
    for (i in 0..3) {
      if (i == changedIndex) {
        focusValues[i] = newValue
        continue
      }
      otherValuesTotal += focusValues[i]
    }
    val desiredOtherValuesTotal = 1.0f - newValue
    if (desiredOtherValuesTotal <= 0.0f) {
      for (i in 0..3) {
        if (i == changedIndex || focusLocks[i]) {
          continue
        }
        focusValues[i] = 0.0f
      }
      return
    }
    val ratio = otherValuesTotal / desiredOtherValuesTotal
    for (i in 0..3) {
      if (i == changedIndex || focusLocks[i]) {
        continue
      }
      var focus = focusValues[i]
      if (focus <= 0.001f) {
        focus = 0.001f
      }
      focusValues[i] = focus / ratio
    }
  }

  companion object {
    // The index into the focus arrays for each type of focus (farming, mining, etc).
    private const val FARMING_INDEX = 0
    private const val MINING_INDEX = 1
    private const val ENERGY_INDEX = 2
    private const val CONSTRUCTION_INDEX = 3
  }

  init {
    View.inflate(context, R.layout.planet_details, this)
    setBackground(
        findViewById(R.id.planet_background), null, star.id)
    this.star = star
    this.planet = planet
    congeniality = findViewById(R.id.congeniality)
    planetIcon = findViewById(R.id.planet_icon)
    empireIcon = findViewById(R.id.empire_icon)
    empireDefence = findViewById(R.id.empire_defence)
    focusContainer = findViewById(R.id.focus_container)
    note = findViewById(R.id.note)
    fleetList = findViewById(R.id.fleet_list)
    focusSeekBars = arrayOf(
        findViewById(R.id.focus_farming),
        findViewById(R.id.focus_mining),
        findViewById(R.id.focus_energy),
        findViewById(R.id.focus_construction))
    focusTextViews = arrayOf(
        findViewById(R.id.focus_farming_value),
        findViewById(R.id.focus_mining_value),
        findViewById(R.id.focus_energy_value),
        findViewById(R.id.focus_construction_value))
    focusMinusButtons = arrayOf(
        findViewById(R.id.focus_farming_minus_btn),
        findViewById(R.id.focus_mining_minus_btn),
        findViewById(R.id.focus_energy_minus_btn),
        findViewById(R.id.focus_construction_minus_btn))
    focusPlusButtons = arrayOf(
        findViewById(R.id.focus_farming_plus_btn),
        findViewById(R.id.focus_mining_plus_btn),
        findViewById(R.id.focus_energy_plus_btn),
        findViewById(R.id.focus_construction_plus_btn))
    focusLockButtons = arrayOf(
        findViewById(R.id.focus_farming_lock),
        findViewById(R.id.focus_mining_lock),
        findViewById(R.id.focus_energy_lock),
        findViewById(R.id.focus_construction_lock))
    empireName = findViewById(R.id.empire_name)
    attackBtn = findViewById(R.id.attack_btn)
    attackBtn.setOnClickListener { v: View? -> callbacks.onAttackClick() }
    colonizeBtn = findViewById(R.id.colonize_btn)
    colonizeBtn.setOnClickListener { v: View? -> callbacks.onColonizeClick() }
    for (i in 0..3) {
      focusLockButtons[i].setOnClickListener { view: View -> onFocusLockClick(view) }
      focusPlusButtons[i].setOnClickListener { view: View -> onFocusPlusClick(view) }
      focusMinusButtons[i].setOnClickListener { view: View -> onFocusMinusClick(view) }
      focusSeekBars[i].setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progressValue: Int, fromUser: Boolean) {
          onFocusProgressChanged(seekBar, progressValue, fromUser)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}
        override fun onStopTrackingTouch(seekBar: SeekBar) {}
      })
    }
    findViewById<View>(R.id.focus_save_btn).setOnClickListener { v: View? ->
      callbacks.onSaveFocusClick(
          focusValues[FARMING_INDEX],
          focusValues[MINING_INDEX],
          focusValues[ENERGY_INDEX],
          focusValues[CONSTRUCTION_INDEX])
    }
    refresh()
  }
}