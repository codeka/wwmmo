package au.com.codeka.warworlds.ctrl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.game.FleetSplitDialog;
import au.com.codeka.warworlds.game.UniverseElementActivity;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.ShipDesign;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.Star;

public class FleetList extends FrameLayout {
    private Logger log = LoggerFactory.getLogger(FleetList.class);
    private FleetListAdapter mFleetListAdapter;
    private Fleet mSelectedFleet;
    private Star mStar;
    private UniverseElementActivity mActivity;
    private boolean mIsInitialized;

    public FleetList(Context context, AttributeSet attrs) {
        super(context, attrs);

        View child = inflate(context, R.layout.fleet_list_ctrl, null);
        this.addView(child);
    }

    public void refresh(UniverseElementActivity activity, Star star, List<Fleet> fleets) {
        mActivity = activity;
        mStar = star;

        initialize();

        // if we had a fleet selected, make sure we still have the same
        // fleet selected after we refresh
        if (mSelectedFleet != null) {
            Fleet selectedFleet = mSelectedFleet;
            mSelectedFleet = null;

            for (Fleet f : mStar.getFleets()) {
                if (f.getKey().equals(selectedFleet.getKey())) {
                    mSelectedFleet = f;
                    break;
                }
            }
        }

        mFleetListAdapter.setFleets(fleets);
    }

    private void initialize() {
        if (mIsInitialized) {
            return;
        }
        mIsInitialized = true;

        mFleetListAdapter = new FleetListAdapter();
        final ListView fleetList = (ListView) findViewById(R.id.ship_list);
        fleetList.setAdapter(mFleetListAdapter);

        // make sure we're aware of any changes to the designs
        ShipDesignManager.getInstance().addDesignsChangedListener(new DesignManager.DesignsChangedListener() {
            @Override
            public void onDesignsChanged() {
                if (mStar != null && mFleetListAdapter != null) {
                    mFleetListAdapter.setFleets(mStar.getFleets());
                }
            }
        });

        mActivity.addUpdatedListener(new UniverseElementActivity.OnUpdatedListener() {
            @Override
            public void onStarUpdated(Star star, Planet selectedPlanet, Colony colony) {
                refresh(mActivity, star, star.getFleets());
            }
            @Override
            public void onSectorUpdated() {
            }
        });

        fleetList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                mSelectedFleet = mStar.getFleets().get(position);
                log.debug("Setting selected fleet: "+mSelectedFleet.getKey());
                mFleetListAdapter.notifyDataSetChanged();
            }
        });

        final Button splitBtn = (Button) findViewById(R.id.split_btn);
        splitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putString("au.com.codeka.warworlds.StarKey", mSelectedFleet.getStarKey());
                args.putString("au.com.codeka.warworlds.FleetKey", mSelectedFleet.getKey());
                mActivity.showDialog(FleetSplitDialog.ID, args);
            }
        });
    }


    /**
     * Populates a solarsystem_fleet_row.xml view with details from the given fleet.
     */
    public static void populateFleetRow(View view, Fleet fleet) {
        ImageView icon = (ImageView) view.findViewById(R.id.ship_icon);
        TextView row1 = (TextView) view.findViewById(R.id.ship_row1);
        TextView row2 = (TextView) view.findViewById(R.id.ship_row2);
        TextView row3 = (TextView) view.findViewById(R.id.ship_row3);

        ShipDesignManager dm = ShipDesignManager.getInstance();
        ShipDesign design = dm.getDesign(fleet.getDesignName());

        Bitmap bm = dm.getDesignIcon(design);
        if (bm != null) {
            icon.setImageBitmap(bm);
        } else {
            icon.setImageBitmap(null);
        }

        row1.setText(design.getName());
        row2.setText(String.format("%d", fleet.getNumShips()));
        row2.setGravity(Gravity.RIGHT);
        row3.setVisibility(View.GONE);
    }

    /**
     * This adapter is used to populate the list of ship fleets that the current colony has.
     */
    private class FleetListAdapter extends BaseAdapter {
        private List<Fleet> mFleets;

        public void setFleets(List<Fleet> fleets) {
            mFleets = fleets;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mFleets == null)
                return 0;
            return mFleets.size();
        }

        @Override
        public Object getItem(int position) {
            if (mFleets == null)
                return null;
            return mFleets.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.fleet_list_row, null);
            }

            Fleet fleet = mFleets.get(position);
            populateFleetRow(view, fleet);

            if (mSelectedFleet != null && mSelectedFleet.getKey().equals(fleet.getKey())) {
                view.setBackgroundColor(0xff0c6476);
            } else {
                view.setBackgroundColor(0xff000000);
            }

            return view;
        }
    }
}
