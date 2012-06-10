package au.com.codeka.warworlds.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import au.com.codeka.warworlds.model.Building;
import au.com.codeka.warworlds.model.BuildingDesign;
import au.com.codeka.warworlds.model.BuildingDesignManager;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Design;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Star;

/**
 * Handles the "Building" tab of the \c SolarSystemBuildDialog.
 */
public class SolarSystemBuildBuildingTab implements SolarSystemBuildDialog.Tab {
    private static Logger log = LoggerFactory.getLogger(SolarSystemBuildBuildingTab.class);
    private BuildingDesignListAdapter mDesignListAdapter;
    private BuildingListAdapter mBuildingListAdapter;
    private SolarSystemActivity mActivity;
    private Colony mColony;
    private View mView;

    SolarSystemBuildBuildingTab(SolarSystemBuildDialog dialog, SolarSystemActivity activity) {
        mActivity = activity;
    }

    public View getView() {
        if (mView == null)
            setup();
        return mView;
    }

    public String getTitle() {
        return "Buildings";
    }

    @Override
    public void setColony(Star star, Colony colony) {
        mColony = colony;

        if (mBuildingListAdapter != null && mColony != null) {
            mBuildingListAdapter.setBuildings(mColony.getBuildings());
        }
    }

    /**
     * Sets up the view and returns the \c View object that we want to use in this tab.
     */
    private void setup() {
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(R.layout.solarsystem_build_buildings_tab, null);

        mDesignListAdapter = new BuildingDesignListAdapter();
        mDesignListAdapter.setDesigns(BuildingDesignManager.getInstance().getDesigns());

        mBuildingListAdapter = new BuildingListAdapter();
        if (mColony != null) {
            mBuildingListAdapter.setBuildings(mColony.getBuildings());
        }

        ListView availableDesignsList = (ListView) mView.findViewById(R.id.buildings_available);
        availableDesignsList.setAdapter(mDesignListAdapter);
        availableDesignsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Bundle args = new Bundle();
                BuildingDesign design = (BuildingDesign) mDesignListAdapter.getItem(position);
                args.putString("au.com.codeka.warworlds.DesignID", design.getID());
                args.putInt("au.com.codeka.warworlds.DesignKind", design.getDesignKind().getValue());
                mActivity.showDialog(SolarSystemActivity.BUILD_CONFIRM_DIALOG, args);
            }
        });

        ListView existingBuildingsList = (ListView) mView.findViewById(R.id.buildings_existing);
        existingBuildingsList.setAdapter(mBuildingListAdapter);

        // make sure we're aware of any changes to the designs
        BuildingDesignManager.getInstance().addDesignsChangedListener(new DesignManager.DesignsChangedListener() {
            @Override
            public void onDesignsChanged() {
                mDesignListAdapter.setDesigns(BuildingDesignManager.getInstance().getDesigns());
                if (mColony != null) {
                    mBuildingListAdapter.setBuildings(mColony.getBuildings());
                }
            }
        });
    }

    /**
     * This adapter is used to populate a list of buildings in a list view.
     */
    private class BuildingListAdapter extends BaseAdapter {
        private List<Building> mBuildings;

        public void setBuildings(List<Building> buildings) {
            log.info("Setting buildings ("+buildings.size()+" buildings)");
            mBuildings = buildings;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mBuildings == null)
                return 0;
            return mBuildings.size();
        }

        @Override
        public Object getItem(int position) {
            if (mBuildings == null)
                return null;
            return mBuildings.get(position);
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

            Building building = mBuildings.get(position);
            BuildingDesign design = building.getDesign();

            Bitmap bm = BuildingDesignManager.getInstance().getDesignIcon(design);
            if (bm != null) {
                icon.setImageBitmap(bm);
            } else {
                icon.setImageBitmap(null);
            }

            row1.setText(design.getName());
            row2.setText("Level 1");

            row3.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
            row3.setText(String.format("Upgrade: $ %d, %.2f hours", design.getBuildCost(),
                    (float) design.getBuildTimeSeconds() / 3600.0f));

            return view;
        }
    }

    /**
     * This adapter is used to populate the list of building designs in one of the views.
     */
    private class BuildingDesignListAdapter extends BaseAdapter {
        private List<BuildingDesign> mDesigns;

        public void setDesigns(Map<String, Design> designs) {
            mDesigns = new ArrayList<BuildingDesign>();
            for (Design d : designs.values()) {
                mDesigns.add((BuildingDesign) d);
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

            BuildingDesign design = mDesigns.get(position);

            Bitmap bm = BuildingDesignManager.getInstance().getDesignIcon(design);
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
