package au.com.codeka.warworlds.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.BuildQueueManager;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Building;
import au.com.codeka.warworlds.model.BuildingDesign;
import au.com.codeka.warworlds.model.BuildingDesignManager;
import au.com.codeka.warworlds.model.Colony;

public class SolarSystemBuildingsDialog extends Dialog {
    private static Logger log = LoggerFactory.getLogger(SolarSystemBuildingsDialog.class);
    private SolarSystemActivity mActivity;
    private Colony mColony;
    private BuildingDesignListAdapter mDesignListAdapter;
    private BuildingListAdapter mBuildingListAdapter;
    private BuildQueueListAdapter mBuildQueueListAdapter;

    public SolarSystemBuildingsDialog(SolarSystemActivity activity) {
        super(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.solarsystem_buildings);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.MATCH_PARENT;
        params.width = LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        mDesignListAdapter = new BuildingDesignListAdapter();
        mDesignListAdapter.setDesigns(BuildingDesignManager.getInstance().getDesigns());

        mBuildingListAdapter = new BuildingListAdapter();
        if (mColony != null) {
            mBuildingListAdapter.setBuildings(mColony.getBuildings());
        }

        mBuildQueueListAdapter = new BuildQueueListAdapter();
        if (mColony != null) {
            mBuildQueueListAdapter.setBuildQueue(BuildQueueManager.getInstance().getBuildQueueForColony(mColony));
        }

        ListView availableDesignsList = (ListView) findViewById(R.id.buildings_available);
        availableDesignsList.setAdapter(mDesignListAdapter);
        availableDesignsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Bundle args = new Bundle();
                BuildingDesign design = (BuildingDesign) mDesignListAdapter.getItem(position);
                args.putString("au.com.codeka.warworlds.BuildingID", design.getID());
                mActivity.showDialog(SolarSystemActivity.BUILDINGS_CONFIRM_DIALOG, args);
            }
        });

        ListView existingBuildingsList = (ListView) findViewById(R.id.buildings_existing);
        existingBuildingsList.setAdapter(mBuildingListAdapter);

        ListView buildQueueList = (ListView) findViewById(R.id.buildings_inprogress);
        buildQueueList.setAdapter(mBuildQueueListAdapter);

        // make sure we're aware of any changes to the designs
        BuildingDesignManager.getInstance().addDesignsChangedListener(new BuildingDesignManager.DesignsChangedListener() {
            @Override
            public void onDesignsChanged() {
                mDesignListAdapter.setDesigns(BuildingDesignManager.getInstance().getDesigns());
                if (mColony != null) {
                    mBuildingListAdapter.setBuildings(mColony.getBuildings());
                    mBuildQueueListAdapter.setBuildQueue(BuildQueueManager.getInstance().getBuildQueueForColony(mColony));
                }
            }
        });

        // make sure we're aware of changes to the build queue
        BuildQueueManager.getInstance().addBuildQueueUpdatedListener(new BuildQueueManager.BuildQueueUpdatedListener() {
            @Override
            public void onBuildQueueUpdated(List<BuildRequest> queue) {
                if (mColony != null) {
                    mBuildQueueListAdapter.setBuildQueue(BuildQueueManager.getInstance().getBuildQueueForColony(mColony));
                }
            }
        });
    }

    public void setColony(Colony colony) {
        mColony = colony;
        if (mBuildingListAdapter != null && mColony != null) {
            mBuildingListAdapter.setBuildings(mColony.getBuildings());
        }
        if (mBuildQueueListAdapter != null && mColony != null) {
            mBuildQueueListAdapter.setBuildQueue(BuildQueueManager.getInstance().getBuildQueueForColony(mColony));
        }
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
                view = (ViewGroup) inflater.inflate(R.layout.solarsystem_buildings_design, null);
            }

            ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
            TextView row1 = (TextView) view.findViewById(R.id.building_row1);
            TextView row2 = (TextView) view.findViewById(R.id.building_row2);
            TextView row3 = (TextView) view.findViewById(R.id.building_row3);

            Building building = mBuildings.get(position);
            BuildingDesign design = building.getDesign();

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


    /**
     * This adapter is used to populate the list of buildings that are currently in progress.
     */
    private class BuildQueueListAdapter extends BaseAdapter {
        private List<BuildRequest> mQueue;

        public void setBuildQueue(List<BuildRequest> queue) {
            mQueue = queue;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mQueue == null)
                return 0;
            return mQueue.size();
        }

        @Override
        public Object getItem(int position) {
            if (mQueue == null)
                return null;
            return mQueue.get(position);
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
                view = (ViewGroup) inflater.inflate(R.layout.solarsystem_buildings_design, null);
            }

            ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
            TextView row1 = (TextView) view.findViewById(R.id.building_row1);
            TextView row2 = (TextView) view.findViewById(R.id.building_row2);
            TextView row3 = (TextView) view.findViewById(R.id.building_row3);

            BuildRequest request = mQueue.get(position);
            BuildingDesign design = request.getBuildingDesign();

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

    /**
     * This adapter is used to populate the list of building designs in one of the views.
     */
    private class BuildingDesignListAdapter extends BaseAdapter {
        private List<BuildingDesign> mDesigns;

        public void setDesigns(Map<String, BuildingDesign> designs) {
            mDesigns = new ArrayList<BuildingDesign>(designs.values());
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
                view = (ViewGroup) inflater.inflate(R.layout.solarsystem_buildings_design, null);
            }

            ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
            TextView row1 = (TextView) view.findViewById(R.id.building_row1);
            TextView row2 = (TextView) view.findViewById(R.id.building_row2);
            TextView row3 = (TextView) view.findViewById(R.id.building_row3);

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
