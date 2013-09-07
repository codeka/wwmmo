package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.TimeInHours;
import au.com.codeka.common.design.BuildingDesign;
import au.com.codeka.common.design.Design;
import au.com.codeka.common.design.DesignKind;
import au.com.codeka.common.model.BuildRequest;
import au.com.codeka.common.model.Building;
import au.com.codeka.common.model.Colony;
import au.com.codeka.common.model.Model;
import au.com.codeka.common.model.Star;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.StarManager;

public class BuildingsList extends ListView
                           implements StarManager.StarFetchedHandler {
    private Context mContext;
    private Star mStar;
    private Colony mColony;
    private BuildingListAdapter mAdapter;
    private boolean mIsAttachedToWindow;

    public BuildingsList(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void setColony(Star star, Colony colony) {
        mStar = star;
        mColony = colony;

        if (mAdapter == null) {
            mAdapter = new BuildingListAdapter();
            setAdapter(mAdapter);
        }
        mAdapter.setColony(mStar, mColony);

        if (!mIsAttachedToWindow) {
            StarManager.i.addStarUpdatedListener(mStar.key, this);
            mIsAttachedToWindow = true;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mIsAttachedToWindow && mStar != null) {
            StarManager.i.addStarUpdatedListener(mStar.key, this);
            mIsAttachedToWindow = true;
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        StarManager.i.removeStarUpdatedListener(this);
        mIsAttachedToWindow = false;
    }

    public Entry getItem(int index) {
        return (Entry) mAdapter.getItem(index);
    }

    @Override
    public void onStarFetched(Star s) {
        if (mColony == null) {
            return;
        }

        for (Colony baseColony : s.colonies) {
            if (baseColony.key.equals(mColony.key)) {
                setColony(s, (Colony) baseColony);
            }
        }
    }

    /**
     * This adapter is used to populate a list of buildings in a list view.
     */
    private class BuildingListAdapter extends BaseAdapter {
        private ArrayList<Entry> mEntries;

        private static final int HEADING_TYPE = 0;
        private static final int EXISTING_BUILDING_TYPE = 1;
        private static final int NEW_BUILDING_TYPE = 2;

        public void setColony(Star star, Colony colony) {
            List<Building> buildings = new ArrayList<Building>();
            for (Building b : star.buildings) {
                if (b.colony_key.equals(colony.key)){
                    buildings.add(b);
                }
            }

            mEntries = new ArrayList<Entry>();
            for (Building b : buildings) {
                Entry entry = new Entry();
                entry.building = (Building) b;
                if (star.build_requests != null) {
                    // if the building is being upgraded (i.e. if there's a build request that
                    // references this building) then add the build request as well
                    for (BuildRequest br : star.build_requests) {
                        if (br.existing_building_key != null && br.existing_building_key.equals(b.key)) {
                            entry.buildRequest = (BuildRequest) br;
                        }
                    }
                }
                mEntries.add(entry);
            }

            for (BuildRequest br : star.build_requests) {
                if (br.colony_key.equals(colony.key) &&
                        br.build_kind.equals(BuildRequest.BUILD_KIND.BUILDING) &&
                        br.existing_building_key == null) {
                    Entry entry = new Entry();
                    entry.buildRequest = (BuildRequest) br;
                    mEntries.add(entry);
                }
            }

            Collections.sort(mEntries, new Comparator<Entry>() {
                @Override
                public int compare(Entry lhs, Entry rhs) {
                    String a = (lhs.building != null ? lhs.building.design_id : lhs.buildRequest.design_id);
                    String b = (rhs.building != null ? rhs.building.design_id : rhs.buildRequest.design_id);
                    return a.compareTo(b);
                }
            });

            Entry title = new Entry();
            title.title = "Existing Buildings";
            mEntries.add(0, title);

            title = new Entry();
            title.title = "Available Buildings";
            mEntries.add(title);

            for (Design d : DesignManager.i.getDesigns(DesignKind.BUILDING).values()) {
                BuildingDesign bd = (BuildingDesign) d;
                if (bd.getMaxPerColony() > 0) {
                    int numExisting = 0;
                    for (Entry e : mEntries) {
                        if (e.building != null) {
                            if (e.building.design_id.equals(bd.getID())) {
                                numExisting ++;
                            }
                        } else if (e.buildRequest != null) {
                            if (e.buildRequest.design_id.equals(bd.getID())) {
                                numExisting ++;
                            }
                        }
                    }
                    if (numExisting >= bd.getMaxPerColony()) {
                        continue;
                    }
                }
                if (bd.getMaxPerEmpire() > 0) {
                    int numExisting = BuildManager.getInstance().getTotalBuildingsInEmpire(bd.getID());
                    for (BuildRequest br : BuildManager.getInstance().getBuildRequests()) {
                        if (br.design_id.equals(bd.getID())) {
                            numExisting ++;
                        }
                    }
                    if (numExisting >= bd.getMaxPerEmpire()) {
                        continue;
                    }
                }
                Entry entry = new Entry();
                entry.design = bd;
                mEntries.add(entry);
            }

            notifyDataSetChanged();
        }

        /**
         * We have three types of items, the "headings", the list of existing buildings
         * and the list of building designs.
         */
        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            if (mEntries == null)
                return 0;

            if (mEntries.get(position).title != null)
                return HEADING_TYPE;
            if (mEntries.get(position).design != null)
                return NEW_BUILDING_TYPE;
            return EXISTING_BUILDING_TYPE;
        }

        @Override
        public boolean isEnabled(int position) {
            if (getItemViewType(position) == HEADING_TYPE) {
                return false;
            }

            // also, if it's an existing building that's at the max level it can't be
            // upgraded any more, so also disabled.
            Entry entry = mEntries.get(position);
            if (entry.building != null) {
                int maxUpgrades = DesignManager.i.getDesign(entry.building).getUpgrades().size();
                if (entry.building.level > maxUpgrades) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public int getCount() {
            if (mEntries == null)
                return 0;

            return mEntries.size();
        }

        @Override
        public Object getItem(int position) {
            if (mEntries == null)
                return null;

            return mEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);

                int viewType = getItemViewType(position);
                if (viewType == HEADING_TYPE) {
                    view = new TextView(mContext);
                } else {
                    view = inflater.inflate(R.layout.solarsystem_buildings_design, parent, false);
                }
            }

            Entry entry = mEntries.get(position);
            if (entry.title != null) {
                TextView tv = (TextView) view;
                tv.setText(entry.title);
            } else if (entry.building != null || entry.buildRequest != null) {
                // existing building/upgrading building
                ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                TextView row1 = (TextView) view.findViewById(R.id.building_row1);
                TextView row2 = (TextView) view.findViewById(R.id.building_row2);
                TextView row3 = (TextView) view.findViewById(R.id.building_row3);
                TextView level = (TextView) view.findViewById(R.id.building_level);
                TextView levelLabel = (TextView) view.findViewById(R.id.building_level_label);
                ProgressBar progress = (ProgressBar) view.findViewById(R.id.building_progress);

                Building building = entry.building;
                BuildRequest buildRequest = entry.buildRequest;
                BuildingDesign design = (BuildingDesign) DesignManager.i.getDesign(DesignKind.BUILDING,
                        (building != null ? building.design_id : buildRequest.design_id));

                icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

                int numUpgrades = design.getUpgrades().size();

                if (numUpgrades == 0 || building == null) {
                    level.setVisibility(View.GONE);
                    levelLabel.setVisibility(View.GONE);
                } else {
                    level.setText(Integer.toString(building.level));
                    level.setVisibility(View.VISIBLE);
                    levelLabel.setVisibility(View.VISIBLE);
                }

                row1.setText(design.getDisplayName());
                if (buildRequest != null) {
                    String verb = (building == null ? "Building" : "Upgrading");
                    row2.setText(Html.fromHtml(String.format(Locale.ENGLISH,
                            "<font color=\"#0c6476\">%s:</font> %d %%, %s left",
                            verb, (int) Model.getPercentComplete(buildRequest),
                             TimeInHours.format(Model.getRemainingTime(buildRequest)))));

                    row3.setVisibility(View.GONE);
                    progress.setVisibility(View.VISIBLE);
                    progress.setProgress((int) Model.getPercentComplete(buildRequest));
                } else {
                    if (numUpgrades < building.level) {
                        row2.setText("No more upgrades");
                        row3.setVisibility(View.GONE);
                        progress.setVisibility(View.GONE);
                    } else {
                        progress.setVisibility(View.GONE);
                        row2.setText(String.format(Locale.ENGLISH,
                                "Upgrade: %.2f hours",
                                (float) design.getBuildCost().getTimeInSeconds() / 3600.0f));

                        String required = design.getDependenciesList(mStar, mColony, building.level);
                        row3.setVisibility(View.VISIBLE);
                        row3.setText(Html.fromHtml(required));
                    }
                }
            } else {
                // new building
                ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                TextView row1 = (TextView) view.findViewById(R.id.building_row1);
                TextView row2 = (TextView) view.findViewById(R.id.building_row2);
                TextView row3 = (TextView) view.findViewById(R.id.building_row3);

                view.findViewById(R.id.building_progress).setVisibility(View.GONE);
                view.findViewById(R.id.building_level).setVisibility(View.GONE);
                view.findViewById(R.id.building_level_label).setVisibility(View.GONE);

                BuildingDesign design = mEntries.get(position).design;

                icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

                row1.setText(design.getDisplayName());
                row2.setText(String.format("%.2f hours",
                        (float) design.getBuildCost().getTimeInSeconds() / 3600.0f));

                String required = design.getDependenciesList(mStar, mColony);
                row3.setText(required);
            }

            return view;
        }
    }

    public static class Entry {
        public String title;
        public BuildRequest buildRequest;
        public Building building;
        public BuildingDesign design;
    }
}
