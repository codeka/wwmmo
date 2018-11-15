package au.com.codeka.warworlds.client.game.build;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.fleets.FleetListHelper;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.BuildHelper;
import au.com.codeka.warworlds.common.sim.DesignHelper;

public class ShipsView extends ListView implements TabContentView {
  private final Context context;
  private final Star star;
  private final Colony colony;

  private ShipListAdapter shipListAdapter;

  public ShipsView(Context context, Star star, Colony colony, BuildLayout buildLayout) {
    super(context);
    this.context = context;
    this.star = star;
    this.colony = colony;

    shipListAdapter = new ShipListAdapter();
    refresh(star, colony);

    setAdapter(shipListAdapter);
    setOnItemClickListener((parent, view, position, id) -> {
      ItemEntry entry = (ItemEntry) shipListAdapter.getItem(position);
      if (entry.fleet == null && entry.buildRequest == null) {
        // It's a new fleet
        buildLayout.showBuildSheet(entry.design);
      } else if (entry.fleet != null && entry.buildRequest == null) {
        // TODO: upgrade
        buildLayout.showBuildSheet(entry.design);
      } else {
        buildLayout.showProgressSheet(null, entry.buildRequest);
      }
    });

    setOnItemLongClickListener((adapterView, view, position, id) -> {
      final ItemEntry entry = (ItemEntry) shipListAdapter.getItem(position);

      //NotesDialog dialog = new NotesDialog();
      //dialog.setup(entry.fleet == null ? entry.buildRequest.getNotes() : entry.fleet.getNotes(),
      //    new NotesDialog.NotesChangedHandler() {
      //      @Override
      //      public void onNotesChanged(String notes) {
      //        if (entry.fleet != null) {
      //          entry.fleet.setNotes(notes);
      //        } else if (entry.buildRequest != null) {
      //          entry.buildRequest.setNotes(notes);
      //        }
      //        shipListAdapter.notifyDataSetChanged();

      //        if (entry.fleet != null) {
      //          FleetManager.i.updateNotes(entry.fleet);
      //        } else {
      //          BuildManager.i.updateNotes(entry.buildRequest.getKey(), notes);
      //        }
      //      }
      //    });

      //dialog.show(getActivity().getSupportFragmentManager(), "");
      return true;
    });
  }

  @Override
  public void refresh(Star star, Colony colony) {
    Empire myEmpire = Preconditions.checkNotNull(EmpireManager.i.getMyEmpire());
    ArrayList<Fleet> fleets = new ArrayList<>();
    for (Fleet fleet : star.fleets) {
      if (fleet.empire_id != null && myEmpire.id.equals(fleet.empire_id)) {
        fleets.add(fleet);
      }
    }

    ArrayList<BuildRequest> buildRequests = new ArrayList<>();
    for (BuildRequest br : BuildHelper.getBuildRequests(star)) {
      Design design = DesignHelper.getDesign(br.design_type);
      if (design.design_kind == Design.DesignKind.SHIP) {
        buildRequests.add(br);
      }
    }

    shipListAdapter.refresh(fleets, buildRequests);
  }

  /** This adapter is used to populate the list of ship designs in our view. */
  private class ShipListAdapter extends BaseAdapter {
    private List<ItemEntry> entries;

    private static final int HEADING_TYPE = 0;
    private static final int EXISTING_SHIP_TYPE = 1;
    private static final int NEW_SHIP_TYPE = 2;

    public void refresh(ArrayList<Fleet> fleets, ArrayList<BuildRequest> buildRequests) {
      entries = new ArrayList<>();

      entries.add(new ItemEntry("New Ships"));
      for (Design design : DesignHelper.getDesigns(Design.DesignKind.SHIP)) {
        entries.add(new ItemEntry(design));
      }

      entries.add(new ItemEntry("Existing Ships"));
      for (Fleet fleet : fleets) {
        if (fleet.state != Fleet.FLEET_STATE.IDLE) {
          continue;
        }
        ItemEntry entry = new ItemEntry(fleet);
        for (BuildRequest buildRequest : buildRequests) {
         // if (buildRequest.getExistingFleetID() != null
        //      && ((int) buildRequest.getExistingFleetID()) == Integer.parseInt(fleet.getKey())) {
         //   entry.buildRequest = buildRequest;
         // }
        }
        entries.add(entry);
      }
      for (BuildRequest buildRequest : buildRequests) {
        //if (buildRequest.getExistingFleetID() != null) {
        //  continue;
       // }
        entries.add(new ItemEntry(buildRequest));
      }

      notifyDataSetChanged();
    }

    /**
     * We have three types of items, the "headings", the list of existing buildings and the list
     * of building designs.
     */
    @Override
    public int getViewTypeCount() {
      return 3;
    }

