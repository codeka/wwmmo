package au.com.codeka.warworlds.client.game.starfield;

import android.content.Context;
import android.transition.TransitionManager;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.ctrl.ChatMiniView;

/**
 * Layout for {@link StarfieldScreen}.
 */
public class StarfieldLayout extends RelativeLayout {
  public interface Callbacks {
    void onChatClick(@Nullable Long roomId);
  }

  private final ViewGroup bottomPane;
  private final ChatMiniView chatMiniView;

  private ViewGroup bottomPaneContent;

  public StarfieldLayout(Context context, Callbacks callbacks) {
    super(context);
    inflate(context, R.layout.starfield, this);

    //  selectionDetailsView = (SelectionDetailsView) view.findViewById(R.id.selection_details);
    bottomPane = findViewById(R.id.bottom_pane);
    //  allianceBtn = (Button) view.findViewById(R.id.alliance_btn);
    //   empireBtn = (Button) view.findViewById(R.id.empire_btn);
    chatMiniView = findViewById(R.id.mini_chat);

    chatMiniView.setCallback(roomId -> callbacks.onChatClick(roomId));
  }

  public void showBottomPane(ViewGroup content, boolean instant) {
    this.bottomPaneContent = content;
    if (!instant) {
      TransitionManager.beginDelayedTransition(this);
    }
    bottomPane.removeAllViews();
    bottomPane.addView(content);
  }
}
