package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.NotesDialog;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.FleetManager;
import au.com.codeka.warworlds.model.ImageManager;
import au.com.codeka.warworlds.model.ShieldManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;

/**
 * This control displays a list of fleets along with controls you can use to manage them (split
 * them, move them around, etc).
 */
public class FleetList extends FrameLayout {
    private FleetListAdapter mFleetListAdapter;
    protected Fleet mSelectedFleet;
    private List<Fleet> mFleets;
    private Map<String, Star> mStars;
    private Context mContext;
    private boolean mIsInitialized;
    private FleetSelectionPanel mFleetSelectionPanel;
    private OnFleetActionListener mFleetActionListener;

    public FleetList(Context context, AttributeSet attrs) {
        this(context, attrs, R.layout.fleet_list_ctrl);
    }

    public FleetList(Context context) {
        this(context, null, R.layout.fleet_list_ctrl);
    }

    protected FleetList(Context context, AttributeSet attrs, int layoutID) {
        super(context, attrs);
        mContext = context;

        View child = inflate(context, layoutID, null);
        this.addView(child);
    }

    public void setOnFleetActionListener(OnFleetActionListener listener) {
        mFleetActionListener = listener;
        if (mFleetSelectionPanel != null) {
            mFleetSelectionPanel.setOnFleetActionListener(listener);
        }
    }

    public void refresh(List<BaseFleet> fleets, Map<String, Star> stars) {
        if (fleets != null) {
            mFleets = new ArrayList<Fleet>();
            for (BaseFleet f : fleets) {
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
                if (f.getKey().equals(selectedFleet.getKey())) {
                    mSelectedFleet = f;
                    break;
                }
            }
        }
        if (mSelectedFleet != null) {
            selectFleet(mSelectedFleet.getKey(), false);
        } else {
            selectFleet(null, false);
        }

        mFleetListAdapter.setFleets(stars, mFleets);
    }

