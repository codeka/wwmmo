package au.com.codeka.warworlds.ctrl;

import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceShieldManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireRank;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;

/**
 * Class that helpers populate a {@link androidx.recyclerview.widget.RecyclerView} for use with
 * empire ranks.
 */
public class EmpireRankRecyclerViewHelper {
  public interface RowsFetchCallback {
    void onRowsFetched(List<Empire> empires);
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
  };

  /**
   * Call this to set a static list of empires. If null, we'll revert to displaying all the empires
   * in rank order.
   */
  public void setEmpires(@Nullable List<Empire> empires) {
    adapter.setEmpires(empires);
  }

  public class EmpireAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private LayoutInflater layoutInflater;
    private List<Empire> empires;
    private int firstRank;
    private boolean includeLoadingPlaceholder;

    public EmpireAdapter(RecyclerView recyclerView) {
      layoutInflater = LayoutInflater.from(recyclerView.getContext());
      includeLoadingPlaceholder = true;
      this.empires = new ArrayList<>();
    }

    public void setEmpires(List<Empire> empires) {
      this.empires = empires;
      sortEmpires();
      notifyDataSetChanged();
    }

    public void addEmpires(List<Empire> empires) {
      this.empires.addAll(empires);
      sortEmpires();
      notifyDataSetChanged();
    }

    private void sortEmpires() {
      Collections.sort(empires, new Comparator<Empire>() {
        @Override
        public int compare(Empire lhs, Empire rhs) {
          if (lhs.getRank() == null || rhs.getRank() == null) {
            return lhs.getDisplayName().compareTo(rhs.getDisplayName());
          }

          int lhsRank = lhs.getRank().getRank();
          int rhsRank = rhs.getRank().getRank();
          return lhsRank - rhsRank;
        }
      });
    }

    @Override
    public int getItemCount() {
      return (includeLoadingPlaceholder ? 1 : 0) + empires.size();
    }

    @Override
    public int getItemViewType(int position) {
      if (!includeLoadingPlaceholder || position < empires.size()) {
        return 0;
      }
      return 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      if (viewType == 0) {
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
      } else if (holder instanceof LoadingIndicatorViewHolder) {
        // This means we're near the end, let's start loading the rest of the elements.
        int startRank = empires.get(empires.size() - 1).getRank().getRank();
        callbacks.fetchRows(startRank, startRank + 30, rowsFetchCallback);
      }
    }
  }

  private class EmpireViewHolder extends RecyclerView.ViewHolder {
    //a reference to the View
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
    }

    public void setEmpire(Empire empire){
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
