package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import au.com.codeka.TimeInHours;
import au.com.codeka.common.design.DesignKind;
import au.com.codeka.common.design.ShipDesign;
import au.com.codeka.common.model.Empire;
import au.com.codeka.common.model.Fleet;
import au.com.codeka.common.model.Model;
import au.com.codeka.common.model.Star;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.ImageManager;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarType;

/**
 * This control displays a list of fleets along with controls you can use to manage them (split
 * them, move them around, etc).
 */
public class FleetList extends FrameLayout implements StarManager.StarFetchedHandler {
    private FleetListAdapter mFleetListAdapter;
    private Fleet mSelectedFleet;
    private List<Fleet> mFleets;
    private Map<String, Star> mStars;
    private Context mContext;
    private boolean mIsInitialized;
    private OnFleetActionListener mFleetActionListener;

    public FleetList(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        View child = inflate(context, R.layout.fleet_list_ctrl, null);
        this.addView(child);
    }

    public FleetList(Context context) {
        super(context);
        mContext = context;

        View child = inflate(context, R.layout.fleet_list_ctrl, null);
        this.addView(child);
    }

    public void setOnFleetActionListener(OnFleetActionListener listener) {
        mFleetActionListener = listener;
    }

    public void refresh(List<Fleet> fleets, Map<String, Star> stars) {
        if (fleets != null) {
            mFleets = new ArrayList<Fleet>();
            for (Fleet f : fleets) {
                mFleets.add((Fleet) f);
            }
        }
        mStars = stars;

        initialize();

        // if we had a fleet selected, make sure we still have the same
        // fleet selected after we refresh
        if (mSelectedFleet != null) {
            Fleet selectedFleet = mSelectedFleet;
            mSelectedFleet = null;

            for (Fleet f : mFleets) {
                if (f.key.equals(selectedFleet.key)) {
                    mSelectedFleet = f;
                    break;
                }
            }
        }

        mFleetListAdapter.setFleets(stars, mFleets);
    }

    public void selectFleet(String fleetKey, boolean recentre) {
        mSelectedFleet = null;
        for (Fleet f : mFleets) {
            if (f.key.equals(fleetKey)) {
                mSelectedFleet = f;
            }
        }

        if (mSelectedFleet != null) {
            final Spinner stanceSpinner = (Spinner) findViewById(R.id.stance);
            stanceSpinner.setSelection(mSelectedFleet.stance.ordinal());

            if (recentre) {
                int position = mFleetListAdapter.getItemPosition(mSelectedFleet);
                if (position >= 0) {
                    final ListView fleetList = (ListView) findViewById(R.id.ship_list);
                    fleetList.setSelection(position);
                }
            }
        }

        mFleetListAdapter.notifyDataSetChanged();
    }