    public void selectFleet(String fleetKey, boolean recentre) {
        mSelectedFleet = null;
        for (Fleet f : mFleets) {
            if (fleetKey != null && f.getKey().equals(fleetKey)) {
                mSelectedFleet = f;
            }
        }

        if (mSelectedFleet != null && recentre) {
            int position = mFleetListAdapter.getItemPosition(mSelectedFleet);
            if (position >= 0) {
                final ListView fleetList = (ListView) findViewById(R.id.ship_list);
                fleetList.setSelection(position);
            }
        }

        if (mSelectedFleet != null) {
            mFleetSelectionPanel.setSelectedFleet(mStars.get(mSelectedFleet.getStarKey()),
                    mSelectedFleet);
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

        mFleetSelectionPanel = (FleetSelectionPanel) findViewById(R.id.bottom_pane);
        mFleetSelectionPanel.setOnFleetActionListener(mFleetActionListener);

        fleetList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FleetListAdapter.ItemEntry entry = (FleetListAdapter.ItemEntry) mFleetListAdapter.getItem(position);
                if (entry.type == FleetListAdapter.FLEET_ITEM_TYPE) {
                    selectFleet(((Fleet) entry.value).getKey(), false);
                }
            }
        });

        fleetList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                FleetListAdapter.ItemEntry entry = (FleetListAdapter.ItemEntry) mFleetListAdapter.getItem(position);
                if (entry.type != FleetListAdapter.FLEET_ITEM_TYPE) {
                    return false;
                }

                final Fleet fleet = (Fleet) entry.value;
                NotesDialog dialog = new NotesDialog();
                dialog.setup(fleet.getNotes(), new NotesDialog.NotesChangedHandler() {
                    @Override
                    public void onNotesChanged(String notes) {
                        fleet.setNotes(notes);
                        mFleetListAdapter.notifyDataSetChanged();

                        FleetManager.i.updateNotes(fleet);
                    }
                });

                dialog.show(((BaseActivity) mContext).getSupportFragmentManager(), "");
                return true;
            }
        });

        onInitialize();

        StarManager.eventBus.register(mEventHandler);
        ImageManager.eventBus.register(mEventHandler);
        ShieldManager.eventBus.register(mEventHandler);
    }

    protected void onInitialize() {
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ShieldManager.eventBus.unregister(mEventHandler);
        ImageManager.eventBus.unregister(mEventHandler);
        StarManager.eventBus.unregister(mEventHandler);
    }

    private Object mEventHandler = new Object() {
        @EventHandler
        public void onStarUpdated(Star s) {
            for (String starKey : mStars.keySet()) {
                if (starKey.equals(s.getKey())) {
                    mStars.put(s.getKey(), s);

                    Iterator<Fleet> it = mFleets.iterator();
                    while (it.hasNext()) {
                        Fleet f = it.next();
                        if (f.getStarKey().equals(starKey)) {
                            it.remove();
                        }
                    }

                    for (int j = 0; j < s.getFleets().size(); j++) {
                        mFleets.add((Fleet) s.getFleets().get(j));
                    }

                    refresh(null, mStars);
                    break;
                }
            }
        }

        @EventHandler
        public void onEmpireUpdated(Empire empire) {
            mFleetListAdapter.notifyDataSetChanged();
        }

        @EventHandler
        public void onSpriteGenerated(ImageManager.SpriteGeneratedEvent event) {
            mFleetListAdapter.notifyDataSetChanged();
        }

        @EventHandler
        public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
            mFleetListAdapter.notifyDataSetChanged();
        }
    };

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
                    if (!lhs.getStarKey().equals(rhs.getStarKey())) {
                        Star lhsStar = mStars.get(lhs.getStarKey());
                        Star rhsStar = mStars.get(rhs.getStarKey());
                        if (!lhsStar.getName().equals(rhsStar.getName())) {
                            return lhsStar.getName().compareTo(rhsStar.getName());
                        } else {
                            return lhsStar.getKey().compareTo(rhsStar.getKey());
                        }
                    } else if (!lhs.getDesignID().equals(rhs.getDesignID())) {
                        return lhs.getDesignID().compareTo(rhs.getDesignID());
                    } else {
                        return (int) (rhs.getNumShips() - lhs.getNumShips());
                    }
                }
            });

            mEntries = new ArrayList<ItemEntry>();
            String lastStarKey = "";
            for (Fleet f : mFleets) {
                if (!f.getStarKey().equals(lastStarKey)) {
                    mEntries.add(new ItemEntry(STAR_ITEM_TYPE, mStars.get(f.getStarKey())));
                    lastStarKey = f.getStarKey();
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
            if (fleet.getEmpireKey() == null) {
                return false;
            }
            if (!fleet.getEmpireKey().equals(mMyEmpire.getKey())) {
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
                    if (entryFleet.getKey().equals(fleet.getKey())) {
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
                    view = inflater.inflate(R.layout.fleet_list_star_row, parent, false);
                } else {
                    view = new FleetListRow(mContext);
                }
            }

            if (entry.type == STAR_ITEM_TYPE) {
                Star star = (Star) entry.value;
                ImageView icon = (ImageView) view.findViewById(R.id.star_icon);
                TextView name = (TextView) view.findViewById(R.id.star_name);

                int imageSize = (int)(star.getSize() * star.getStarType().getImageScale() * 2);
                if (entry.drawable == null) {
                    Sprite sprite = StarImageManager.getInstance().getSprite(star, imageSize, true);
                    entry.drawable = new SpriteDrawable(sprite);
                }
                if (entry.drawable != null) {
                    icon.setImageDrawable(entry.drawable);
                }

                name.setText(star.getName());
            } else {
                Fleet fleet = (Fleet) entry.value;
                ((FleetListRow) view).setFleet(fleet);

                if (mSelectedFleet != null && mSelectedFleet.getKey().equals(fleet.getKey())) {
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

    public interface OnFleetActionListener {
        void onFleetView(Star star, Fleet fleet);
        void onFleetSplit(Star star, Fleet fleet);
        void onFleetMove(Star star, Fleet fleet);
        void onFleetBoost(Star star, Fleet fleet);
        void onFleetStanceModified(Star star, Fleet fleet, Fleet.Stance newStance);
        void onFleetMerge(Fleet fleet, List<Fleet> potentialFleets);
    }
}
