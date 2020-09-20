package au.com.codeka.warworlds.ctrl;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import au.com.codeka.common.Log;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceShieldManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireBattleRank;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireRank;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;

/**
 * Class that helpers populate a {@link androidx.recyclerview.widget.RecyclerView} for use with
 * empire ranks.
 */
public class EmpireRankRecyclerViewHelper {
  private static final Log log = new Log("EmpireRankRecyclerViewHelper");

  public interface RowsFetchCallback {
    void onRowsFetched(List<Empire> empires);
    void onBattleRankRowsFetched(List<EmpireBattleRank> ranks);
  }

  public interface Callbacks {
    /**
     * Called when an empire row is clicked on.
     * @param empire The {@link Empire} that was clicked on.
     */
    void onEmpireClick(Empire empire);

    /**
     * Called to fetch more rows.
     * @param startPosition The start index to fetch.
     * @param count The number of results to return.
     * @param callback A callback that you should call when fetching is complete.
     */
    void fetchRows(int startPosition, int count, RowsFetchCallback callback);
  }

  private final RecyclerView recyclerView;
  private final Callbacks callbacks;
  private final EmpireAdapter adapter;

  public EmpireRankRecyclerViewHelper(RecyclerView recyclerView, Callbacks callbacks) {
    this.recyclerView = recyclerView;
    this.callbacks = callbacks;
    this.adapter = new EmpireAdapter(recyclerView);

    recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
    recyclerView.setVisibility(View.GONE);
    recyclerView.setAdapter(adapter);

    DividerItemDecoration dividerItemDecoration =
        new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
    recyclerView.addItemDecoration(dividerItemDecoration);

    callbacks.fetchRows(0, 30, rowsFetchCallback);
  }

  private final RowsFetchCallback rowsFetchCallback = new RowsFetchCallback() {
    @Override
    public void onRowsFetched(List<Empire> empires) {
      adapter.addEmpires(empires);
      if (recyclerView.getVisibility() == View.GONE) {
        recyclerView.setVisibility(View.VISIBLE);
      }
    }

    @Override
    public void onBattleRankRowsFetched(List<EmpireBattleRank> ranks) {
      adapter.addBattleRanks(ranks);
      if (recyclerView.getVisibility() == View.GONE) {
        recyclerView.setVisibility(View.VISIBLE);
      }
    }
  };

  /**
   * Call this to set a static list of empires. If null, we'll revert to displaying all the empires
   * in rank order.
   */
  public void setEmpires(@Nullable List<Empire> empires) {
    adapter.setEmpires(empires);
  }

  /**
   * Clear out all existing state and refresh from scratch.
   */
  public void refresh() {
    adapter.empires.clear();
    adapter.battleRanks.clear();
    callbacks.fetchRows(0, 30, rowsFetchCallback);
  }

  public class EmpireAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private LayoutInflater layoutInflater;
    private List<Empire> empires;
    private List<EmpireBattleRank> battleRanks;
    private int firstRank;
    private boolean includeLoadingPlaceholder;

    public EmpireAdapter(RecyclerView recyclerView) {
      layoutInflater = LayoutInflater.from(recyclerView.getContext());
      includeLoadingPlaceholder = true;
      this.empires = new ArrayList<>();
      this.battleRanks = new ArrayList<>();
    }

    public void setEmpires(List<Empire> empires) {
      this.battleRanks.clear();
      this.empires = empires;
      sortEmpires();
      notifyDataSetChanged();
      includeLoadingPlaceholder = false;
    }

    public void addBattleRanks(List<EmpireBattleRank> battleRanks) {
      this.empires.clear();
      this.battleRanks.addAll(battleRanks);
      notifyDataSetChanged();
      includeLoadingPlaceholder = !battleRanks.isEmpty();
    }

    public void addEmpires(List<Empire> empires) {
      this.battleRanks.clear();
      this.empires.addAll(empires);
      sortEmpires();
      notifyDataSetChanged();
    }

    private void sortEmpires() {
      Collections.sort(empires, (lhs, rhs) -> {
        if (lhs.getRank() == null || rhs.getRank() == null) {
          return lhs.getDisplayName().compareTo(rhs.getDisplayName());
        }

        int lhsRank = lhs.getRank().getRank();
        int rhsRank = rhs.getRank().getRank();
        return lhsRank - rhsRank;
      });
    }

