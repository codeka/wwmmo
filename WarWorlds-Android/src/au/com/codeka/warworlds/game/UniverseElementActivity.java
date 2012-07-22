package au.com.codeka.warworlds.game;

import java.util.concurrent.CopyOnWriteArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;

/**
 * This is the base class for the \c SolarSystemActivity and \c StarfieldActivity, mostly so
 * that we can share dialogs between the two, etc.
 */
public class UniverseElementActivity extends Activity {
    private CopyOnWriteArrayList<OnUpdatedListener> mUpdatedListeners;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUpdatedListeners = new CopyOnWriteArrayList<OnUpdatedListener>();
    }

    /**
     * Causes us to refresh the current star/sector/whatever.
     */
    public void refresh() {
    }

    public void addUpdatedListener(OnUpdatedListener listener) {
        if (!mUpdatedListeners.contains(listener)) {
            mUpdatedListeners.add(listener);
        }
    }

    public void removeUpdatedListener(OnUpdatedListener listener) {
        mUpdatedListeners.remove(listener);
    }

    protected void fireStarUpdated(Star star, Planet selectedPlanet, Colony colony) {
        for(OnUpdatedListener listener : mUpdatedListeners) {
            listener.onStarUpdated(star, selectedPlanet, colony);
        }
    }

    protected void fireSectorUpdated() {
        for (OnUpdatedListener listener : mUpdatedListeners) {
            listener.onSectorUpdated();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case FleetSplitDialog.ID:
            return new FleetSplitDialog(this);
        }

        return super.onCreateDialog(id);
    }


    @Override
    protected void onPrepareDialog(int id, Dialog d, Bundle args) {
        super.onPrepareDialog(id, d, args);
    }

    /**
     * One of these will be called when a star and/or the sector is updated with
     * data from the server.
     */
    public interface OnUpdatedListener {
        void onStarUpdated(Star star, Planet selectedPlanet, Colony colony);
        void onSectorUpdated();
    }
}
