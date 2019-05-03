package au.com.codeka.warworlds;

import java.util.ArrayList;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import javax.annotation.Nullable;

import au.com.codeka.common.Log;

/**
 * Similar to \c AlertDialog, except with our own styling.
 */
public class StyledDialog extends Dialog implements ViewTreeObserver.OnGlobalLayoutListener {
  private static final Log log = new Log("StyledDialog");
  private View contentView;
  private Builder builder;
  private Context context;
  private boolean buttonsVisible;
  private boolean titleVisible;

  private StyledDialog(Context context, Builder builder) {
    super(context);
    this.builder = builder;
    this.context = context;
  }

  public static void showErrorMessage(Context context, String message) {
    if (message == null || message.equals("")) {
      message = "An unknown or unexpected error occurred.";
    }

    new StyledDialog.Builder(context)
        .setMessage(message)
        .setTitle("Error")
        .setNeutralButton("OK", null)
        .create().show();
  }

  @Nullable
  public Button getPositiveButton() {
    Window wnd = getWindow();
    if (wnd == null) {
      return null;
    }
    return (Button) wnd.findViewById(R.id.positive_btn);
  }

  @Nullable
  public Button getNeutralButton() {
    Window wnd = getWindow();
    if (wnd == null) {
      return null;
    }
    return (Button) wnd.findViewById(R.id.neutral_btn);
  }

  @Nullable
  public Button getNegativeButton() {
    Window wnd = getWindow();
    if (wnd == null) {
      return null;
    }
    return (Button) wnd.findViewById(R.id.negative_btn);
  }

  /**
   * A helper method to enable/disable the buttons (and ability to cancel) so that the user can't
   * close the dialog while some operating is in progress.
   */
  public void setCloseable(boolean closeable) {
    Button btn;
    btn = getPositiveButton();
    if (btn != null) {
      btn.setEnabled(closeable);
    }
    btn = getNeutralButton();
    if (btn != null) {
      btn.setEnabled(closeable);
    }
    btn = getNegativeButton();
    if (btn != null) {
      btn.setEnabled(closeable);
    }
    setCancelable(closeable);
  }

  @Override
  public void show() {
    try {
      super.show();
    } catch (WindowManager.BadTokenException e) {
      // this can happen if the activity has been stopped... just ignore it.
    }
  }

  public void show(boolean ignoreErrors) {
    try {
      super.show();
    } catch (Exception e) {
      log.error("Error showing dialog.", e);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstance) {
    super.onCreate(savedInstance);
    Window wnd = getWindow();
    if (wnd == null) {
      return;
    }
    wnd.requestFeature(Window.FEATURE_NO_TITLE);

    contentView = getLayoutInflater().inflate(R.layout.styled_dialog, null);
    wnd.setContentView(contentView);

    if (builder.title != null) {
      TextView title = wnd.findViewById(R.id.title);
      title.setText(builder.title);
      titleVisible = true;
    } else {
      wnd.findViewById(R.id.title_container).setVisibility(View.GONE);
      titleVisible = false;
    }

    FrameLayout content = wnd.findViewById(R.id.content);
    content.addView(builder.view);

    layoutButtons(wnd);
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    contentView.getViewTreeObserver().addOnGlobalLayoutListener(this);
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      contentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    } else {
      contentView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }
  }

