package au.com.codeka.warworlds.game.starfield;

import android.app.Dialog;

/**
 * This dialog shows the status of the empire. You can see all your colonies, all your fleets, etc.
 */
public class EmpireDialog extends Dialog {
    private StarfieldActivity mActivity;

    public EmpireDialog(StarfieldActivity activity) {
        super(activity);
        mActivity = activity;
    }

}
