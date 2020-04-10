package au.com.codeka.warworlds.client.ui;

import android.content.Context;
import android.view.View;

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

  public String toDebugString(Context context) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < sharedViews.size(); i++) {
      if (i == 0) {
        sb.append("[");
      } else {
        sb.append(", ");
      }
      sb.append(sharedViews.get(i).toDebugString(context));
    }
    sb.append("]");
    return sb.toString();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class SharedView {
    private final int viewId;
    private final View fromView;
    private final int fromViewId;
    private final int toViewId;

    public SharedView(int viewId) {
      this.viewId = viewId;
      this.fromView = null;
      this.fromViewId = 0;
      this.toViewId = 0;
    }

    public SharedView(int fromViewId, int toViewId) {
      this.viewId = 0;
      this.fromView = null;
      this.fromViewId = fromViewId;
      this.toViewId = toViewId;
    }

    public SharedView(View fromView, int toViewId) {
      this.viewId = 0;
      this.fromView = fromView;
      this.fromViewId = 0;
      this.toViewId = toViewId;
    }

    public int getViewId() {
      return viewId;
    }

    public View getFromView() {
      return fromView;
    }

    public int getFromViewId() {
      return fromViewId;
    }

    public int getToViewId() {
      return toViewId;
    }

    public String toDebugString(Context context) {
      if (viewId != 0) {
        return context.getResources().getResourceName(viewId);
      } else {
        String str;
        if (fromView != null) {
          str = fromView.toString();
        } else {
          str = context.getResources().getResourceName(fromViewId);
        }
        str += " -> ";
        str += context.getResources().getResourceName(toViewId);
        return str;
      }
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

    public Builder addSharedView(View fromView, int toViewId) {
      sharedViews.add(new SharedView(fromView, toViewId));
      return this;
    }

    public SharedViews build() {
      return new SharedViews(sharedViews);
    }
  }
}
