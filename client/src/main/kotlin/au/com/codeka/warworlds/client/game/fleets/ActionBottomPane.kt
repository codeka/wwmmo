package au.com.codeka.warworlds.client.game.fleets

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.common.proto.Fleet.FLEET_STANCE
import com.google.common.base.CaseFormat
import com.google.common.base.Preconditions

/**
 * Bottom pane of the fleets view that contains the main action buttons (split, move, etc).
 */
class ActionBottomPane(context: Context?, callback: Callback) : RelativeLayout(context, null) {
  /** Implement this to get notified of events.  */
  interface Callback {
    fun onMoveClick()
    fun onSplitClick()
    fun onMergeClick()
  }

  private val callback: Callback

  /** Called when you click 'split'.  */
  private fun onSplitClick(view: View) {
    callback.onSplitClick()
  }

  /** Called when you click 'merge'.  */
  private fun onMergeClick(view: View) {
    callback.onMergeClick()
  }

  /** Called when you click 'move'.  */
  private fun onMoveClick(view: View) {
    callback.onMoveClick()
  }

  inner class StanceAdapter internal constructor() : BaseAdapter(), SpinnerAdapter {
    var values: Array<FLEET_STANCE>

    override fun getCount(): Int {
      return values.size
    }

    override fun getItem(position: Int): Any {
      return values[position]
    }

    override fun getItemId(position: Int): Long {
      return values[position].value.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
      val view = getCommonView(position, convertView, parent)
      view.setTextColor(Color.WHITE)
      return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
      val view = getCommonView(position, convertView, parent)
      val lp: ViewGroup.LayoutParams = AbsListView.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
          FrameLayout.LayoutParams.MATCH_PARENT)
      lp.height = 80
      view.layoutParams = lp
      view.setTextColor(Color.WHITE)
      view.text = "  " + view.text.toString()
      return view
    }

    private fun getCommonView(position: Int, convertView: View?, parent: ViewGroup): TextView {
      val view: TextView
      if (convertView != null) {
        view = convertView as TextView
      } else {
        view = TextView(context)
        view.gravity = Gravity.CENTER_VERTICAL
      }
      val value = values[position]
      view.text = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, value.toString())
      return view
    }

    init {
      values = FLEET_STANCE.values()
    }
  }

  init {
    this.callback = Preconditions.checkNotNull(callback)
    View.inflate(context, R.layout.ctrl_fleet_action_bottom_pane, this)
    val stanceSpinner = findViewById<View>(R.id.stance) as Spinner
    stanceSpinner.adapter = StanceAdapter()
    findViewById<View>(R.id.split_btn).setOnClickListener { view: View -> onSplitClick(view) }
    findViewById<View>(R.id.merge_btn).setOnClickListener { view: View -> onMergeClick(view) }
    findViewById<View>(R.id.move_btn).setOnClickListener { view: View -> onMoveClick(view) }
  }
}