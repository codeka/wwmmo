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
  private FleetListAdapter fleetListAdapter;
  protected Fleet selectedFleet;
  private List<Fleet> fleets;
  private Map<String, Star> stars;
  private Context context;
  private boolean isInitialized;
  private FleetSelectionPanel fleetSelectionPanel;
  private OnFleetActionListener fleetActionListener;

  public FleetList(Context context, AttributeSet attrs) {
    this(context, attrs, R.layout.fleet_list_ctrl);
  }

  public FleetList(Context context) {
    this(context, null, R.layout.fleet_list_ctrl);
  }

  protected FleetList(Context context, AttributeSet attrs, int layoutID) {
    super(context, attrs);
    this.context = context;

    View child = inflate(context, layoutID, null);
    this.addView(child);
  }

  public void setOnFleetActionListener(OnFleetActionListener listener) {
    fleetActionListener = listener;
    if (fleetSelectionPanel != null) {
      fleetSelectionPanel.setOnFleetActionListener(listener);
    }
  }

  public void refresh(List<BaseFleet> fleets, Map<String, Star> stars) {
    if (fleets != null) {
      this.fleets = new ArrayList<>();
      for (BaseFleet f : fleets) {
        this.fleets.add((Fleet) f);
      }
    }
    this.stars = stars;

    initialize();

    // if we had a fleet selected, make sure we still have the same fleet selected after we refresh
    if (selectedFleet != null) {
      Fleet selectedFleet = this.selectedFleet;
      this.selectedFleet = null;

      for (Fleet f : this.fleets) {
        if (f.getKey().equals(selectedFleet.getKey())) {
          this.selectedFleet = f;
          break;
        }
      }
    }
    if (selectedFleet != null) {
      selectFleet(selectedFleet.getID(), false);
    } else {
      selectFleet(-1, false);
    }

    fleetListAdapter.setFleets(stars, this.fleets);
  }

  public void selectFleet(int fleetID, boolean recentre) {
    selectedFleet = null;
    for (Fleet f : fleets) {
      if (fleetID > 0 && f.getID() == fleetID) {
        selectedFleet = f;
      }
    }

    if (selectedFleet != null && recentre) {
      int position = fleetListAdapter.getItemPosition(selectedFleet);
      if (position >= 0) {
        final ListView fleetList = findViewById(R.id.ship_list);
        fleetList.setSelection(position);
      }
    }

    if (selectedFleet != null) {
      fleetSelectionPanel.setSelectedFleet(stars.get(selectedFleet.getStarKey()),
          selectedFleet);
    }
    fleetListAdapter.notifyDataSetChanged();
  }

  private void initialize() {
    if (isInitialized) {
      return;
    }
    isInitialized = true;

    fleetListAdapter = new FleetListAdapter();
    final ListView fleetList = findViewById(R.id.ship_list);
    fleetList.setAdapter(fleetListAdapter);

    fleetSelectionPanel = findViewById(R.id.bottom_pane);
    fleetSelectionPanel.setOnFleetActionListener(fleetActionListener);

    fleetList.setOnItemClickListener((parent, view, position, id) -> {
      FleetListAdapter.ItemEntry entry =
          (FleetListAdapter.ItemEntry) fleetListAdapter.getItem(position);
      if (entry.type == FleetListAdapter.FLEET_ITEM_TYPE) {
        selectFleet(((Fleet) entry.value).getID(), false);
      }
    });

    fleetList.setOnItemLongClickListener((parent, view, position, id) -> {
      FleetListAdapter.ItemEntry entry =
          (FleetListAdapter.ItemEntry) fleetListAdapter.getItem(position);
      if (entry.type != FleetListAdapter.FLEET_ITEM_TYPE) {
        return false;
      }

      final Fleet fleet = (Fleet) entry.value;
      NotesDialog dialog = new NotesDialog();
      dialog.setup(fleet.getNotes(), notes -> {
        fleet.setNotes(notes);
        fleetListAdapter.notifyDataSetChanged();

        FleetManager.i.updateNotes(fleet);
      });

      dialog.show(((BaseActivity) context).getSupportFragmentManager(), "");
      return true;
    });

    onInitialize();

    StarManager.eventBus.register(eventHandler);
    ImageManager.eventBus.register(eventHandler);
    ShieldManager.eventBus.register(eventHandler);
  }

  protected void onInitialize() {
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    ShieldManager.eventBus.unregister(eventHandler);
    ImageManager.eventBus.unregister(eventHandler);
    StarManager.eventBus.unregister(eventHandler);
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onStarUpdated(Star s) {
      for (String starKey : stars.keySet()) {
        if (starKey.equals(s.getKey())) {
          stars.put(s.getKey(), s);

          Iterator<Fleet> it = fleets.iterator();
          while (it.hasNext()) {
            Fleet f = it.next();
            if (f.getStarKey().equals(starKey)) {
              it.remove();
            }
          }

          for (int j = 0; j < s.getFleets().size(); j++) {
            fleets.add((Fleet) s.getFleets().get(j));
          }

          refresh(null, stars);
          break;
        }
      }
    }

    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      fleetListAdapter.notifyDataSetChanged();
    }

    @EventHandler
    public void onSpriteGenerated(ImageManager.SpriteGeneratedEvent event) {
      fleetListAdapter.notifyDataSetChanged();
    }

    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      fleetListAdapter.notifyDataSetChanged();
    }
  };

  /**
   * Filters the given list of fleets, removing the ones with no ships.
   */
  public static ArrayList<Fleet> filterFleets(List<Fleet> fleets) {
    ArrayList<Fleet> filtered = new ArrayList<>();
    for (Fleet fleet : fleets) {
      if (fleet.getNumShips() > 0.01f) {
        filtered.add(fleet);
      }
    }
    return filtered;
  }

  /**
   * This adapter is used to populate the list of ship fleets that the current colony has.
   */
  private class FleetListAdapter extends BaseAdapter {
    private ArrayList<Fleet> fleets;
    private Map<String, Star> stars;
    private ArrayList<ItemEntry> entries;
    private Empire myEmpire;

    private static final int STAR_ITEM_TYPE = 0;
    private static final int FLEET_ITEM_TYPE = 1;

    public FleetListAdapter() {
      myEmpire = EmpireManager.i.getEmpire();
    }

    /**
     * Sets the list of fleets that we'll be displaying.
     */
    public void setFleets(Map<String, Star> stars, List<Fleet> fleets) {
      this.fleets = filterFleets(fleets);
      this.stars = stars;

      Collections.sort(this.fleets, (lhs, rhs) -> {
        // sort by star, then by design, then by count
        if (!lhs.getStarKey().equals(rhs.getStarKey())) {
          Star lhsStar = FleetListAdapter.this.stars.get(lhs.getStarKey());
          Star rhsStar = FleetListAdapter.this.stars.get(rhs.getStarKey());
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
      });

      entries = new ArrayList<>();
      String lastStarKey = "";
      for (Fleet f : this.fleets) {
        if (!f.getStarKey().equals(lastStarKey)) {
          entries.add(new ItemEntry(STAR_ITEM_TYPE, this.stars.get(f.getStarKey())));
          lastStarKey = f.getStarKey();
        }
        entries.add(new ItemEntry(FLEET_ITEM_TYPE, f));
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
      if (entries == null)
        return 0;

      return entries.get(position).type;
    }

    @Override
    public boolean isEnabled(int position) {
      if (entries.get(position).type == STAR_ITEM_TYPE) {
        return false;
      }

      // if we don't own this fleet, we also can't do anything with it.
      Fleet fleet = (Fleet) entries.get(position).value;
      if (fleet.getEmpireKey() == null) {
        return false;
      }
      if (!fleet.getEmpireKey().equals(myEmpire.getKey())) {
        return false;
      }

      return true;
    }

    @Override
    public int getCount() {
      if (entries == null)
        return 0;
      return entries.size();
    }

    @Override
    public Object getItem(int position) {
      if (entries == null)
        return null;
      return entries.get(position);
    }

    public int getItemPosition(Fleet fleet) {
      int index = 0;
      for (; index < entries.size(); index++) {
        ItemEntry entry = entries.get(index);
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
      ItemEntry entry = entries.get(position);
      View view = convertView;

      if (view == null) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService
            (Context.LAYOUT_INFLATER_SERVICE);
        if (entry.type == STAR_ITEM_TYPE) {
          view = inflater.inflate(R.layout.fleet_list_star_row, parent, false);
        } else {
          view = new FleetListRow(context);
        }
      }

      if (entry.type == STAR_ITEM_TYPE) {
        Star star = (Star) entry.value;
        ImageView icon = view.findViewById(R.id.star_icon);
        TextView name = view.findViewById(R.id.star_name);

        int imageSize = (int) (star.getSize() * star.getStarType().getImageScale() * 2);
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

        if (selectedFleet != null && selectedFleet.getKey().equals(fleet.getKey())) {
          view.setBackgroundResource(R.color.list_item_selected);
        } else {
          view.setBackgroundResource(android.R.color.transparent);
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
