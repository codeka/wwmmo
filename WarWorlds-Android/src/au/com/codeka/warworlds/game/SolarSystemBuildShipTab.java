package au.com.codeka.warworlds.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Design;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.ShipDesign;
import au.com.codeka.warworlds.model.ShipDesignManager;

public class SolarSystemBuildShipTab implements SolarSystemBuildDialog.Tab {
    private SolarSystemActivity mActivity;
    private ShipDesignListAdapter mShipDesignListAdapter;
    private View mView;

    SolarSystemBuildShipTab(SolarSystemBuildDialog dialog, SolarSystemActivity activity) {
        mActivity = activity;
    }

    @Override
    public View getView() {
        if (mView == null)
            setup();
        return mView;
    }

    @Override
    public String getTitle() {
        return "Ships";
    }

    @Override
    public void setColony(Colony colony) {
    }

    private void setup() {
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(R.layout.solarsystem_build_ships_tab, null);

        mShipDesignListAdapter = new ShipDesignListAdapter();
        mShipDesignListAdapter.setDesigns(ShipDesignManager.getInstance().getDesigns());

        ListView availableDesignsList = (ListView) mView.findViewById(R.id.ship_list);
        availableDesignsList.setAdapter(mShipDesignListAdapter);
        availableDesignsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Bundle args = new Bundle();
                ShipDesign design = (ShipDesign) mShipDesignListAdapter.getItem(position);
                args.putString("au.com.codeka.warworlds.DesignID", design.getID());
                //mActivity.showDialog(SolarSystemActivity.BUILDINGS_CONFIRM_DIALOG, args);
            }
        });

        // make sure we're aware of any changes to the designs
        ShipDesignManager.getInstance().addDesignsChangedListener(new DesignManager.DesignsChangedListener() {
            @Override
            public void onDesignsChanged() {
                mShipDesignListAdapter.setDesigns(ShipDesignManager.getInstance().getDesigns());
            }
        });
    }

    /**
     * This adapter is used to populate the list of ship designs in our view.
     */
    private class ShipDesignListAdapter extends BaseAdapter {
        private List<ShipDesign> mDesigns;

        public void setDesigns(Map<String, Design> designs) {
            mDesigns = new ArrayList<ShipDesign>();
            for (Design d : designs.values()) {
                mDesigns.add((ShipDesign) d);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mDesigns == null)
                return 0;
            return mDesigns.size();
        }

        @Override
        public Object getItem(int position) {
            if (mDesigns == null)
                return null;
            return mDesigns.get(position);
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
                view = inflater.inflate(R.layout.solarsystem_buildings_design, null);
            }

            ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
            TextView row1 = (TextView) view.findViewById(R.id.building_row1);
            TextView row2 = (TextView) view.findViewById(R.id.building_row2);
            TextView row3 = (TextView) view.findViewById(R.id.building_row3);
            ProgressBar progress = (ProgressBar) view.findViewById(R.id.building_progress);
            progress.setVisibility(View.GONE);

            ShipDesign design = mDesigns.get(position);

            Bitmap bm = ShipDesignManager.getInstance().getDesignIcon(design);
            if (bm != null) {
                icon.setImageBitmap(bm);
            } else {
                icon.setImageBitmap(null);
            }

            row1.setText(design.getName());
            row2.setText(String.format("$ %d - %.2f hours", design.getBuildCost(),
                    (float) design.getBuildTimeSeconds() / 3600.0f));
            row3.setText("Required: none");

            return view;
        }
    }
}
