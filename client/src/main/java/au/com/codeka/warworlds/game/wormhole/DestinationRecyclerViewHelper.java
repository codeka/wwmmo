package au.com.codeka.warworlds.game.wormhole;

import android.graphics.Bitmap;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;

public class DestinationRecyclerViewHelper {
  public interface RowsFetchCallback {
    void onRowsFetched(List<Star> wormholes);
  }

  public interface Callbacks {
    /**
     * Called when a row is clicked on.
     * @param wormhole The {@link Star} that was clicked on.
     */
    void onWormholeClick(Star wormhole);

    /**
     * Called to fetch more rows.
     * @param startPosition The start index to fetch.
     * @param count The number of results to return.
     * @param callback A callback that you should call when fetching is complete.
     */
    void fetchRows(int startPosition, int count, RowsFetchCallback callback);
  }

  private final RecyclerView recyclerView;
  private final Star srcWormhole;
  private final Callbacks callbacks;
  private final WormholeAdapter adapter;

  public DestinationRecyclerViewHelper(
      RecyclerView recyclerView,
      Star srcWormhole,
      Callbacks callbacks) {
    this.recyclerView = recyclerView;
    this.srcWormhole = srcWormhole;
    this.callbacks = callbacks;
    this.adapter = new WormholeAdapter(recyclerView);

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
    public void onRowsFetched(List<Star> wormholes) {
      adapter.addWormholes(wormholes);
      if (recyclerView.getVisibility() == View.GONE) {
        recyclerView.setVisibility(View.VISIBLE);
      }
    }
  };

  /**
   * Clear out all existing state and refresh from scratch.
   */
  public void refresh() {
    adapter.wormholes.clear();
    callbacks.fetchRows(0, 30, rowsFetchCallback);
  }

  public class WormholeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private LayoutInflater layoutInflater;
    private List<Star> wormholes;
    private boolean includeLoadingPlaceholder;

    WormholeAdapter(RecyclerView recyclerView) {
      layoutInflater = LayoutInflater.from(recyclerView.getContext());
      includeLoadingPlaceholder = true;
      this.wormholes = new ArrayList<>();
    }

    void addWormholes(List<Star> wormholes) {
      if (wormholes.size() == 0) {
        includeLoadingPlaceholder = false;
      } else {
        this.wormholes.addAll(wormholes);
      }
      notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
      int size = wormholes.size();
      if (includeLoadingPlaceholder) {
        size += 1;
      }
      return size;
    }

    @Override
    public int getItemViewType(int position) {
      if (wormholes.size() > 0 && position < wormholes.size()) {
        return 0;
      } else {
        return 1;
      }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      if (viewType == 0) {
        View itemView =
            layoutInflater.inflate(R.layout.wormhole_destination_entry_row, parent, false);
        return new WormholeViewHolder(itemView);
      } else {
        View itemView =
            layoutInflater.inflate(R.layout.empire_rank_list_ctrl_loading, parent, false);
        return new LoadingIndicatorViewHolder(itemView);
      }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
      if (holder instanceof WormholeViewHolder) {
        Star wormhole = wormholes.get(position);
        ((WormholeViewHolder) holder).setWormhole(wormhole);
      } else if (holder instanceof LoadingIndicatorViewHolder) {
        // This means we're near the end, let's start loading the rest of the elements.
        callbacks.fetchRows(position, 30, rowsFetchCallback);
      }
    }
  }

  private class WormholeViewHolder extends RecyclerView.ViewHolder {
    private boolean isSelected;

    @Nullable
    private Star wormhole;

    private View itemView;
    private ImageView starIcon;
    private ImageView empireIcon;
    private TextView wormholeName;
    private TextView empireName;
    private TextView distance;

    private WormholeViewHolder(@NonNull View view){
      super(view);
      itemView = view;

      starIcon = itemView.findViewById(R.id.star_icon);
      empireIcon = itemView.findViewById(R.id.empire_icon);
      wormholeName = itemView.findViewById(R.id.wormhole_name);
      empireName = itemView.findViewById(R.id.empire_name);
      distance = itemView.findViewById(R.id.distance);

      view.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          if (wormhole != null) {
            callbacks.onWormholeClick(wormhole);
          }
        }
      });
    }

    public void setWormhole(@NonNull Star wormhole){
      this.wormhole = wormhole;

      Empire empire = EmpireManager.i.getEmpire(wormhole.getWormholeExtra().getEmpireID());
      if (empire == null) {
        empireIcon.setImageBitmap(null);
        empireName.setText("");
      } else {
        Bitmap bmp = EmpireShieldManager.i.getShield(itemView.getContext(), empire);
        empireIcon.setImageBitmap(bmp);
        empireName.setText(empire.getDisplayName());
      }

      Sprite starSprite = StarImageManager.getInstance().getSprite(wormhole, 20, true);
      starIcon.setImageDrawable(new SpriteDrawable(starSprite));

      wormholeName.setText(wormhole.getName());

      float distanceInPc = Sector.distanceInParsecs(srcWormhole, wormhole);
      distance.setText(String.format(Locale.ENGLISH, "%s %.1f pc", wormhole.getCoordinateString(),
          distanceInPc));

      if (isSelected) {
        itemView.setBackgroundResource(R.color.list_item_selected);
      } else {
        itemView.setBackgroundResource(android.R.color.transparent);
      }
    }
  }

  private class LoadingIndicatorViewHolder extends RecyclerView.ViewHolder {
    private LoadingIndicatorViewHolder(@NonNull View view){
      super(view);
    }
  }
}
