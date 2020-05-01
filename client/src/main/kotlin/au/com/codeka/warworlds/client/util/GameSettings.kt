package au.com.codeka.warworlds.client.util

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.BuildConfig
import com.google.common.base.Preconditions
import java.util.*
import kotlin.collections.ArrayList


/** Wrapper class around our {@link SharedPreferences} instance. */
object GameSettings {
  enum class ValueType {
    BOOLEAN,
    INT,
    STRING,
    ENUM
  }

  enum class ChatProfanityFilter {
    AllowAll,
    AllowMild,
    AllowNone,
  }

  /** An enumeration for the current state of the sign in process. */
  enum class SignInState {
    /** The initial state: you're an 'anonymous' user, with no email address. */
    ANONYMOUS,

    /** We're waiting for authentication or something to go through. */
    PENDING,

    /** You've logged in and we have your email address. */
    VERIFIED,
  }

  enum class Key(
      val valueType: ValueType, val defValue: Any, val enumType: Class<out Enum<*>>? = null) {
    /** If true, we'll automatically translate chat messages to English. */
    CHAT_AUTO_TRANSLATE(ValueType.BOOLEAN, false),

    /** How much we should filter chat message which contain profanity. */
    CHAT_PROFANITY_FILTER(
        ValueType.ENUM, ChatProfanityFilter.AllowAll, ChatProfanityFilter::class.java),

    /** The cookie used to authenicate with the server. */
    COOKIE(ValueType.STRING, ""),

    /** If you've associated with an email address, this is it. */
    EMAIL_ADDR(ValueType.STRING, ""),

    /** A unique ID that's guaranteed to not change (as long as the user doesn't clear app data) */
    INSTANCE_ID(ValueType.STRING, ""),

    /** Your current {@link SignInState}. */
    SIGN_IN_STATE(
        ValueType.ENUM, SignInState.ANONYMOUS, SignInState::class.java),

    /** The base URL of the server. */
    SERVER(ValueType.STRING, BuildConfig.DEFAULT_SERVER),

    /** Set to true after you've seen the warm welcome, so we don't show it again. */
    WARM_WELCOME_SEEN(ValueType.BOOLEAN, false)
  }

  interface SettingChangeHandler {
    fun onSettingChanged(key: Key)
  }

  class Editor(sharedPreferences: SharedPreferences) {
    private var editor: SharedPreferences.Editor
    private var committed: Boolean

    init {
      @SuppressLint("CommitPrefEdits") // we have our own commit() method.
      editor = sharedPreferences.edit()
      committed = false
    }

    fun setBoolean(key: Key, value: Boolean):  Editor {
      Preconditions.checkState(key.valueType == ValueType.BOOLEAN)
      editor.putBoolean(key.toString(), value)
      return this
    }

    fun <T : Enum<*>> setEnum(key: Key, value: T): Editor {
      Preconditions.checkArgument(key.valueType == ValueType.ENUM)
      Preconditions.checkArgument(key.enumType == value::class.java)
      editor.putString(key.toString(), value.toString())
      return this
    }

    fun setString(key: Key, value: String): Editor {
      Preconditions.checkState(key.valueType == ValueType.STRING)
      editor.putString(key.toString(), value)
      return this
    }

    fun commit() {
      editor.apply()
      committed = true
    }

    protected fun finalize() {
      if (!committed) {
        // TODO: log, or something?
        commit()
      }
    }
  }

  private var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App)
  private val settingChangeHandlers = ArrayList<(Key) -> Unit>()

  private val onPrefChangedListener = object : SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyName: String) {
      val key: Key
      try {
        key = Key.valueOf(keyName)
      } catch (e: IllegalArgumentException) {
        // This will happen if the setting isn't one of ours. Ignore.
        return
      }
      for (handler in settingChangeHandlers) {
        handler(key)
      }
    }
  }

  init {
    sharedPreferences.registerOnSharedPreferenceChangeListener(onPrefChangedListener)

    if (getString(Key.INSTANCE_ID) == "") {
      edit().setString(Key.INSTANCE_ID, UUID.randomUUID().toString()).commit()
    }
  }

  fun addSettingChangedHandler(handler: (Key) -> Unit) {
    settingChangeHandlers.add(handler)
  }

  fun removeSettingChangedHandler(handler: (Key) -> Unit) {
    settingChangeHandlers.remove(handler)
  }

  fun getBoolean(key: Key): Boolean {
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App)
    Preconditions.checkArgument(key.valueType == ValueType.BOOLEAN)
    return sharedPreferences.getBoolean(key.toString(), key.defValue as Boolean)
  }

  fun <T : Enum<*>> getEnum(key: Key, enumType: Class<T>): T {
    Preconditions.checkArgument(key.valueType == ValueType.ENUM)
    Preconditions.checkArgument(key.enumType == enumType)
    val strValue = sharedPreferences.getString(key.toString(), key.defValue.toString())

    return (enumType.enumConstants as Array<out T>).first { it.name == strValue }
  }

  fun getInt(key: Key): Int {
    Preconditions.checkArgument(key.valueType == ValueType.INT)
    return sharedPreferences.getInt(key.toString(), key.defValue as Int)
  }

  fun getString(key: Key): String {
    Preconditions.checkArgument(key.valueType == ValueType.STRING)
    return sharedPreferences.getString(key.toString(), key.defValue as String)!!
  }

  fun edit(): Editor {
    return Editor(sharedPreferences)
  }
}
