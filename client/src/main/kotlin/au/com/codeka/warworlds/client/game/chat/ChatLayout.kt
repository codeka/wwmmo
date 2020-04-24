package au.com.codeka.warworlds.client.game.chat

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.transition.TransitionManager
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.common.proto.ChatRoom
import java.util.*

/**
 * Layout for [ChatScreen].
 */
class ChatLayout(context: Context, private val callbacks: Callbacks) : RelativeLayout(context) {
  interface Callbacks {
    fun onSend(msg: String?)
  }

  private val viewPager: ViewPager
  private val bottomPane: FrameLayout
  private val adapter: ChatPagerAdapter
  fun refresh(rooms: List<ChatRoom>?) {
    adapter.refresh(rooms)
  }

  /** Show the default, send, pane.  */
  private fun showSendPane() {
    val sendBottomPane = SendBottomPane(context, object : SendBottomPane.Callback {
      override fun onSendClick(message: String?) {
        callbacks.onSend(message)
      }
    })
    TransitionManager.beginDelayedTransition(bottomPane)
    bottomPane.removeAllViews()
    bottomPane.addView(sendBottomPane)
  }

  inner class ChatPagerAdapter : PagerAdapter() {
    private val rooms: MutableList<ChatRoom> = ArrayList()
    fun refresh(rooms: List<ChatRoom>?) {
      this.rooms.clear()
      this.rooms.addAll(rooms!!)
      notifyDataSetChanged()
    }

    override fun instantiateItem(parent: ViewGroup, position: Int): Any {
      val rv = RoomView(context, rooms[position])
      parent.addView(rv)
      return rv
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
      collection.removeView(view as RoomView)
    }

    override fun getItemPosition(item: Any): Int {
      if (item is ChatRoom) {
        for (i in rooms.indices) {
          if (rooms[i].id == item.id) {
            return i
          }
        }
      }
      return POSITION_NONE
    }

    override fun getCount(): Int {
      return rooms.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
      return rooms[position].name
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
      super.setPrimaryItem(container, position, `object`)

      //ChatRoom room = rooms.get(position);
      //room.markAllRead()
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
      return view === `object`
    }
  }

  init {
    View.inflate(context, R.layout.chat, this)
    setBackgroundColor(context.resources.getColor(R.color.default_background))
    adapter = ChatPagerAdapter()
    viewPager = findViewById(R.id.pager)
    viewPager.adapter = adapter
    bottomPane = findViewById(R.id.bottom_pane)
    showSendPane()
  }
}