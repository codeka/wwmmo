package au.com.codeka.warworlds.game;

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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.ShipDesign;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.Star;

public class SolarSystemFleetDialog extends Dialog {
    private Logger log = LoggerFactory.getLogger(SolarSystemFleetDialog.class);
    private SolarSystemActivity mActivity;
    private Star mStar;
    private FleetListAdapter mFleetListAdapter;

    public SolarSystemFleetDialog(SolarSystemActivity activity) {
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
        ListView fleetList = (ListView) findViewById(R.id.ship_list);
        fleetList.setAdapter(mFleetListAdapter);
    }

    public void setStar(Star star) {
        mStar = star;

        if (mFleetListAdapter != null) {
            log.debug(String.format("Setting fleets: %d", mStar.getFleets().size()));
            mFleetListAdapter.setFleets(mStar.getFleets());
        }
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

            ImageView icon = (ImageView) view.findViewById(R.id.ship_icon);
            TextView row1 = (TextView) view.findViewById(R.id.ship_row1);
            TextView row2 = (TextView) view.findViewById(R.id.ship_row2);
            TextView row3 = (TextView) view.findViewById(R.id.ship_row3);

            Fleet fleet = mFleets.get(position);
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

            return view;
        }
    }
}
