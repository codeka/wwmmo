package au.com.codeka.warworlds.game.solarsystem;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.ShipDesign;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.Star;

public class FleetDialog extends Dialog {
    private Logger log = LoggerFactory.getLogger(FleetDialog.class);
    private SolarSystemActivity mActivity;
    private Star mStar;
    private FleetListAdapter mFleetListAdapter;
    private Fleet mSelectedFleet;

    public FleetDialog(SolarSystemActivity activity) {
        super(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.solarsystem_fleet_dlg);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.MATCH_PARENT;
        params.width = LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

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

        mActivity.addStarUpdatedListener(new SolarSystemActivity.OnStarUpdatedListener() {
            @Override
            public void onStarUpdated(Star star, Planet selectedPlanet, Colony colony) {
                setStar(star);
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
                mActivity.showDialog(SolarSystemActivity.FLEET_SPLIT_DIALOG, args);
            }
        });
    }

    public void setStar(Star star) {
        mStar = star;

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

        if (mFleetListAdapter != null) {
            mFleetListAdapter.setFleets(mStar.getFleets());
        }
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
                view = inflater.inflate(R.layout.solarsystem_fleet_row, null);
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
