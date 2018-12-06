package au.com.codeka.warworlds.client.ui;

import android.view.View;

import androidx.annotation.Nullable;

public class ShowInfo {
  private final View view;
  private final boolean toolbarVisible;

  private ShowInfo(Builder builder) {
    this.view = builder.view;
    this.toolbarVisible = builder.toolbarVisible;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Nullable
  public View getView() {
    return view;
  }

  public boolean getToolbarVisible() {
    return toolbarVisible;
  }

  public static class Builder {
    private View view;
    private boolean toolbarVisible;

    private Builder() {
      toolbarVisible = true;
    }

    public Builder view(View view) {
      this.view = view;
      return this;
    }

    public Builder toolbarVisible(boolean toolbarVisible) {
      this.toolbarVisible = toolbarVisible;
      return this;
    }

    public ShowInfo build() {
      return new ShowInfo(this);
    }
  }
}
