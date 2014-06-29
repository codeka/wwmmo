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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseFleet.State;
import au.com.codeka.common.model.BaseFleetUpgrade;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.NotesDialog;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.FleetManager;
import au.com.codeka.warworlds.model.FleetUpgrade.BoostFleetUpgrade;
import au.com.codeka.warworlds.model.ImageManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.ShieldManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;

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

        final Spinner stanceSpinner = (Spinner) findViewById(R.id.stance);
        final Button moveBtn = (Button) findViewById(R.id.move_btn);
        final Button viewBtn = (Button) findViewById(R.id.view_btn);
        final Button splitBtn = (Button) findViewById(R.id.split_btn);
        final Button mergeBtn = (Button) findViewById(R.id.merge_btn);

        moveBtn.setText("Move");
        if (mSelectedFleet != null) {
            stanceSpinner.setEnabled(true);
            if (viewBtn != null) {
                viewBtn.setEnabled(true);
            }

            if (mSelectedFleet.getState() == State.IDLE) {
                moveBtn.setEnabled(true);
                splitBtn.setEnabled(true);
                mergeBtn.setEnabled(true);
            } else if (mSelectedFleet.getState() == State.MOVING) {
                BoostFleetUpgrade boost = (BoostFleetUpgrade) mSelectedFleet.getUpgrade("boost");
                if (boost != null) {
                    moveBtn.setText("Boost");
                    moveBtn.setEnabled(!boost.isBoosting());
                } else {
                    moveBtn.setEnabled(false);
                }
                splitBtn.setEnabled(false);
                mergeBtn.setEnabled(false);
            } else {
                moveBtn.setEnabled(false);
                splitBtn.setEnabled(false);
                mergeBtn.setEnabled(false);
            }

            stanceSpinner.setSelection(mSelectedFleet.getStance().getValue() - 1);

            if (recentre) {
                int position = mFleetListAdapter.getItemPosition(mSelectedFleet);
                if (position >= 0) {
                    final ListView fleetList = (ListView) findViewById(R.id.ship_list);
                    fleetList.setSelection(position);
                }
            }
        } else {
            stanceSpinner.setEnabled(false);
            if (viewBtn != null) {
                viewBtn.setEnabled(false);
            }
            moveBtn.setEnabled(false);
            splitBtn.setEnabled(false);
            mergeBtn.setEnabled(false);
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
                Fleet.Stance stance = Fleet.Stance.values()[position];
                if (mSelectedFleet == null) {
                    return;
                }

                if (mSelectedFleet.getStance() != stance && mFleetActionListener != null) {
                    mFleetActionListener.onFleetStanceModified(
                            mStars.get(mSelectedFleet.getStarKey()),
                            mSelectedFleet, stance);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

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

        final Button splitBtn = (Button) findViewById(R.id.split_btn);
        splitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedFleet == null) {
                    return;
                }

                if (mFleetActionListener != null) {
                    mFleetActionListener.onFleetSplit(mStars.get(mSelectedFleet.getStarKey()),
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
                    Star star = mStars.get(mSelectedFleet.getStarKey());
                    if (mSelectedFleet.getState() == State.MOVING && mSelectedFleet.hasUpgrade("boost")) {
                        mFleetActionListener.onFleetBoost(star, mSelectedFleet);
                    } else if (mSelectedFleet.getState() == State.IDLE) {
                        mFleetActionListener.onFleetMove(star, mSelectedFleet);
                    }
                }
            }
        });

        final Button viewBtn = (Button) findViewById(R.id.view_btn);
        if (viewBtn != null) viewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedFleet == null) {
                    return;
                }

                if (mFleetActionListener != null) {
                    mFleetActionListener.onFleetView(mStars.get(mSelectedFleet.getStarKey()),
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
     * Populates a solarsystem_fleet_row.xml view with details from the given fleet.
     */
    public static void populateFleetRow(final Context context, final Map<String, Star> stars, 
                                        View view, final Fleet fleet) {
        ImageView icon = (ImageView) view.findViewById(R.id.ship_icon);
        final LinearLayout row1 = (LinearLayout) view.findViewById(R.id.ship_row1);
        final LinearLayout row2 = (LinearLayout) view.findViewById(R.id.ship_row2);
        final LinearLayout row3 = (LinearLayout) view.findViewById(R.id.ship_row3);
        final TextView notes = (TextView) view.findViewById(R.id.notes);

        row1.removeAllViews();
        row2.removeAllViews();
        row3.removeAllViews();

        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());
        icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

        populateFleetNameRow(context, row1, fleet, design);
        populateFleetStanceRow(context, row2, fleet);

        if (notes != null && fleet.getNotes() != null) {
            notes.setText(fleet.getNotes());
            notes.setVisibility(View.VISIBLE);
        } else if (notes != null) {
            notes.setText("");
            notes.setVisibility(View.GONE);
        }

        if (fleet.getState() == Fleet.State.MOVING) {
            row3.setVisibility(View.GONE);
            populateFleetDestinationRow(context, row3, fleet, stars, true);
        } else {
            row3.setVisibility(View.GONE);

            final MyEmpire myEmpire = EmpireManager.i.getEmpire();
            Empire empire = EmpireManager.i.getEmpire(fleet.getEmpireID());
            if (empire != null) {
                row3.setVisibility(View.VISIBLE);
    
                Bitmap shieldBmp = EmpireShieldManager.i.getShield(context, empire);
                if (shieldBmp != null) {
                    addImageToRow(context, row3, shieldBmp, 0);
                }
    
                if (myEmpire.getKey().equals(empire.getKey())) {
                    addTextToRow(context, row3, empire.getDisplayName(), 0);
                } else if (myEmpire.getAlliance() != null && empire.getAlliance() != null &&
                        myEmpire.getAlliance().getKey().equals(empire.getAlliance().getKey())) {
                    addTextToRow(context, row3, empire.getDisplayName(), 0);
                } else {
                    addTextToRow(context, row3, Html.fromHtml("<font color=\"red\">"+empire.getDisplayName()+"</font>"), 0);
                }
            }
        }
    }

    public static void populateFleetNameRow(Context context, LinearLayout row, Fleet fleet, ShipDesign design) {
        populateFleetNameRow(context, row, fleet, design, 0);
    }

    public static void populateFleetNameRow(Context context, LinearLayout row, Fleet fleet, ShipDesign design, float textSize) {
        if (fleet == null) {
            String text = String.format(Locale.ENGLISH, "%s", design.getDisplayName(false));
            addTextToRow(context, row, text, textSize);
        } else if (fleet.getUpgrades().size() == 0) {
            String text = String.format(Locale.ENGLISH, "%d × %s",
                    (int) Math.ceil(fleet.getNumShips()), design.getDisplayName(fleet.getNumShips() > 1));
            addTextToRow(context, row, text, textSize);
        } else {
            String text = String.format(Locale.ENGLISH, "%d ×", (int) Math.ceil(fleet.getNumShips()));
            addTextToRow(context, row, text, textSize);
            for (BaseFleetUpgrade upgrade : fleet.getUpgrades()) {
                Sprite sprite = SpriteManager.i.getSprite(design.getUpgrade(upgrade.getUpgradeID()).getSpriteName());
                addImageToRow(context, row, sprite, textSize);
            }
            text = String.format(Locale.ENGLISH, "%s", design.getDisplayName(fleet.getNumShips() > 1));
            addTextToRow(context, row, text, textSize);
        }
    }

    public static void populateFleetDestinationRow(Context context, LinearLayout row, Fleet fleet, boolean includeEta) {
        populateFleetDestinationRow(context, row, fleet, null, includeEta);
    }

    public static void fetchSrcDestStar(final Fleet fleet, Map<String, Star> stars, final SrcDestStarsFetchedHandler handler) {
        StarSummary srcStar = null;
        StarSummary destStar = null;
        if (stars != null) {
            srcStar = stars.get(fleet.getStarKey());
            destStar = stars.get(fleet.getDestinationStarKey());
        }

        if (srcStar == null) {
            SectorManager.getInstance().findStar(fleet.getStarKey());
        }
        if (destStar == null) {
            SectorManager.getInstance().findStar(fleet.getStarKey());
        }

        if (srcStar != null && destStar != null) {
            handler.onSrcDestStarsFetched(srcStar, destStar);
            return;
        }

        if (srcStar == null) {
            final StarSummary oldDestStar = destStar;
            StarManager.getInstance().requestStarSummary(fleet.getStarKey(),
                new StarManager.StarSummaryFetchedHandler() {
                    @Override
                    public void onStarSummaryFetched(final StarSummary srcStar) {
                        if (oldDestStar == null) {
                            StarManager.getInstance().requestStarSummary(fleet.getDestinationStarKey(),
                                    new StarManager.StarSummaryFetchedHandler() {
                                        @Override
                                        public void onStarSummaryFetched(StarSummary destStar) {
                                            handler.onSrcDestStarsFetched(srcStar, destStar);
                                        }
                            });
                        } else {
                            handler.onSrcDestStarsFetched(srcStar, oldDestStar);
                        }
                    }
            });
        } else {
            final StarSummary oldSrcStar = srcStar;
            StarManager.getInstance().requestStarSummary(fleet.getDestinationStarKey(),
                    new StarManager.StarSummaryFetchedHandler() {
                        @Override
                        public void onStarSummaryFetched(StarSummary destStar) {
                            handler.onSrcDestStarsFetched(oldSrcStar, destStar);
                        }
            });
        }
    }

    public interface SrcDestStarsFetchedHandler {
        void onSrcDestStarsFetched(StarSummary srcStar, StarSummary destStar);
    }

    public static void populateFleetDestinationRow(final Context context, final LinearLayout row,
            final Fleet fleet, Map<String, Star> stars, final boolean includeEta) {
        fetchSrcDestStar(fleet, stars, new SrcDestStarsFetchedHandler() {
            @Override
            public void onSrcDestStarsFetched(StarSummary srcStar, StarSummary destStar) {
                populateFleetDestinationRow(context, row, fleet, srcStar, destStar, includeEta);
            }
        });
    }

    private static void populateFleetDestinationRow(Context context, LinearLayout row, Fleet fleet, StarSummary src,
            StarSummary dest, boolean includeEta) {
        float timeRemainingInHours = fleet.getTimeToDestination();
        Sprite sprite = StarImageManager.getInstance().getSprite(dest, -1, true);
        String eta = TimeFormatter.create().format(timeRemainingInHours);

        float marginHorz = 0;
        float marginVert = 0;
        if (dest.getStarType().getImageScale() > 2.5) {
            marginHorz = -(float) (sprite.getWidth() / dest.getStarType().getImageScale());
            marginVert = -(float) (sprite.getHeight() / dest.getStarType().getImageScale());
        }

        BoostFleetUpgrade boostUpgrade = (BoostFleetUpgrade) fleet.getUpgrade("boost");
        if (boostUpgrade != null && boostUpgrade.isBoosting()) {
            addTextToRow(context, row, "→", 0);
        }
        addTextToRow(context, row, "→", 0);
        addImageToRow(context, row, sprite, marginHorz, marginVert, 0);
        String name = dest.getName();
        if (dest.getStarType().getInternalName().equals("marker")) {
            name = "<i>Empty Space</i>";
        }
        if (includeEta) {
            String text = String.format("%s <b>ETA:</b> %s",
                                        name, eta);
            addTextToRow(context, row, Html.fromHtml(text), 0);
        } else {
            addTextToRow(context, row, Html.fromHtml(name), 0);
        }

        row.setVisibility(View.VISIBLE);
    }

    public static void populateFleetStanceRow(Context context, LinearLayout row, Fleet fleet) {
        String text = String.format("%s (stance: %s)",
                StringUtils.capitalize(fleet.getState().toString().toLowerCase(Locale.ENGLISH)),
                StringUtils.capitalize(fleet.getStance().toString().toLowerCase(Locale.ENGLISH)));
        addTextToRow(context, row, text, 0);
    }

    private static void addTextToRow(Context context, LinearLayout row, CharSequence text, float size) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setSingleLine(true);
        tv.setEllipsize(TruncateAt.END);
        if (size != 0) {
            tv.setTextSize(size);
        }
        row.addView(tv);
    }

    private static void addImageToRow(Context context, LinearLayout row, Sprite sprite, float size) {
        addImageToRow(context, row, sprite, 0, 0, size);
    }
    private static void addImageToRow(Context context, LinearLayout row, Bitmap bmp, float size) {
        ImageView iv = new ImageView(context);
        iv.setImageBitmap(bmp);

        if (size == 0) {
            size = 16.0f;
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) size * 2, (int) size * 2);
        lp.setMargins(5, -5, 5, -5);
        iv.setLayoutParams(lp);

        row.addView(iv);
    }

    private static void addImageToRow(Context context, LinearLayout row, Sprite sprite, float marginHorz, float marginVert, float size) {
        ImageView iv = new ImageView(context);
        iv.setImageDrawable(new SpriteDrawable(sprite));

        if (size == 0) {
            size = 16.0f;
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) size * 2, (int) size * 2);
        lp.setMargins((int) (marginHorz + 5), (int) (marginVert - 5), (int) (marginHorz + 5), (int) (marginVert - 5));
        iv.setLayoutParams(lp);

        row.addView(iv);
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
                    view = inflater.inflate(R.layout.fleet_list_star_row, null);
                } else {
                    view = inflater.inflate(R.layout.fleet_list_row, null);
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
                populateFleetRow(mContext, mStars, view, fleet);

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

    public class StanceAdapter extends BaseAdapter implements SpinnerAdapter {
        Fleet.Stance[] mValues;

        public StanceAdapter() {
            mValues = Fleet.Stance.values();
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
            return mValues[position].getValue();
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

            Fleet.Stance value = mValues[position];
            view.setText(StringUtils.capitalize(value.toString().toLowerCase(Locale.ENGLISH)));
            return view;
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
