package au.com.codeka.warworlds.game.empire;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import au.com.codeka.warworlds.R;

class BaseFragment extends Fragment {
    /** Gets a view to display if we're still loading the empire details. */
    protected View getLoadingView(LayoutInflater inflator) {
        return inflator.inflate(R.layout.empire_loading_tab, null);
    }
}
