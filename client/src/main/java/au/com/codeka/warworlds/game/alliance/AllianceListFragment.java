package au.com.codeka.warworlds.game.alliance;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.AllianceShieldManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.ShieldManager;

public class AllianceListFragment extends Fragment {
  private View view;
  private RankListAdapter rankListAdapter;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    view = inflater.inflate(R.layout.alliance_overview_tab, container, false);
    rankListAdapter = new RankListAdapter();

    final Button createBtn = view.findViewById(R.id.create_alliance_btn);
    createBtn.setOnClickListener(v -> onAllianceCreate());

    final CheckBox showInactiveChk = view.findViewById(R.id.show_inactive);
    showInactiveChk.setOnCheckedChangeListener((compoundButton, b) -> refresh());

    ListView alliancesList = view.findViewById(R.id.alliances);
    alliancesList.setAdapter(rankListAdapter);
    alliancesList.setOnItemClickListener((parent, view, position, id) -> {
      RankListAdapter.ItemEntry item =
          (RankListAdapter.ItemEntry) rankListAdapter.getItem(position);
      if (item.alliance != null) {
        AllianceDetailsFragmentArgs args =
            new AllianceDetailsFragmentArgs.Builder(item.alliance.getID()).build();
        NavHostFragment.findNavController(this).navigate(
            R.id.allianceDetailsFragment, args.toBundle());
      }
    });

    refresh();
    return view;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    AllianceManager.eventBus.register(eventHandler);
    ShieldManager.eventBus.register(eventHandler);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    AllianceManager.eventBus.unregister(eventHandler);
    ShieldManager.eventBus.unregister(eventHandler);
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onAllianceUpdated(Alliance alliance) {
      refresh();
    }

    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      rankListAdapter.notifyDataSetChanged();
    }
  };

  private void onAllianceCreate() {
    AllianceCreateDialog dialog = new AllianceCreateDialog();
    dialog.show(getChildFragmentManager(), "");
  }

  private void refresh() {
    final ProgressBar progressBar = view.findViewById(R.id.loading);
    final ListView alliancesList = view.findViewById(R.id.alliances);
    alliancesList.setVisibility(View.GONE);
    progressBar.setVisibility(View.VISIBLE);

    boolean hideDead = !((CheckBox) view.findViewById(R.id.show_inactive)).isChecked();
    AllianceManager.i.fetchAlliances(hideDead,
        new AllianceManager.FetchAlliancesCompleteHandler() {
          @Override
          public void onAlliancesFetched(List<Alliance> alliances) {
            rankListAdapter.setAlliances(alliances);

            alliancesList.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
          }
        });
  }

  private class RankListAdapter extends BaseAdapter {
    private ArrayList<ItemEntry> entries;

    public void setAlliances(List<Alliance> alliances) {
      Alliance myAlliance = (Alliance) EmpireManager.i.getEmpire().getAlliance();
      // remove my alliance from the list, it'll always go at the front
      if (myAlliance != null) {
        for (int i = 0; i < alliances.size(); i++) {
          if (alliances.get(i).getKey().equals(myAlliance.getKey())) {
            myAlliance = alliances.get(i); // this'll ensure it's the most recent
            alliances.remove(i);
            break;
          }
        }
      }

      ArrayList<Alliance> sorted = new ArrayList<>(alliances);
      Collections.sort(sorted, new Comparator<Alliance>() {
        @Override
        public int compare(Alliance lhs, Alliance rhs) {
          if (lhs.getNumMembers() == rhs.getNumMembers()) {
            return lhs.getName().compareTo(rhs.getName());
          } else {
            return rhs.getNumMembers() - lhs.getNumMembers();
          }
        }
      });

      entries = new ArrayList<>();
      if (myAlliance != null) {
        entries.add(new ItemEntry(myAlliance));
        entries.add(new ItemEntry(null));
      }
      for (Alliance alliance : sorted) {
        entries.add(new ItemEntry(alliance));
      }

      notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() {
      return 2;
    }

    @Override
    public int getItemViewType(int position) {
      ItemEntry entry = entries.get(position);
      return (entry.alliance == null ? 0 : 1);
    }

    @Override
    public int getCount() {
      if (entries == null)
        return 0;
      return entries.size();
    }

    @Override
    public boolean isEnabled(int position) {
      return entries != null && (entries.get(position).alliance != null);
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
      ItemEntry entry = entries.get(position);
      Activity activity = getActivity();
      View view = convertView;

      if (view == null) {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService
            (Context.LAYOUT_INFLATER_SERVICE);
        if (entry.alliance == null) {
          view = new View(activity);
          view.setLayoutParams(
              new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 10));
        } else {
          view = inflater.inflate(R.layout.alliance_overview_rank_row, parent, false);
        }
      }

      if (entry.alliance != null) {
        TextView allianceName = view.findViewById(R.id.alliance_name);
        allianceName.setText(entry.alliance.getName());

        TextView allianceMembers = view.findViewById(R.id.alliance_num_members);
        allianceMembers.setText(
            String.format(Locale.US, "Members: %d • Stars: %d",
                entry.alliance.getNumMembers(), entry.alliance.getTotalStars()));

        ImageView allianceIcon = view.findViewById(R.id.alliance_icon);
        allianceIcon.setImageBitmap(
            AllianceShieldManager.i.getShield(getActivity(), entry.alliance));
      }

      return view;
    }

    public class ItemEntry {
      public Alliance alliance;

      public ItemEntry(Alliance alliance) {
        this.alliance = alliance;
      }
    }
  }
}
