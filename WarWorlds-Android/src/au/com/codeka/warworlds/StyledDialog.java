package au.com.codeka.warworlds;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Similar to \c AlertDialog, except with our own styling.
 */
public class StyledDialog extends Dialog implements ViewTreeObserver.OnGlobalLayoutListener {
    private Logger log = LoggerFactory.getLogger(StyledDialog.class);
    private Builder mBuilder;
    private Context mContext;
    private boolean mButtonsVisible;
    private boolean mTitleVisible;

    private StyledDialog(Context context, Builder builder) {
        super(context);
        mBuilder = builder;
        mContext = context;
    }

    public Button getPositiveButton() {
        return (Button) getWindow().findViewById(R.id.positive_btn);
    }
    public Button getNeutralButton() {
        return (Button) getWindow().findViewById(R.id.neutral_btn);
    }
    public Button getNegativeButton() {
        return (Button) getWindow().findViewById(R.id.negative_btn);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        Window wnd = getWindow();
        wnd.requestFeature(Window.FEATURE_NO_TITLE);

        View contentView = getLayoutInflater().inflate(R.layout.styled_dialog, null);
        wnd.setContentView(contentView);

        if (mBuilder.mTitle != null) {
            TextView title = (TextView) wnd.findViewById(R.id.title);
            title.setText(mBuilder.mTitle);
            mTitleVisible = true;
        } else {
            wnd.findViewById(R.id.title_container).setVisibility(View.GONE);
            mTitleVisible = false;
        }

        FrameLayout content = (FrameLayout) wnd.findViewById(R.id.content);
        content.addView(mBuilder.mView);

        layoutButtons(wnd);

        contentView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    /**
     * This is called whenever the layout changes. We want to make sure there's
     * enough space for the title and/or buttons (if required). Normally, the
     * content would expand to fill the whole space if we didn't have this.
     * 
     * Note that currently, this does not take into account what happens when
     * the dialog *shrinks* but in practise that never happens.
     */
    @Override
    public void onGlobalLayout() {
        FrameLayout content = (FrameLayout) getWindow().findViewById(R.id.content);

        double pixelScale = mContext.getResources().getDisplayMetrics().density;

        int availableHeight = getWindow().getDecorView().getHeight();
        availableHeight -= 12 * pixelScale;
        if (mTitleVisible) {
            availableHeight -= 60 * pixelScale;
        }
        if (mButtonsVisible) {
            availableHeight -= 40 * pixelScale;
        }
        int contentHeight = content.getHeight();

        log.info(String.format("available height: %d; content height: %d;",
                 availableHeight, contentHeight));

        if (availableHeight < contentHeight) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)
                    content.getLayoutParams();
            lp.height = availableHeight;
            content.setLayoutParams(lp);
        }
    }

    private void layoutButtons(Window wnd) {
        ArrayList<Button> buttons = new ArrayList<Button>();
        if (mBuilder.mNegativeLabel != null) {
            Button btn = (Button) wnd.findViewById(R.id.negative_btn);
            btn.setText(mBuilder.mNegativeLabel);
            if (mBuilder.mNegativeClickListener != null) {
                btn.setOnClickListener(mBuilder.mNegativeClickListener);
            } else {
                btn.setOnClickListener(new DefButtonClickListener());
            }
            buttons.add(btn);
        } else {
            wnd.findViewById(R.id.negative_btn).setVisibility(View.GONE);
        }
        if (mBuilder.mNeutralLabel != null) {
            Button btn = (Button) wnd.findViewById(R.id.neutral_btn);
            btn.setText(mBuilder.mNeutralLabel);
            if (mBuilder.mNeutralClickListener != null) {
                btn.setOnClickListener(mBuilder.mNeutralClickListener);
            } else {
                btn.setOnClickListener(new DefButtonClickListener());
            }
            buttons.add(btn);
        } else {
            wnd.findViewById(R.id.neutral_btn).setVisibility(View.GONE);
        }
        if (mBuilder.mPositiveLabel != null) {
            Button btn = (Button) wnd.findViewById(R.id.positive_btn);
            btn.setText(mBuilder.mPositiveLabel);
            if (mBuilder.mPositiveClickListener != null) {
                if (mBuilder.mPositiveAutoClose) {
                    btn.setOnClickListener(new DefButtonClickListener(mBuilder.mPositiveClickListener));
                } else {
                    btn.setOnClickListener(mBuilder.mPositiveClickListener);
                }
            } else {
                btn.setOnClickListener(new DefButtonClickListener());
            }
            buttons.add(btn);
        } else {
            wnd.findViewById(R.id.positive_btn).setVisibility(View.GONE);
        }

        if (buttons.size() == 0) {
            // they're all hidden, adjust the content so it fills up
            // the whole space
            View content = getWindow().findViewById(R.id.content);
            RelativeLayout.LayoutParams lp = 
                    (RelativeLayout.LayoutParams) content.getLayoutParams();
            lp.bottomMargin = 0;
            content.setLayoutParams(lp);
            mButtonsVisible = false;
        } else if (buttons.size() == 1) {
            buttons.get(0).setBackgroundResource(R.drawable.dialog_button_right_bg);
            mButtonsVisible = true;
        } else if (buttons.size() == 2) {
            buttons.get(0).setBackgroundResource(R.drawable.dialog_button_left_bg);
            buttons.get(1).setBackgroundResource(R.drawable.dialog_button_right_bg);
            mButtonsVisible = true;
        } else {
            buttons.get(0).setBackgroundResource(R.drawable.dialog_button_left_bg);
            buttons.get(1).setBackgroundResource(R.drawable.dialog_button_left_bg);
            buttons.get(2).setBackgroundResource(R.drawable.dialog_button_right_bg);
            mButtonsVisible = true;
        }
    }

    private class DefButtonClickListener implements View.OnClickListener {
        private View.OnClickListener mOtherListener;

        public DefButtonClickListener() {
        }
        public DefButtonClickListener(View.OnClickListener otherListener) {
            mOtherListener = otherListener;
        }

        @Override
        public void onClick(View v) {
            if (mOtherListener != null) {
                mOtherListener.onClick(v);
            }
            dismiss();
        }
    }

    public static class Builder {
        private Context mContext;
        private CharSequence mPositiveLabel;
        private View.OnClickListener mPositiveClickListener;
        private boolean mPositiveAutoClose;
        private CharSequence mNegativeLabel;
        private View.OnClickListener mNegativeClickListener;
        private CharSequence mNeutralLabel;
        private View.OnClickListener mNeutralClickListener;
        private View mView;
        private CharSequence mTitle;

        public Builder(Context context) {
            mContext = context;
        }

        public Builder setPositiveButton(CharSequence label, View.OnClickListener listener) {
            return setPositiveButton(label, false, listener);
        }
        public Builder setPositiveButton(CharSequence label, boolean autoClose, final View.OnClickListener listener) {
            mPositiveLabel = label;
            mPositiveClickListener = listener;
            mPositiveAutoClose = autoClose;
            return this;
        }
        public Builder setNegativeButton(CharSequence label, View.OnClickListener listener) {
            mNegativeLabel = label;
            mNegativeClickListener = listener;
            return this;
        }
        public Builder setNeutralButton(CharSequence label, View.OnClickListener listener) {
            mNeutralLabel = label;
            mNeutralClickListener = listener;
            return this;
        }

        public Builder setTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        public Builder setMessage(CharSequence msg) {
            TextView tv = new TextView(mContext);
            tv.setText(msg);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins((int)(5 * mContext.getResources().getDisplayMetrics().density),
                          (int)(10 * mContext.getResources().getDisplayMetrics().density),
                          (int)(10 * mContext.getResources().getDisplayMetrics().density),
                          (int)(5 * mContext.getResources().getDisplayMetrics().density));
            tv.setLayoutParams(lp);
            mView = tv;
            return this;
        }

        public Builder setView(View view) {
            mView = view;
            return this;
        }

        public StyledDialog create() {
            return new StyledDialog(mContext, this);
        }
    }
}