  /**
   * This is called whenever the layout changes. We want to make sure there's enough space for the
   * title and/or buttons (if required). Normally, the content would expand to fill the whole space
   * if we didn't have this.
   * <p>
   * Note that currently, this does not take into account what happens when the dialog *shrinks*
   * but in practise that never happens.
   */
  @Override
  public void onGlobalLayout() {
    final Window wnd = getWindow();
    if (wnd == null) {
      return;
    }
    FrameLayout scrollView = wnd.findViewById(R.id.scroll_view);

    double pixelScale = context.getResources().getDisplayMetrics().density;

    int availableHeight = wnd.getDecorView().getHeight();
    availableHeight -= 12 * pixelScale;
    if (titleVisible) {
      availableHeight -= 60 * pixelScale;
    }
    if (buttonsVisible) {
      availableHeight -= 40 * pixelScale;
    }
    int scrollViewHeight = scrollView.getHeight();

    int displayHeight = context.getResources().getDisplayMetrics().heightPixels;
    if (availableHeight > (displayHeight - 200)) {
      availableHeight = displayHeight - 200; // fudge factor
    }
    if (availableHeight > 2048) {
      availableHeight = 2048;
    }

    log.info("available height: %d; content height: %d; content height in dp: %d",
        availableHeight, scrollViewHeight, (int) (scrollViewHeight / pixelScale));

    if (availableHeight < scrollViewHeight) {
      RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)
          scrollView.getLayoutParams();
      lp.height = availableHeight;
      scrollView.setLayoutParams(lp);
    }
  }

  private void layoutButtons(Window wnd) {
    ArrayList<Button> buttons = new ArrayList<>();
    if (builder.negativeLabel != null) {
      Button btn = wnd.findViewById(R.id.negative_btn);
      btn.setText(builder.negativeLabel);
      btn.setOnClickListener(
          new ButtonClickListener(builder.negativeClickListener, Dialog.BUTTON_NEGATIVE, false));
      buttons.add(btn);
    } else {
      wnd.findViewById(R.id.negative_btn).setVisibility(View.GONE);
    }
    if (builder.neutralLabel != null) {
      Button btn = wnd.findViewById(R.id.neutral_btn);
      btn.setText(builder.neutralLabel);
      btn.setOnClickListener(
          new ButtonClickListener(builder.neutralClickListener, Dialog.BUTTON_NEUTRAL, false));
      buttons.add(btn);
    } else {
      wnd.findViewById(R.id.neutral_btn).setVisibility(View.GONE);
    }
    if (builder.positiveLabel != null) {
      Button btn = wnd.findViewById(R.id.positive_btn);
      btn.setText(builder.positiveLabel);
      btn.setOnClickListener(
          new ButtonClickListener(
              builder.positiveClickListener,
              Dialog.BUTTON_POSITIVE,
              builder.positiveAutoClose));
      buttons.add(btn);
    } else {
      wnd.findViewById(R.id.positive_btn).setVisibility(View.GONE);
    }

    if (buttons.size() == 0) {
      // they're all hidden, adjust the content so it fills up the whole space.
      View content = wnd.findViewById(R.id.content);
      FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) content.getLayoutParams();
      lp.bottomMargin = 0;
      content.setLayoutParams(lp);
      buttonsVisible = false;
    } else if (buttons.size() == 1) {
      buttons.get(0).setBackgroundResource(R.drawable.dialog_button_right_bg);
      buttonsVisible = true;
    } else if (buttons.size() == 2) {
      buttons.get(0).setBackgroundResource(R.drawable.dialog_button_left_bg);
      buttons.get(1).setBackgroundResource(R.drawable.dialog_button_right_bg);
      buttonsVisible = true;
    } else {
      buttons.get(0).setBackgroundResource(R.drawable.dialog_button_left_bg);
      buttons.get(1).setBackgroundResource(R.drawable.dialog_button_left_bg);
      buttons.get(2).setBackgroundResource(R.drawable.dialog_button_right_bg);
      buttonsVisible = true;
    }
  }

  private class ButtonClickListener implements View.OnClickListener {
    private DialogInterface.OnClickListener mOtherListener;
    private int mWhich;
    private boolean mAutoClose;

    public ButtonClickListener(
        DialogInterface.OnClickListener otherListener, int which, boolean autoClose) {
      mOtherListener = otherListener;
      mWhich = which;
      mAutoClose = autoClose;
    }

    @Override
    public void onClick(View v) {
      if (mOtherListener != null) {
        mOtherListener.onClick(StyledDialog.this, mWhich);
      }
      if (mOtherListener == null || mAutoClose) {
        dismiss();
      }
    }
  }

  public static class Builder {
    private Context context;
    private CharSequence positiveLabel;
    private DialogInterface.OnClickListener positiveClickListener;
    private boolean positiveAutoClose;
    private CharSequence negativeLabel;
    private DialogInterface.OnClickListener negativeClickListener;
    private CharSequence neutralLabel;
    private DialogInterface.OnClickListener neutralClickListener;
    private View view;
    private CharSequence title;

    public Builder(Context context) {
      this.context = context;
    }

    public Builder setPositiveButton(CharSequence label, DialogInterface.OnClickListener listener) {
      return setPositiveButton(label, false, listener);
    }

    public Builder setPositiveButton(
        CharSequence label, boolean autoClose, final DialogInterface.OnClickListener listener) {
      positiveLabel = label;
      positiveClickListener = listener;
      positiveAutoClose = autoClose;
      return this;
    }

    public Builder setNegativeButton(CharSequence label, DialogInterface.OnClickListener listener) {
      negativeLabel = label;
      negativeClickListener = listener;
      return this;
    }

    public Builder setNeutralButton(CharSequence label, DialogInterface.OnClickListener listener) {
      neutralLabel = label;
      neutralClickListener = listener;
      return this;
    }

    public Builder setTitle(CharSequence title) {
      this.title = title;
      return this;
    }

    public Builder setMessage(CharSequence msg) {
      TextView tv = new TextView(context);
      tv.setText(msg);
      FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT,
          FrameLayout.LayoutParams.WRAP_CONTENT);
      lp.setMargins((int) (5 * context.getResources().getDisplayMetrics().density),
          (int) (10 * context.getResources().getDisplayMetrics().density),
          (int) (10 * context.getResources().getDisplayMetrics().density),
          (int) (5 * context.getResources().getDisplayMetrics().density));
      tv.setLayoutParams(lp);
      view = tv;
      return this;
    }

    public Builder setView(View view) {
      this.view = view;
      return this;
    }

    public StyledDialog create() {
      return new StyledDialog(context, this);
    }
  }
}
