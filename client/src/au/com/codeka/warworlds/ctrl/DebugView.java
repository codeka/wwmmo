package au.com.codeka.warworlds.ctrl;

import java.util.Locale;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.api.RequestManager.RequestManagerState;

/**
 * This is a view that's displayed over all activities and shows up a little bit of debugging
 * information.
 */
public class DebugView extends FrameLayout
                       implements RequestManager.RequestManagerStateChangedHandler {
    private View mView;
    private Handler mHandler;

    public DebugView(Context context) {
        this(context, null);
    }

    public DebugView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mView = inflate(context, R.layout.debug_ctrl, null);
        addView(mView);
    }

    @Override
    public void onAttachedToWindow() {
        if (!isInEditMode()) {
            mHandler = new Handler();

            RequestManager.addRequestManagerStateChangedHandler(this);
            onStateChanged();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        if (!isInEditMode()) {
            RequestManager.removeRequestManagerStateChangedHandler(this);
        }
    }

    @Override
    public void onStateChanged() {
        // this is not called on the UI, so we have to send a request to the
        // UI thread to update the UI
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                TextView connectionInfo = (TextView) mView.findViewById(R.id.connection_info);
                RequestManagerState state = RequestManager.getCurrentState();
                if (state.numInProgressRequests > 0) {
                    String str = String.format(Locale.ENGLISH, "Conn: %d %s",
                                               state.numInProgressRequests,
                                               state.lastUri);
                    connectionInfo.setText(str);
                } else {
                    connectionInfo.setText("Conn: none");
                }
            }
        });
    }
}