    @Override
    public int getItemCount() {
      int size = 0;
      if (battleRanks.size() > 0) {
        size = battleRanks.size();
      } else if (empires.size() > 0) {
        size = empires.size();
      }
      if (includeLoadingPlaceholder) {
        size += 1;
      }
      return size;
    }

    @Override
    public int getItemViewType(int position) {
      if (battleRanks.size() > 0 && position < battleRanks.size()) {
        return 0;
      } else if (empires.size() > 0 && position < empires.size()) {
        return 1;
      } else {
        return 2;
      }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      if (viewType == 0) {
        View itemView = layoutInflater.inflate(R.layout.empire_rank_list_ctrl_row, parent, false);
        return new BattleRankViewHolder(itemView);
      } if (viewType == 1) {
        View itemView = layoutInflater.inflate(R.layout.empire_rank_list_ctrl_row, parent, false);
        return new EmpireViewHolder(itemView);
      } else {
        View itemView =
            layoutInflater.inflate(R.layout.empire_rank_list_ctrl_loading, parent, false);
        return new LoadingIndicatorViewHolder(itemView);
      }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
      if (holder instanceof EmpireViewHolder) {
        Empire empire = empires.get(position);
        ((EmpireViewHolder) holder).setEmpire(empire);
      } else if (holder instanceof BattleRankViewHolder) {
        EmpireBattleRank battleRank = battleRanks.get(position);
        ((BattleRankViewHolder) holder).setBattleRank(battleRank, position);
      } else if (holder instanceof LoadingIndicatorViewHolder) {
        // This means we're near the end, let's start loading the rest of the elements.
        callbacks.fetchRows(position, 30, rowsFetchCallback);
      }
    }
  }

  private class BattleRankViewHolder extends RecyclerView.ViewHolder {
    private EmpireBattleRank battleRank;

    private View itemView;
    private TextView rankView;
    private ImageView empireIcon;
    private TextView empireName;
    private TextView lastSeen;
    private TextView totalPopulation;
    private TextView totalStars;
    private TextView totalColonies;
    private TextView totalShips;
    private TextView totalBuildings;
    private TextView allianceName;
    private ImageView allianceIcon;

    private BattleRankViewHolder(@NonNull View view){
      super(view);
      itemView = view;

      rankView = itemView.findViewById(R.id.rank);
      empireIcon = itemView.findViewById(R.id.empire_icon);
      empireName = itemView.findViewById(R.id.empire_name);
      lastSeen = itemView.findViewById(R.id.last_seen);
      totalPopulation = itemView.findViewById(R.id.total_population);
      totalStars = itemView.findViewById(R.id.total_stars);
      totalColonies = itemView.findViewById(R.id.total_colonies);
      totalShips = itemView.findViewById(R.id.total_ships);
      totalBuildings = itemView.findViewById(R.id.total_buildings);
      allianceName = itemView.findViewById(R.id.alliance_name);
      allianceIcon = itemView.findViewById(R.id.alliance_icon);

      view.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          if (battleRank != null) {
            callbacks.onEmpireClick(battleRank.getEmpire());
          }
        }
      });
    }

    public void setBattleRank(EmpireBattleRank battleRank, int position){
      this.battleRank = battleRank;
      Empire empire = battleRank.getEmpire();

      empireName.setText(empire.getDisplayName());
      empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(recyclerView.getContext(), empire));
      if (empire.getLastSeen() == null) {
        lastSeen.setText(Html.fromHtml("Last seen: <i>never</i>"));
      } else {
        lastSeen.setText(
            String.format("Last seen: %s", TimeFormatter.create().format(empire.getLastSeen())));
      }

      Alliance alliance = (Alliance) empire.getAlliance();
      if (alliance != null) {
        allianceName.setText(alliance.getName());
        allianceIcon.setImageBitmap(
            AllianceShieldManager.i.getShield(recyclerView.getContext(), alliance));
        allianceName.setVisibility(View.VISIBLE);
        allianceIcon.setVisibility(View.VISIBLE);
      } else {
        allianceName.setVisibility(View.GONE);
        allianceIcon.setVisibility(View.GONE);
      }

      DecimalFormat formatter = new DecimalFormat("#,##0");
      rankView.setText(formatter.format(position + 1));
      totalPopulation.setText(Html.fromHtml(
          String.format("Ships destroyed: <b>%s</b>",
              formatter.format(battleRank.getShipsDestroyed()))));
      totalStars.setText(Html.fromHtml(
          String.format("Colonies destroyed: <b>%s</b>",
              formatter.format(battleRank.getColoniesDestroyed()))));
      totalColonies.setText(Html.fromHtml(
          String.format("Population destroyed: <b>%s</b>",
              formatter.format(battleRank.getPopulationDestroyed()))));

      totalShips.setText("");
      totalBuildings.setText("");
    }
  }

  private class EmpireViewHolder extends RecyclerView.ViewHolder {
    private Empire empire;

    private View itemView;
    private TextView rankView;
    private ImageView empireIcon;
    private TextView empireName;
    private TextView lastSeen;
    private TextView totalPopulation;
    private TextView totalStars;
    private TextView totalColonies;
    private TextView totalShips;
    private TextView totalBuildings;
    private TextView allianceName;
    private ImageView allianceIcon;

    private EmpireViewHolder(@NonNull View view){
      super(view);
      itemView = view;

      rankView = itemView.findViewById(R.id.rank);
      empireIcon = itemView.findViewById(R.id.empire_icon);
      empireName = itemView.findViewById(R.id.empire_name);
      lastSeen = itemView.findViewById(R.id.last_seen);
      totalPopulation = itemView.findViewById(R.id.total_population);
      totalStars = itemView.findViewById(R.id.total_stars);
      totalColonies = itemView.findViewById(R.id.total_colonies);
      totalShips = itemView.findViewById(R.id.total_ships);
      totalBuildings = itemView.findViewById(R.id.total_buildings);
      allianceName = itemView.findViewById(R.id.alliance_name);
      allianceIcon = itemView.findViewById(R.id.alliance_icon);

      view.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          if (empire != null) {
            callbacks.onEmpireClick(empire);
          }
        }
      });
    }

    public void setEmpire(Empire empire){
      this.empire = empire;
      EmpireRank rank = (EmpireRank) empire.getRank();

      empireName.setText(empire.getDisplayName());
      empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(recyclerView.getContext(), empire));
      if (empire.getLastSeen() == null) {
        lastSeen.setText(Html.fromHtml("Last seen: <i>never</i>"));
      } else {
        lastSeen.setText(
            String.format("Last seen: %s", TimeFormatter.create().format(empire.getLastSeen())));
      }

      Alliance alliance = (Alliance) empire.getAlliance();
      if (alliance != null) {
        allianceName.setText(alliance.getName());
        allianceIcon.setImageBitmap(
            AllianceShieldManager.i.getShield(recyclerView.getContext(), alliance));
        allianceName.setVisibility(View.VISIBLE);
        allianceIcon.setVisibility(View.VISIBLE);
      } else {
        allianceName.setVisibility(View.GONE);
        allianceIcon.setVisibility(View.GONE);
      }

      if (rank == null) {
        rankView.setText("");
        totalPopulation.setText("");
        totalStars.setText("");
        totalColonies.setText("");
        totalShips.setText("");
        totalBuildings.setText("");
      } else {
        DecimalFormat formatter = new DecimalFormat("#,##0");
        rankView.setText(formatter.format(rank.getRank()));
        totalPopulation.setText(Html.fromHtml(
            String.format("Population: <b>%s</b>", formatter.format(rank.getTotalPopulation()))));
        totalStars.setText(Html.fromHtml(
            String.format("Stars: <b>%s</b>", formatter.format(rank.getTotalStars()))));
        totalColonies.setText(Html.fromHtml(
            String.format("Colonies: <b>%s</b>", formatter.format(rank.getTotalColonies()))));

        MyEmpire myEmpire = EmpireManager.i.getEmpire();
        if (rank.getTotalStars() > 10 || empire.getKey().equals(myEmpire.getKey())) {
          totalShips.setText(Html.fromHtml(
              String.format("Ships: <b>%s</b>", formatter.format(rank.getTotalShips()))));
          totalBuildings.setText(Html.fromHtml(
              String.format("Buildings: <b>%s</b>", formatter.format(rank.getTotalBuildings()))));
        } else {
          totalShips.setText("");
          totalBuildings.setText("");
        }
      }
    }
  }

  private class LoadingIndicatorViewHolder extends RecyclerView.ViewHolder {
    private LoadingIndicatorViewHolder(@NonNull View view){
      super(view);
    }
  }
}
