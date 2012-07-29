package au.com.codeka.warworlds;

import java.lang.reflect.Field;
import java.util.TreeMap;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import au.com.codeka.warworlds.game.FleetMoveDialog;
import au.com.codeka.warworlds.game.FleetSplitDialog;
import au.com.codeka.warworlds.game.solarsystem.BuildConfirmDialog;
import au.com.codeka.warworlds.game.solarsystem.FleetDialog;

/**
 * Manages the dialogs we show, since they could come from multiple activities it's easier
 * to manage them centrally.
 */
public class DialogManager {
    private static DialogManager sInstance = new DialogManager();
    public static DialogManager getInstance() {
        return sInstance;
    }

    private TreeMap<Integer, DialogInterface.OnDismissListener> mDismissListeners
                = new TreeMap<Integer, DialogInterface.OnDismissListener>();

    private DialogManager() {
    }

    public void show(Activity activity, Class<?> dialogClass) {
        show(activity, dialogClass, null, null);
    }

    public void show(Activity activity, Class<?> dialogClass, Bundle args) {
        show(activity, dialogClass, args, null);
    }

    public void show(Activity activity, Class<?> dialogClass, Bundle args,
                     DialogInterface.OnDismissListener onDismissListener) {
        int id = 0;
        try {
            Field idField = dialogClass.getDeclaredField("ID");
            id = idField.getInt(null);
        } catch (Exception e) {
            throw new RuntimeException("Dialog class requires ID static field.", e);
        }

        // if you've given us an onDismissListener, we'll save it so that when the activity
        // calls onPrepareDialog, we can populate the dialog's dismiss listener then.
        if (onDismissListener != null) {
            mDismissListeners.put(id, onDismissListener);
        }

        activity.showDialog(id, args);
    }

    /**
     * Activities should call this in their onCreateDialog method.
     */
    public Dialog onCreateDialog(Activity activity, int id) {
        switch(id) {
        case FleetSplitDialog.ID:
            return new FleetSplitDialog(activity);
        case FleetMoveDialog.ID:
            return new FleetMoveDialog(activity);
        case FleetDialog.ID:
            return new FleetDialog(activity);
        case BuildConfirmDialog.ID:
            return new BuildConfirmDialog(activity);
        }

        return null;
    }

    /**
     * Activities should call this in their onPrepareDialog method.
     */
    public void onPrepareDialog(Activity activity, int id, Dialog d, Bundle bundle) {
        if (d instanceof DialogConfigurable) {
            ((DialogConfigurable) d).setBundle(activity, bundle);
        }

        // if we have a dismiss listener for this dialog, then set it now
        DialogInterface.OnDismissListener dismissListener = mDismissListeners.get(id);
        if (dismissListener != null) {
            mDismissListeners.remove(id);
            d.setOnDismissListener(dismissListener);
        }
    }

    /**
     * Dialogs that want configuration data passed to them from our \c onPrepareDialog should
     * implement this interface.
     */
    public interface DialogConfigurable {
        void setBundle(Activity activity, Bundle bundle);
    }
}