    @Override
    public boolean isEnabled(int position) {
      if (getItemViewType(position) == HEADING_TYPE) {
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
    public int getItemViewType(int position) {
      if (entries == null)
        return 0;

      if (entries.get(position).heading != null)
        return HEADING_TYPE;
      if (entries.get(position).design != null)
        return NEW_SHIP_TYPE;
      return EXISTING_SHIP_TYPE;
    }

    @Override
    public Object getItem(int position) {
      if (entries == null) {
        return null;
      }
      return entries.get(position);
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
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
            Context.LAYOUT_INFLATER_SERVICE);
        if (entry.heading != null) {
          view = new TextView(getContext());
        } else {
          view = inflater.inflate(R.layout.ctrl_build_design, parent, false);
        }
      }

      if (entry.heading != null) {
        TextView tv = (TextView) view;
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setText(entry.heading);
      } else if (entry.fleet != null || entry.buildRequest != null) {
        // existing fleet/upgrading fleet
        ImageView icon = view.findViewById(R.id.building_icon);
        TextView row1 = view.findViewById(R.id.design_row1);
        TextView row2 = view.findViewById(R.id.design_row2);
        TextView row3 = view.findViewById(R.id.design_row3);
        TextView level = view.findViewById(R.id.build_level);
        TextView levelLabel = view.findViewById(R.id.build_level_label);
        ProgressBar progress = view.findViewById(R.id.build_progress);
        TextView notes = view.findViewById(R.id.notes);

        Fleet fleet = entry.fleet;
        BuildRequest buildRequest = entry.buildRequest;
        Design design = DesignHelper.getDesign(
            fleet != null ? fleet.design_type : buildRequest.design_type);
        BuildViewHelper.setDesignIcon(design, icon);

        int numUpgrades = design.upgrades.size();
        if (numUpgrades == 0 || fleet == null) {
          level.setVisibility(View.GONE);
          levelLabel.setVisibility(View.GONE);
        } else {
          // TODO
          level.setText("?");
          level.setVisibility(View.VISIBLE);
          levelLabel.setVisibility(View.VISIBLE);
        }

        if (fleet == null) {
          row1.setText(FleetListHelper.getFleetName(buildRequest, design));
        } else {
          row1.setText(FleetListHelper.getFleetName(fleet, design));
        }
        if (buildRequest != null) {
          String verb = (fleet == null ? "Building" : "Upgrading");
          row2.setText(Html.fromHtml(String.format(Locale.ENGLISH,
              "<font color=\"#0c6476\">%s:</font> %d %%, %s left", verb,
              Math.round(buildRequest.progress * 100.0f),
              BuildHelper.formatTimeRemaining(buildRequest))));

          row3.setVisibility(View.GONE);
          progress.setVisibility(View.VISIBLE);
          progress.setProgress(Math.round(buildRequest.progress * 100.0f));
        } else {
          String upgrades = "";
          for (Design.Upgrade upgrade : design.upgrades) {
            //if (fleet != null && !fleet.hasUpgrade(upgrade.getID())) {
            //  if (upgrades.length() > 0) {
            //    upgrades += ", ";
            //  }
            //  upgrades += upgrade.getDisplayName();
            //}
          }

          progress.setVisibility(View.GONE);
          if (upgrades.length() == 0) {
            row2.setText(Html.fromHtml("Upgrades: <i>none</i>"));
          } else {
            row2.setText(String.format(Locale.US, "Upgrades: %s", upgrades));
          }

          String requiredHtml = DesignHelper.getDependenciesHtml(colony, design);
          row3.setVisibility(View.VISIBLE);
          row3.setText(Html.fromHtml(requiredHtml));
        }

        if (fleet != null && fleet.notes != null) {
          notes.setText(fleet.notes);
          notes.setVisibility(View.VISIBLE);
        //} else if (buildRequest != null && buildRequest.getNotes() != null) {
        //  notes.setText(buildRequest.getNotes());
        //  notes.setVisibility(View.VISIBLE);
        } else {
          notes.setText("");
          notes.setVisibility(View.GONE);
        }
      } else {
        // new fleet
        ImageView icon = view.findViewById(R.id.building_icon);
        TextView row1 = view.findViewById(R.id.design_row1);
        TextView row2 = view.findViewById(R.id.design_row2);
        TextView row3 = view.findViewById(R.id.design_row3);

        view.findViewById(R.id.build_progress).setVisibility(View.GONE);
        view.findViewById(R.id.build_level).setVisibility(View.GONE);
        view.findViewById(R.id.build_level_label).setVisibility(View.GONE);
        view.findViewById(R.id.notes).setVisibility(View.GONE);

        Design design = entry.design;
        BuildViewHelper.setDesignIcon(design, icon);

        row1.setText(FleetListHelper.getFleetName((Fleet) null, design));
        String requiredHtml = DesignHelper.getDependenciesHtml(colony, design);
        row2.setText(Html.fromHtml(requiredHtml));

        row3.setVisibility(View.GONE);
      }

      return view;
    }
  }

  public static class ItemEntry {
    Design design;
    Fleet fleet;
    BuildRequest buildRequest;
    String heading;

    ItemEntry(Design design) {
      this.design = design;
    }

    ItemEntry(BuildRequest buildRequest) {
      this.buildRequest = buildRequest;
    }

    ItemEntry(Fleet fleet) {
      this.fleet = fleet;
    }

    ItemEntry(String heading) {
      this.heading = heading;
    }
  }
}
