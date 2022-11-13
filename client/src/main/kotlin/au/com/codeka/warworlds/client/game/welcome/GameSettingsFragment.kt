package au.com.codeka.warworlds.client.game.welcome

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import au.com.codeka.warworlds.client.BuildConfig
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.util.GameSettings
import au.com.codeka.warworlds.client.util.GameSettings.ChatProfanityFilter
import au.com.codeka.warworlds.client.util.GameSettings.getEnum
import au.com.codeka.warworlds.client.util.GameSettings.getString
import com.google.common.collect.ImmutableMap

class GameSettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
  override fun onCreateRecyclerView(
      inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?): RecyclerView {
    val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
    // Add a margin to allow for the action bar.
    (recyclerView.layoutParams as FrameLayout.LayoutParams).topMargin = (48 * requireContext().resources.displayMetrics.density).toInt()
    return recyclerView
  }

  override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
    preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
    var category = PreferenceCategory(requireContext(), null)
    category.setTitle(R.string.pref_category_chat)
    preferenceManager.preferenceScreen.addPreference(category)
    val chatProfanityFilterPref = ListPreference(requireContext())
    chatProfanityFilterPref.key = GameSettings.Key.CHAT_PROFANITY_FILTER.toString()
    chatProfanityFilterPref.setTitle(R.string.pref_chat_profanity_filter)
    populateListPreference(
        chatProfanityFilterPref,
        ChatProfanityFilter.values(),
        CHAT_PROFANITY_FILTER_DISPLAY)
    category.addPreference(chatProfanityFilterPref)
    if (BuildConfig.DEBUG) {
      category = PreferenceCategory(requireContext(), null)
      category.setTitle(R.string.pref_category_debug)
      preferenceManager.preferenceScreen.addPreference(category)
      val serverUrlPref = ListPreference(requireContext())
      serverUrlPref.key = GameSettings.Key.SERVER.toString()
      serverUrlPref.setTitle(R.string.pref_server_url)
      val urls = arrayOf(
          "http://wwmmo.codeka.com.au/",
          "http://10.0.2.2:8080/",
          "http://192.168.1.3:8080/"
      )
      val displayUrls = arrayOf(
          "wwmmo.codeka.com.au",
          "10.0.2.2:8080",
          "192.168.1.3:8080"
      )
      serverUrlPref.entries = displayUrls
      serverUrlPref.entryValues = urls
      category.addPreference(serverUrlPref)
    }
  }

  override fun onStart() {
    super.onStart()
    val actionBar = (activity as AppCompatActivity?)!!.supportActionBar
    if (actionBar != null) {
      actionBar.setTitle(R.string.options)
      actionBar.show()
      actionBar.setHomeButtonEnabled(true)
      actionBar.setDisplayHomeAsUpEnabled(true)
    }
    preferenceManager.sharedPreferences
        ?.registerOnSharedPreferenceChangeListener(this)
    refreshPreferenceSummaries()
  }

  override fun onStop() {
    super.onStop()
    preferenceManager.sharedPreferences
        ?.unregisterOnSharedPreferenceChangeListener(this)
    val actionBar = (activity as AppCompatActivity?)!!.supportActionBar
    actionBar?.hide()
  }

  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    refreshPreferenceSummaries()
  }

  private fun <T : Enum<*>?> populateListPreference(
      pref: ListPreference, enumValues: Array<T>, displayValues: Map<T, String>) {
    val entryValues = arrayOfNulls<String>(enumValues.size)
    val entryDisplay = arrayOfNulls<String>(enumValues.size)
    for (i in entryValues.indices) {
      entryValues[i] = enumValues[i].toString()
      entryDisplay[i] = displayValues[enumValues[i]]
    }
    pref.entryValues = entryValues
    pref.entries = entryDisplay
  }

  private fun refreshPreferenceSummaries() {
    var p = findPreference<Preference>(GameSettings.Key.CHAT_PROFANITY_FILTER.name)
    val filter = getEnum(
        GameSettings.Key.CHAT_PROFANITY_FILTER, ChatProfanityFilter::class.java)
    p!!.summary = CHAT_PROFANITY_FILTER_DISPLAY[filter]
    if (BuildConfig.DEBUG) {
      p = findPreference(GameSettings.Key.SERVER.name)
      p!!.summary = getString(GameSettings.Key.SERVER)
    }
  }

  companion object {
    val CHAT_PROFANITY_FILTER_DISPLAY: Map<ChatProfanityFilter, String> = ImmutableMap.of(
        ChatProfanityFilter.AllowAll, "Do not filter",
        ChatProfanityFilter.AllowMild, "Filter only strong profanity",
        ChatProfanityFilter.AllowNone, "Filter all profanity"
    )
  }
}