    private void initialize() {
        if (mIsInitialized) {
            return;
        }
        mIsInitialized = true;

        mFleetListAdapter = new FleetListAdapter();
        final ListView fleetList = (ListView) findViewById(R.id.ship_list);
        fleetList.setAdapter(mFleetListAdapter);

        final Spinner stanceSpinner = (Spinner) findViewById(R.id.stance);
        stanceSpinner.setAdapter(new StanceAdapter());

        stanceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Fleet.FLEET_STANCE stance = Fleet.FLEET_STANCE.values()[position];
                if (mSelectedFleet == null) {
                    return;
                }

                if (mSelectedFleet.stance != stance && mFleetActionListener != null) {
                    mFleetActionListener.onFleetStanceModified(
                            mStars.get(mSelectedFleet.star_key),
                            mSelectedFleet, stance);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        fleetList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                FleetListAdapter.ItemEntry entry =
                        (FleetListAdapter.ItemEntry) mFleetListAdapter.getItem(position);
                if (entry.type == FleetListAdapter.FLEET_ITEM_TYPE) {
                    selectFleet(((Fleet) entry.value).key, false);
                }
            }
        });

        final Button splitBtn = (Button) findViewById(R.id.split_btn);
        splitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedFleet == null) {
                    return;
                }

                if (mFleetActionListener != null) {
                    mFleetActionListener.onFleetSplit(mStars.get(mSelectedFleet.star_key),
                                                      mSelectedFleet);
                }
            }
        });

        final Button moveBtn = (Button) findViewById(R.id.move_btn);
        moveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedFleet == null) {
                    return;
                }

                if (mFleetActionListener != null) {
                    mFleetActionListener.onFleetMove(mStars.get(mSelectedFleet.star_key),
                                                     mSelectedFleet);
                }
            }
        });

        final Button viewBtn = (Button) findViewById(R.id.view_btn);
        viewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedFleet == null) {
                    return;
                }

                if (mFleetActionListener != null) {
                    mFleetActionListener.onFleetView(mStars.get(mSelectedFleet.star_key),
                                                     mSelectedFleet);
                }
            }
        });

        final Button mergeBtn = (Button) findViewById(R.id.merge_btn);
        mergeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedFleet == null) {
                    return;
                }

                if (mFleetActionListener != null) {
                    mFleetActionListener.onFleetMerge(mSelectedFleet,
                                                      mFleets);
                }
            }
        });

        StarManager.i.addStarUpdatedListener(null, this);
    }

    @Override
    public void onDetachedFromWindow() {
        StarManager.i.removeStarUpdatedListener(this);
    }

    /**
     * When a star is updated, we may need to refresh the list.
     */
    @Override
    public void onStarFetched(Star s) {
        for (String starKey : mStars.keySet()) {
            if (starKey.equals(s.key)) {
                mStars.put(s.key, s);

                Iterator<Fleet> it = mFleets.iterator();
                while (it.hasNext()) {
                    Fleet f = it.next();
                    if (f.star_key.equals(starKey)) {
                        it.remove();
                    }
                }

                for (int j = 0; j < s.fleets.size(); j++) {
                    mFleets.add((Fleet) s.fleets.get(j));
                }

                refresh(null, mStars);
                break;
            }
        }
    }

    /**
     * Populates a solarsystem_fleet_row.xml view with details from the given fleet.
     */
    public static void populateFleetRow(final Context context, final Map<String, Star> stars, 
                                        View view, final Fleet fleet) {
        ImageView icon = (ImageView) view.findViewById(R.id.ship_icon);
        final TextView row1 = (TextView) view.findViewById(R.id.ship_row1);
        final TextView row2 = (TextView) view.findViewById(R.id.ship_row2);
        final TextView row3 = (TextView) view.findViewById(R.id.ship_row3);

        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.design_id);
        icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

        String text = String.format(Locale.ENGLISH, "%d × %s",
                    (int) Math.ceil(fleet.num_ships), design.getDisplayName(fleet.num_ships > 1));
        row1.setText(text);

        text = String.format("%s (stance: %s)",
                             StringUtils.capitalize(fleet.state.toString().toLowerCase(Locale.ENGLISH)),
                             StringUtils.capitalize(fleet.stance.toString().toLowerCase(Locale.ENGLISH)));
        row2.setText(text);

        if (fleet.state == Fleet.FLEET_STATE.MOVING) {
            row3.setVisibility(View.GONE);
            StarManager.i.requestStarSummary(fleet.destination_star_key,
                    new StarManager.StarSummaryFetchedHandler() {
                        @Override
                        public void onStarSummaryFetched(Star destStar) {
                            Star srcStar = null;
                            if (stars != null) {
                                srcStar = stars.get(fleet.star_key);
                            }
                            if (srcStar == null) {
                                srcStar = SectorManager.i.findStar(fleet.star_key);
                            }
                            if (srcStar == null) {
                                row3.setText("→ (unknown)");
                            } else {
                                float timeRemainingInHours = Model.getTimeToDestination(fleet, srcStar, destStar);

                                String eta = TimeInHours.format(timeRemainingInHours);
                                String html = String.format("→ <img src=\"star\" width=\"16\" height=\"16\" /> %s <b>ETA:</b> %s",
                                                            destStar.name, eta);
                                row3.setText(Html.fromHtml(html, 
                                                           new FleetListImageGetter(destStar),
                                                           null));
                            }
                            row3.setVisibility(View.VISIBLE);
                        }
                    });
        } else {
            row3.setText("");
            row3.setVisibility(View.GONE);

            final Empire myEmpire = EmpireManager.i.getEmpire();
            EmpireManager.i.fetchEmpire(fleet.empire_key,
                    new EmpireManager.EmpireFetchedHandler() {
                @Override
                public void onEmpireFetched(Empire empire) {
                    row3.setVisibility(View.VISIBLE);

                    if (myEmpire.key.equals(empire.key)) {
                        row3.setText(empire.display_name);
                    } else {
                        row3.setText(Html.fromHtml("<font color=\"red\">"+empire.display_name+"</font>"));
                    }
                }
            });
        }
    }

    /**
     * Fetches the inline images we use to display star icons and whatnot.
     */
    private static class FleetListImageGetter implements Html.ImageGetter {
        private Star mStarSummary;

        public FleetListImageGetter(Star starSummary) {
            mStarSummary = starSummary;
        }

        @Override
        public Drawable getDrawable(String source) {
            if (mStarSummary != null) {
                Sprite sprite = StarImageManager.getInstance().getSprite(mStarSummary, -1, true);
                Drawable d = new SpriteDrawable(sprite);
                d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                return d;
            } else {
                return null;
            }
        }
    }

    /**
     * This adapter is used to populate the list of ship fleets that the current colony has.
     */
    private class FleetListAdapter extends BaseAdapter {
        private ArrayList<Fleet> mFleets;
        private Map<String, Star> mStars;
        private ArrayList<ItemEntry> mEntries;
        private Empire mMyEmpire;

        private static final int STAR_ITEM_TYPE = 0;
        private static final int FLEET_ITEM_TYPE = 1;

        public FleetListAdapter() {
            mMyEmpire = EmpireManager.i.getEmpire();

            // whenever a new star bitmap is generated, redraw the screen
            StarImageManager.getInstance().addSpriteGeneratedListener(
                    new ImageManager.SpriteGeneratedListener() {
                @Override
                public void onSpriteGenerated(String key, Sprite sprite) {
                    notifyDataSetChanged();
                }
            });
        }

        /**
         * Sets the list of fleets that we'll be displaying.
         */
        public void setFleets(Map<String, Star> stars, List<Fleet> fleets) {
            mFleets = new ArrayList<Fleet>(fleets);
            mStars = stars;

            Collections.sort(mFleets, new Comparator<Fleet>() {
                @Override
                public int compare(Fleet lhs, Fleet rhs) {
                    // sort by star, then by design, then by count
                    if (!lhs.star_key.equals(rhs.star_key)) {
                        Star lhsStar = mStars.get(lhs.star_key);
                        Star rhsStar = mStars.get(rhs.star_key);
                        if (!lhsStar.name.equals(rhsStar.name)) {
                            return lhsStar.name.compareTo(rhsStar.name);
                        } else {
                            return lhsStar.key.compareTo(rhsStar.key);
                        }
                    } else if (!lhs.design_id.equals(rhs.design_id)) {
                        return lhs.design_id.compareTo(rhs.design_id);
                    } else {
                        return (int) (rhs.num_ships - lhs.num_ships);
                    }
                }
            });

            mEntries = new ArrayList<ItemEntry>();
            String lastStarKey = "";
            for (Fleet f : mFleets) {
                if (!f.star_key.equals(lastStarKey)) {
                    mEntries.add(new ItemEntry(STAR_ITEM_TYPE, mStars.get(f.star_key)));
                    lastStarKey = f.star_key;
                }
                mEntries.add(new ItemEntry(FLEET_ITEM_TYPE, f));
            }

            notifyDataSetChanged();
        }

        /**
         * We have two types of items, the star and the actual fleet.
         */
        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (mEntries == null)
                return 0;

            return mEntries.get(position).type;
        }

        @Override
        public boolean isEnabled(int position) {
            if (mEntries.get(position).type == STAR_ITEM_TYPE) {
                return false;
            }

            // if we don't own this fleet, we also can't do anything with it.
            Fleet fleet = (Fleet) mEntries.get(position).value;
            if (!fleet.empire_key.equals(mMyEmpire.key)) {
                return false;
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

        public int getItemPosition(Fleet fleet) {
            int index = 0;
            for (; index < mEntries.size(); index++) {
                ItemEntry entry = mEntries.get(index);
                if (entry.type == FLEET_ITEM_TYPE) {
                    Fleet entryFleet = (Fleet) entry.value;
                    if (entryFleet.key.equals(fleet.key)) {
                        return index;
                    }
                }
            }

            return -1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ItemEntry entry = mEntries.get(position);
            View view = convertView;

            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                if (entry.type == STAR_ITEM_TYPE) {
                    view = inflater.inflate(R.layout.fleet_list_star_row, null);
                } else {
                    view = inflater.inflate(R.layout.fleet_list_row, null);
                }
            }

            if (entry.type == STAR_ITEM_TYPE) {
                Star star = (Star) entry.value;
                ImageView icon = (ImageView) view.findViewById(R.id.star_icon);
                TextView name = (TextView) view.findViewById(R.id.star_name);

                int imageSize = (int)(star.size * StarType.get(star).getImageScale() * 2);
                if (entry.drawable == null) {
                    Sprite sprite = StarImageManager.getInstance().getSprite(star, imageSize, true);
                    entry.drawable = new SpriteDrawable(sprite);
                }
                if (entry.drawable != null) {
                    icon.setImageDrawable(entry.drawable);
                }

                name.setText(star.name);
            } else {
                Fleet fleet = (Fleet) entry.value;
                populateFleetRow(mContext, mStars, view, fleet);

                if (mSelectedFleet != null && mSelectedFleet.key.equals(fleet.key)) {
                    view.setBackgroundColor(0xff0c6476);
                } else {
                    view.setBackgroundColor(0xff000000);
                }
            }

            return view;
        }

        public class ItemEntry {
            public int type;
            public Object value;
            public Drawable drawable;

            public ItemEntry(int type, Object value) {
                this.type = type;
                this.value = value;
                this.drawable = null;
            }
        }
    }

    public class StanceAdapter extends BaseAdapter implements SpinnerAdapter {
        Fleet.FLEET_STANCE[] mValues;

        public StanceAdapter() {
            mValues = Fleet.FLEET_STANCE.values();
        }

        @Override
        public int getCount() {
            return mValues.length;
        }

        @Override
        public Object getItem(int position) {
            return mValues[position];
        }

        @Override
        public long getItemId(int position) {
            return mValues[position].ordinal();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = getCommonView(position, convertView, parent);

            view.setTextColor(Color.WHITE);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView view = getCommonView(position, convertView, parent);

            ViewGroup.LayoutParams lp = new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT,
                                                                     LayoutParams.MATCH_PARENT);
            lp.height = 80;
            view.setLayoutParams(lp);
            view.setTextColor(Color.WHITE);
            view.setText("  "+view.getText().toString());
            return view;
        }

        private TextView getCommonView(int position, View convertView, ViewGroup parent) {
            TextView view;
            if (convertView != null) {
                view = (TextView) convertView;
            } else {
                view = new TextView(mContext);
                view.setGravity(Gravity.CENTER_VERTICAL);
            }

            Fleet.FLEET_STANCE value = mValues[position];
            view.setText(StringUtils.capitalize(value.toString().toLowerCase(Locale.ENGLISH)));
            return view;
        }
    }

    public interface OnFleetActionListener {
        void onFleetView(Star star, Fleet fleet);
        void onFleetSplit(Star star, Fleet fleet);
        void onFleetMove(Star star, Fleet fleet);
        void onFleetStanceModified(Star star, Fleet fleet, Fleet.FLEET_STANCE newStance);
        void onFleetMerge(Fleet fleet, List<Fleet> potentialFleets);
    }
}
