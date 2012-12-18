package au.com.codeka.warworlds;

import java.util.ArrayList;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Similar to \c AlertDialog, except with our own styling.
 */
public class StyledDialog extends Dialog {
    private Builder mBuilder;

    private StyledDialog(Context context, Builder builder) {
        super(context);
        mBuilder = builder;
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
        wnd.setContentView(R.layout.styled_dialog);
        wnd.setLayout(FrameLayout.LayoutParams.WRAP_CONTENT,
                      FrameLayout.LayoutParams.WRAP_CONTENT);

        WindowManager.LayoutParams params = wnd.getAttributes();
        params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
        wnd.setAttributes((android.view.WindowManager.LayoutParams) params);

        if (mBuilder.mTitle != null) {
            TextView title = (TextView) wnd.findViewById(R.id.title);
            title.setText(mBuilder.mTitle);
        } else {
            wnd.findViewById(R.id.title_container).setVisibility(View.GONE);
        }

        FrameLayout content = (FrameLayout) wnd.findViewById(R.id.content);
        content.addView(mBuilder.mView);

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
        layoutButtons(buttons);
    }

    private void layoutButtons(ArrayList<Button> buttons) {
        if (buttons.size() == 0) {
            // they're all hidden, adjust the content so it fills up
            // the whole space
            View content = getWindow().findViewById(R.id.content);
            RelativeLayout.LayoutParams lp = 
                    (RelativeLayout.LayoutParams) content.getLayoutParams();
            lp.bottomMargin = 0;
            content.setLayoutParams(lp);
        } else if (buttons.size() == 1) {
            buttons.get(0).setBackgroundResource(R.drawable.dialog_button_right_bg);
        } else if (buttons.size() == 2) {
            buttons.get(0).setBackgroundResource(R.drawable.dialog_button_left_bg);
            buttons.get(1).setBackgroundResource(R.drawable.dialog_button_right_bg);
        } else {
            buttons.get(0).setBackgroundResource(R.drawable.dialog_button_left_bg);
            buttons.get(1).setBackgroundResource(R.drawable.dialog_button_left_bg);
            buttons.get(2).setBackgroundResource(R.drawable.dialog_button_right_bg);
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
