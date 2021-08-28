package au.com.codeka.warworlds.client.game.starfield

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.transition.TransitionManager
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.ctrl.ChatMiniView

/**
 * Layout for [StarfieldScreen].
 */
class StarfieldLayout(context: Context?, callbacks: Callbacks) : RelativeLayout(context) {
  interface Callbacks {
    fun onChatClick(roomId: Long?)
  }

  private val bottomPane: ViewGroup
  private val chatMiniView: ChatMiniView
  private var bottomPaneContent: ViewGroup? = null

  fun showBottomPane(content: ViewGroup, instant: Boolean) {
    bottomPaneContent = content
    if (!instant) {
      TransitionManager.beginDelayedTransition(this)
    }
    bottomPane.removeAllViews()
    bottomPane.addView(content)
  }

  init {
    View.inflate(context, R.layout.starfield, this)

    //  selectionDetailsView = (SelectionDetailsView) view.findViewById(R.id.selection_details);
    bottomPane = findViewById(R.id.bottom_pane)
    //  allianceBtn = (Button) view.findViewById(R.id.alliance_btn);
    //   empireBtn = (Button) view.findViewById(R.id.empire_btn);
    chatMiniView = findViewById(R.id.mini_chat)
    chatMiniView.setCallback(object : ChatMiniView.Callback {
      override fun showChat(conversationId: Long?) {
        callbacks.onChatClick(conversationId)
      }
    })
  }
}
