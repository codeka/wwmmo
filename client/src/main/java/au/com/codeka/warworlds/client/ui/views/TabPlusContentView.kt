package au.com.codeka.warworlds.client.ui.views

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import au.com.codeka.warworlds.client.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener

/**
 * A simple layout that just shows tabs at the top and content underneath.
 */
abstract class TabPlusContentView(context: Context) : FrameLayout(context) {
  private val tabLayout: TabLayout
  protected val tabContent: FrameLayout
  protected fun addTab(titleResId: Int) {
    tabLayout.addTab(tabLayout.newTab().setText(titleResId))
  }

  protected abstract fun onTabSelected(tab: TabLayout.Tab?, index: Int)

  private val tabSelectedListener: OnTabSelectedListener = object : OnTabSelectedListener {
    override fun onTabSelected(tab: TabLayout.Tab) {
      this@TabPlusContentView.onTabSelected(tab, tab.position)
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {}
    override fun onTabReselected(tab: TabLayout.Tab) {}
  }

  init {
    tabLayout = TabLayout(context, null, R.style.TabLayout)
    tabLayout.layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT)
    tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
    addView(tabLayout)
    tabContent = FrameLayout(context)
    val lp = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT)
    tabLayout.post {

      // Once the TabLayout has laid itself out, we can work out it's height.
      lp.topMargin = tabLayout.height
      tabContent.layoutParams = lp
      tabSelectedListener.onTabSelected(tabLayout.getTabAt(0))
    }
    addView(tabContent)
    tabLayout.addOnTabSelectedListener(tabSelectedListener)
  }
}