package au.com.codeka.warworlds.client.game.chat;

import android.content.Context;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.common.proto.ChatRoom;

/**
 * Layout for {@link ChatScreen}.
 */
public class ChatLayout extends RelativeLayout {
  interface Callbacks {
    void onSend(String msg);
  }

  private final Callbacks callbacks;
  private final ViewPager viewPager;
  private final FrameLayout bottomPane;
  private final ChatPagerAdapter adapter;

  public ChatLayout(Context context, Callbacks callbacks) {
    super(context);
    this.callbacks = callbacks;
    inflate(context, R.layout.chat, this);
    setBackgroundColor(context.getResources().getColor(R.color.default_background));

    adapter = new ChatPagerAdapter();
    viewPager = findViewById(R.id.pager);
    viewPager.setAdapter(adapter);
    bottomPane = findViewById(R.id.bottom_pane);

    showSendPane();
  }

  public void refresh(List<ChatRoom> rooms) {
    adapter.refresh(rooms);
  }

  /** Show the default, send, pane. */
  private void showSendPane() {
    SendBottomPane sendBottomPane = new SendBottomPane(getContext(), callbacks::onSend);

    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.removeAllViews();
    bottomPane.addView(sendBottomPane);
  }

  public class ChatPagerAdapter extends PagerAdapter {
    private final List<ChatRoom> rooms = new ArrayList<>();

    public void refresh(List<ChatRoom> rooms) {
      this.rooms.clear();
      this.rooms.addAll(rooms);
      notifyDataSetChanged();
    }

    @Override
    public Object instantiateItem(ViewGroup parent, int position) {
      RoomView rv = new RoomView(getContext(), rooms.get(position));
      parent.addView(rv);
      return rv;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
      collection.removeView((RoomView) view);
    }

    @Override
    public int getItemPosition(Object item) {
      if (item instanceof ChatRoom) {
        ChatRoom room = (ChatRoom) item;
        for (int i = 0; i < rooms.size(); i++) {
          if (rooms.get(i).id.equals(room.id)) {
            return i;
          }
        }
      }
      return POSITION_NONE;
    }

    @Override
    public int getCount() {
      return rooms.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return rooms.get(position).name;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
      super.setPrimaryItem(container, position, object);

      //ChatRoom room = rooms.get(position);
      //room.markAllRead()
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
      return view == object;
    }
  }
}
