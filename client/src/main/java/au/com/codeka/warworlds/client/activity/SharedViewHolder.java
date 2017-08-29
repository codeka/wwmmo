package au.com.codeka.warworlds.client.activity;

import android.view.View;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Container class for holding shared views that we want to animate between fragment transitions.
 */
public class SharedViewHolder {
  private final ArrayList<SharedView> sharedViews;

  private SharedViewHolder(ArrayList<SharedView> sharedViews) {
    this.sharedViews = Preconditions.checkNotNull(sharedViews);
  }

  public ArrayList<SharedView> getSharedViews() {
    return sharedViews;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class SharedView {
    private int viewId;
    @Nullable private View view;
    private String transitionName;

    public SharedView(int viewId, String transitionName) {
      this.viewId = viewId;
      this.transitionName = transitionName;
    }

    public SharedView(@Nonnull View view, String transitionName) {
      this.view = view;
      this.transitionName = transitionName;
    }

    @Nullable
    public View getView() {
      return view;
    }

    public int getViewId() {
      return viewId;
    }

    public String getTransitionName() {
      return transitionName;
    }
  }

  public static class Builder {
    private final ArrayList<SharedView> sharedViews = new ArrayList<>();

    public Builder addSharedView(int viewId, String transitionName) {
      sharedViews.add(new SharedView(viewId, transitionName));
      return this;
    }

    public Builder addSharedView(View view, String transitionName) {
      sharedViews.add(new SharedView(view, transitionName));
      return this;
    }

    public SharedViewHolder build() {
      return new SharedViewHolder(sharedViews);
    }
  }
}
