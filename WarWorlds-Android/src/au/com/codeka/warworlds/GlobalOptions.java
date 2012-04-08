package au.com.codeka.warworlds;

import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.content.SharedPreferences;

public class GlobalOptions {
    private static CopyOnWriteArrayList<OptionsChangedListener> mOptionsChangedListeners =
            new CopyOnWriteArrayList<OptionsChangedListener>();

    public static void addOptionsChangedListener(OptionsChangedListener listener) {
        if (!mOptionsChangedListeners.contains(listener)) {
            mOptionsChangedListeners.add(listener);
        }
    }

    public static void removeOptionsChangedListener(OptionsChangedListener listener) {
        mOptionsChangedListeners.remove(listener);
    }

    protected void fireOptionsChanged(GlobalOptions newOptions) {
        for(OptionsChangedListener listener : mOptionsChangedListeners) {
            listener.onOptionsChanged(newOptions);
        }
    }

    public enum GraphicsDetail {
        LOW (0),
        MEDIUM (1),
        HIGH (2);

        private int mValue;
        GraphicsDetail(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static GraphicsDetail fromValue(int value) {
            for(GraphicsDetail d : GraphicsDetail.values()) {
                if (d.getValue() == value) {
                    return d;
                }
            }

            return GraphicsDetail.HIGH;
        }
    };

    private SharedPreferences mPreferences;

    public GlobalOptions(Context context) {
        mPreferences = Util.getSharedPreferences(context);
    }

    public GraphicsDetail getGraphicsDetail() {
        int value = mPreferences.getInt("GlobalOptions.GraphicsDetail", GraphicsDetail.HIGH.getValue());
        return GraphicsDetail.fromValue(value);
    }

    public void setGraphicsDetail(GraphicsDetail detail) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt("GlobalOptions.GraphicsDetail", detail.getValue());
        editor.commit();

        fireOptionsChanged(this);
    }

    /**
     * Pass this to \c addOnOptionsChangedListener to be notified when options change.
     */
    public interface OptionsChangedListener {
        void onOptionsChanged(GlobalOptions newOptions);
    }
}
