package au.com.codeka.warworlds.client.ui;

import com.google.common.base.Preconditions;

import java.util.ArrayList;

/**
 * Container class for holding shared views that we want to animate specially between screen
 * transitions.
 */
public class SharedViews {
  private final ArrayList<SharedView> sharedViews;

  private SharedViews(ArrayList<SharedView> sharedViews) {
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

    public SharedView(int viewId) {
      this.viewId = viewId;
    }

    public int getViewId() {
      return viewId;
    }
  }

  public static class Builder {
    private final ArrayList<SharedView> sharedViews = new ArrayList<>();

    public Builder addSharedView(int viewId) {
      sharedViews.add(new SharedView(viewId));
      return this;
    }

    public SharedViews build() {
      return new SharedViews(sharedViews);
    }
  }
}
