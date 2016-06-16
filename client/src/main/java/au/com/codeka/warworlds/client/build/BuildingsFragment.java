package au.com.codeka.warworlds.client.build;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.common.sim.DesignHelper;
import au.com.codeka.warworlds.common.proto.Building;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Star;

public class BuildingsFragment extends BuildFragment.BaseTabFragment {

  @Override
  protected int getViewResourceId() {
    return R.layout.frag_build_buildings;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    BuildingListAdapter adapter = new BuildingListAdapter();
    adapter.setColony(getStar(), getColony());
    ((ListView) view.findViewById(R.id.building_list)).setAdapter(adapter);
  }

  /** This adapter is used to populate a list of buildings in a list view. */
  private class BuildingListAdapter extends BaseAdapter {
    private ArrayList<Entry> entries;

    private static final int HEADING_TYPE = 0;
    private static final int EXISTING_BUILDING_TYPE = 1;
    private static final int NEW_BUILDING_TYPE = 2;

    public void setColony(Star star, Colony colony) {
      entries = new ArrayList<>();

      List<Building> buildings = colony.buildings;
      if (buildings == null) {
        buildings = new ArrayList<>();
      }

      ArrayList<Entry> existingBuildingEntries = new ArrayList<>();
      for (Building b : buildings) {
        Entry entry = new Entry();
        entry.building = b;
//        if (star.build_requests != null) {
//          // if the building is being upgraded (i.e. if there's a build request that
//          // references this building) then add the build request as well
//          for (BaseBuildRequest br : star.getBuildRequests()) {
//            if (br.getExistingBuildingKey() != null && br.getExistingBuildingKey().equals(b.getKey())) {
//              entry.buildRequest = (BuildRequest) br;
//            }
//          }
//        }
        existingBuildingEntries.add(entry);
      }

//      for (BaseBuildRequest br : star.getBuildRequests()) {
//        if (br.getColonyKey().equals(colony.getKey()) &&
//            br.getDesignKind().equals(DesignKind.BUILDING) &&
//            br.getExistingBuildingKey() == null) {
//          Entry entry = new Entry();
//          entry.buildRequest = (BuildRequest) br;
//          existingBuildingEntries.add(entry);
//        }
//      }

      Collections.sort(existingBuildingEntries, new Comparator<Entry>() {
        @Override
        public int compare(Entry lhs, Entry rhs) {
//          String a = (lhs.building != null ? lhs.building.design_id : lhs.buildRequest.design_id);
//          String b = (rhs.building != null ? rhs.building.design_id : rhs.buildRequest.design_id);
          Design.DesignType a = lhs.building.design_type;
          Design.DesignType b = rhs.building.design_type;
          return a.compareTo(b);
        }
      });

      Entry title = new Entry();
      title.title = "New Buildings";
      entries.add(title);

      for (Design design : DesignHelper.getDesigns(Design.DesignKind.BUILDING)) {
        if (design.max_per_colony != null && design.max_per_colony > 0) {
          int numExisting = 0;
          for (Entry e : existingBuildingEntries) {
            if (e.building != null) {
              if (e.building.design_type.equals(design.type)) {
                numExisting ++;
              }
            }// else if (e.buildRequest != null) {
             // if (e.buildRequest.getDesignID().equals(bd.getID())) {
             //   numExisting ++;
             // }
            //}
          }
          if (numExisting >= design.max_per_colony) {
            continue;
          }
        }
       // if (bd.getMaxPerEmpire() > 0) {
       //   int numExisting = BuildManager.i.getTotalBuildingsInEmpire(bd.getID());
       //   // If you're building one, we'll still think it's OK to build again, but it's
       //   // actually going to be blocked by the server.
       //   if (numExisting >= bd.getMaxPerEmpire()) {
       //     continue;
       //   }
       // }
        Entry entry = new Entry();
        entry.design = design;
        entries.add(entry);
      }

      title = new Entry();
      title.title = "Existing Buildings";
      entries.add(title);

      for (Entry entry : existingBuildingEntries) {
        entries.add(entry);
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
      if (entries == null)
        return 0;

      if (entries.get(position).title != null)
        return HEADING_TYPE;
      if (entries.get(position).design != null)
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
      Entry entry = entries.get(position);
      if (entry.building != null) {
        int maxUpgrades = DesignHelper.getDesign(entry.building.design_type).upgrades.size();
        if (entry.building.level > maxUpgrades) {
          return false;
        }
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

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View view = convertView;
      if (view == null) {
        LayoutInflater inflater =
            (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int viewType = getItemViewType(position);
        if (viewType == HEADING_TYPE) {
          view = new TextView(getContext());
        } else {
          view = inflater.inflate(R.layout.ctrl_build_design, parent, false);
        }
      }

      Entry entry = entries.get(position);
      if (entry.title != null) {
        TextView tv = (TextView) view;
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setText(entry.title);
      } else if (entry.building != null /*|| entry.buildRequest != null*/) {
        // existing building/upgrading building
        ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
        LinearLayout row1 = (LinearLayout) view.findViewById(R.id.design_row1);
        TextView row2 = (TextView) view.findViewById(R.id.design_row2);
        TextView row3 = (TextView) view.findViewById(R.id.design_row3);
        TextView level = (TextView) view.findViewById(R.id.build_level);
        TextView levelLabel = (TextView) view.findViewById(R.id.build_level_label);
        ProgressBar progress = (ProgressBar) view.findViewById(R.id.build_progress);
        TextView notes = (TextView) view.findViewById(R.id.notes);

        Building building = entry.building;
        //BuildRequest buildRequest = entry.buildRequest;
        Design design = DesignHelper.getDesign(
            (building != null ? building.design_type : /*buildRequest.getDesignID()*/null));

        BuildHelper.setDesignIcon(design, icon);
        int numUpgrades = design.upgrades.size();

        if (numUpgrades == 0 || building == null) {
          level.setVisibility(View.GONE);
          levelLabel.setVisibility(View.GONE);
        } else {
          level.setText(String.format(Locale.US, "%d", building.level));
          level.setVisibility(View.VISIBLE);
          levelLabel.setVisibility(View.VISIBLE);
        }

        row1.removeAllViews();
        addTextToRow(getContext(), row1, design.display_name);
        /*if (buildRequest != null) {
          String verb = (building == null ? "Building" : "Upgrading");
          row2.setText(Html.fromHtml(String.format(Locale.ENGLISH,
              "<font color=\"#0c6476\">%s:</font> %d %%, %s left",
              verb, (int) buildRequest.getPercentComplete(),
              TimeFormatter.create().format(buildRequest.getRemainingTime()))));

          row3.setVisibility(View.GONE);
          progress.setVisibility(View.VISIBLE);
          progress.setProgress((int) buildRequest.getPercentComplete());
        } else*/ if (building != null) {
          if (numUpgrades < building.level) {
            row2.setText(getContext().getString(R.string.no_more_upgrades));
            row3.setVisibility(View.GONE);
            progress.setVisibility(View.GONE);
          } else {
            progress.setVisibility(View.GONE);

            String requiredHtml =
                DesignHelper.getDependenciesHtml(getColony(), design, building.level + 1);
            row2.setText(Html.fromHtml(requiredHtml));

            row3.setVisibility(View.GONE);
          }
        }

        if (building != null && building.notes != null) {
          notes.setText(building.notes);
          notes.setVisibility(View.VISIBLE);
        } /*else if (buildRequest != null && buildRequest.getNotes() != null) {
          notes.setText(buildRequest.getNotes());
          notes.setVisibility(View.VISIBLE);
        }*/ else {
          notes.setText("");
          notes.setVisibility(View.GONE);
        }
      } else {
        // new building
        ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
        LinearLayout row1 = (LinearLayout) view.findViewById(R.id.design_row1);
        TextView row2 = (TextView) view.findViewById(R.id.design_row2);
        TextView row3 = (TextView) view.findViewById(R.id.design_row3);

        view.findViewById(R.id.build_progress).setVisibility(View.GONE);
        view.findViewById(R.id.build_level).setVisibility(View.GONE);
        view.findViewById(R.id.build_level_label).setVisibility(View.GONE);
        view.findViewById(R.id.notes).setVisibility(View.GONE);

        Design design = entries.get(position).design;
        BuildHelper.setDesignIcon(design, icon);

        row1.removeAllViews();
        addTextToRow(getContext(), row1, design.display_name);
        String requiredHtml = DesignHelper.getDependenciesHtml(getColony(), design);
        row2.setText(Html.fromHtml(requiredHtml));

        row3.setVisibility(View.GONE);
      }

      return view;
    }

    private void addTextToRow(Context context, LinearLayout row, CharSequence text) {
      TextView tv = new TextView(context);
      tv.setText(text);
      tv.setSingleLine(true);
      tv.setEllipsize(TextUtils.TruncateAt.END);
      row.addView(tv);
    }
  }

  public static class Entry {
    public String title;
  //  public BuildRequest buildRequest;
    public Building building;
    public Design design;
  }
}
