package au.com.codeka.warworlds.game.empire;

import android.view.LayoutInflater;
import android.view.View;

import androidx.fragment.app.Fragment;
import au.com.codeka.warworlds.R;

public class BaseFragment extends Fragment {
    /** Gets a view to display if we're still loading the empire details. */
    protected View getLoadingView(LayoutInflater inflator) {
        return inflator.inflate(R.layout.empire_loading_tab, null);
    }
}
