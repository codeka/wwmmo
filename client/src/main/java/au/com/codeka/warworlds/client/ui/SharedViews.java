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
    private final int viewId;
    private final int fromViewId;
    private final int toViewId;

    public SharedView(int viewId) {
      this.viewId = viewId;
      this.fromViewId = 0;
      this.toViewId = 0;
    }

    public SharedView(int fromViewId, int toViewId) {
      this.viewId = 0;
      this.fromViewId = fromViewId;
      this.toViewId = toViewId;
    }

    public int getViewId() {
      return viewId;
    }

    public int getFromViewId() {
      return fromViewId;
    }

    public int getToViewId() {
      return toViewId;
    }
  }

  public static class Builder {
    private final ArrayList<SharedView> sharedViews = new ArrayList<>();

    public Builder addSharedView(int viewId) {
      sharedViews.add(new SharedView(viewId));
      return this;
    }

    public Builder addSharedView(int fromViewId, int toViewId) {
      sharedViews.add(new SharedView(fromViewId, toViewId));
      return this;
    }

    public SharedViews build() {
      return new SharedViews(sharedViews);
    }
  }
}
