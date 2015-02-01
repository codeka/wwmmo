package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Building;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/**
 * This is a list of existing buildings, in-progress buildings and new designs available to build.
 */
public class BuildingsList extends ListView {
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
            StarManager.eventBus.register(mEventHandler);
            mIsAttachedToWindow = true;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mIsAttachedToWindow && mStar != null) {
            StarManager.eventBus.register(mEventHandler);
            mIsAttachedToWindow = true;
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        StarManager.eventBus.unregister(mEventHandler);
        mIsAttachedToWindow = false;
    }

    public Entry getItem(int index) {
        return (Entry) mAdapter.getItem(index);
    }

    private Object mEventHandler = new Object() {
        @EventHandler
        public void onStarUpdated(Star star) {
            if (mStar != null && !mStar.getKey().equals(star.getKey())) {
                return;
            }
            if (mColony == null) {
                return;
            }

            for (BaseColony baseColony : star.getColonies()) {
                if (baseColony.getKey().equals(mColony.getKey())) {
                    setColony(star, (Colony) baseColony);
                }
            }

        }
    };

    /** This adapter is used to populate a list of buildings in a list view. */
    private class BuildingListAdapter extends BaseAdapter {
        private ArrayList<Entry> mEntries;

        private static final int HEADING_TYPE = 0;
        private static final int EXISTING_BUILDING_TYPE = 1;
        private static final int NEW_BUILDING_TYPE = 2;

        public void setColony(Star star, Colony colony) {
            mEntries = new ArrayList<>();

            List<BaseBuilding> buildings = colony.getBuildings();
            if (buildings == null) {
                buildings = new ArrayList<>();
            }

            ArrayList<Entry> existingBuildingEntries = new ArrayList<>();
            for (BaseBuilding b : buildings) {
                Entry entry = new Entry();
                entry.building = (Building) b;
                if (star.getBuildRequests() != null) {
                    // if the building is being upgraded (i.e. if there's a build request that
                    // references this building) then add the build request as well
                    for (BaseBuildRequest br : star.getBuildRequests()) {
                        if (br.getExistingBuildingKey() != null && br.getExistingBuildingKey().equals(b.getKey())) {
                            entry.buildRequest = (BuildRequest) br;
                        }
                    }
                }
                existingBuildingEntries.add(entry);
            }

            for (BaseBuildRequest br : star.getBuildRequests()) {
                if (br.getColonyKey().equals(colony.getKey()) &&
                    br.getDesignKind().equals(DesignKind.BUILDING) &&
                    br.getExistingBuildingKey() == null) {
                    Entry entry = new Entry();
                    entry.buildRequest = (BuildRequest) br;
                    existingBuildingEntries.add(entry);
                }
            }

            Collections.sort(existingBuildingEntries, new Comparator<Entry>() {
                @Override
                public int compare(Entry lhs, Entry rhs) {
                    String a = (lhs.building != null ? lhs.building.getDesignID() : lhs.buildRequest.getDesignID());
                    String b = (rhs.building != null ? rhs.building.getDesignID() : rhs.buildRequest.getDesignID());
                    return a.compareTo(b);
                }
            });

            Entry title = new Entry();
            title.title = "New Buildings";
            mEntries.add(title);

            for (Design d : DesignManager.i.getDesigns(DesignKind.BUILDING).values()) {
                BuildingDesign bd = (BuildingDesign) d;
                if (bd.getMaxPerColony() > 0) {
                    int numExisting = 0;
                    for (Entry e : existingBuildingEntries) {
                        if (e.building != null) {
                            if (e.building.getDesignID().equals(bd.getID())) {
                                numExisting ++;
                            }
                        } else if (e.buildRequest != null) {
                            if (e.buildRequest.getDesignID().equals(bd.getID())) {
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
                    // If you're building one, we'll still think it's OK to build again, but it's
                    // actually going to be blocked by the server.
                    if (numExisting >= bd.getMaxPerEmpire()) {
                        continue;
                    }
                }
                Entry entry = new Entry();
                entry.design = bd;
                mEntries.add(entry);
            }

            title = new Entry();
            title.title = "Existing Buildings";
            mEntries.add(title);

            for (Entry entry : existingBuildingEntries) {
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
                int maxUpgrades = entry.building.getDesign().getUpgrades().size();
                if (entry.building.getLevel() > maxUpgrades) {
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
                    view = inflater.inflate(R.layout.buildings_design, parent, false);
                }
            }

            Entry entry = mEntries.get(position);
            if (entry.title != null) {
                TextView tv = (TextView) view;
                tv.setTypeface(Typeface.DEFAULT_BOLD);
                tv.setText(entry.title);
            } else if (entry.building != null || entry.buildRequest != null) {
                // existing building/upgrading building
                ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                LinearLayout row1 = (LinearLayout) view.findViewById(R.id.building_row1);
                TextView row2 = (TextView) view.findViewById(R.id.building_row2);
                TextView row3 = (TextView) view.findViewById(R.id.building_row3);
                TextView level = (TextView) view.findViewById(R.id.building_level);
                TextView levelLabel = (TextView) view.findViewById(R.id.building_level_label);
                ProgressBar progress = (ProgressBar) view.findViewById(R.id.building_progress);
                TextView notes = (TextView) view.findViewById(R.id.notes);

                Building building = entry.building;
                BuildRequest buildRequest = entry.buildRequest;
                BuildingDesign design = (BuildingDesign) DesignManager.i.getDesign(DesignKind.BUILDING,
                        (building != null ? building.getDesignID() : buildRequest.getDesignID()));

                icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

                int numUpgrades = design.getUpgrades().size();

                if (numUpgrades == 0 || building == null) {
                    level.setVisibility(View.GONE);
                    levelLabel.setVisibility(View.GONE);
                } else {
                    level.setText(Integer.toString(building.getLevel()));
                    level.setVisibility(View.VISIBLE);
                    levelLabel.setVisibility(View.VISIBLE);
                }

                row1.removeAllViews();
                addTextToRow(mContext, row1, design.getDisplayName());
                if (buildRequest != null) {
                    String verb = (building == null ? "Building" : "Upgrading");
                    row2.setText(Html.fromHtml(String.format(Locale.ENGLISH,
                            "<font color=\"#0c6476\">%s:</font> %d %%, %s left",
                            verb, (int) buildRequest.getPercentComplete(),
                            TimeFormatter.create().format(buildRequest.getRemainingTime()))));

                    row3.setVisibility(View.GONE);
                    progress.setVisibility(View.VISIBLE);
                    progress.setProgress((int) buildRequest.getPercentComplete());
                } else if (building != null) {
                    if (numUpgrades < building.getLevel()) {
                        row2.setText("No more upgrades");
                        row3.setVisibility(View.GONE);
                        progress.setVisibility(View.GONE);
                    } else {
                        progress.setVisibility(View.GONE);

                        String requiredHtml = design.getDependenciesHtml(mColony, building.getLevel() + 1);
                        row2.setText(Html.fromHtml(requiredHtml));

                        row3.setVisibility(View.GONE);
                    }
                }

                if (building != null && building.getNotes() != null) {
                    notes.setText(building.getNotes());
                    notes.setVisibility(View.VISIBLE);
                } else if (buildRequest != null && buildRequest.getNotes() != null) {
                    notes.setText(buildRequest.getNotes());
                    notes.setVisibility(View.VISIBLE);
                } else {
                    notes.setText("");
                    notes.setVisibility(View.GONE);
                }
            } else {
                // new building
                ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                LinearLayout row1 = (LinearLayout) view.findViewById(R.id.building_row1);
                TextView row2 = (TextView) view.findViewById(R.id.building_row2);
                TextView row3 = (TextView) view.findViewById(R.id.building_row3);

                view.findViewById(R.id.building_progress).setVisibility(View.GONE);
                view.findViewById(R.id.building_level).setVisibility(View.GONE);
                view.findViewById(R.id.building_level_label).setVisibility(View.GONE);
                view.findViewById(R.id.notes).setVisibility(View.GONE);

                BuildingDesign design = mEntries.get(position).design;

                icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

                row1.removeAllViews();
                addTextToRow(mContext, row1, design.getDisplayName());
                String requiredHtml = design.getDependenciesHtml(mColony);
                row2.setText(Html.fromHtml(requiredHtml));

                row3.setVisibility(View.GONE);
            }

            return view;
        }

        private void addTextToRow(Context context, LinearLayout row, CharSequence text) {
            TextView tv = new TextView(context);
            tv.setText(text);
            tv.setSingleLine(true);
            tv.setEllipsize(TruncateAt.END);
            row.addView(tv);
        }
    }

    public static class Entry {
        public String title;
        public BuildRequest buildRequest;
        public Building building;
        public BuildingDesign design;
    }
}
