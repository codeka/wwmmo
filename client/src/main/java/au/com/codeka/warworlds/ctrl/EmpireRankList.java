package au.com.codeka.warworlds.ctrl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceShieldManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireRank;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.ShieldManager;

public class EmpireRankList extends ListView {
  private RankListAdapter mRankListAdapter;
  private Context mContext;

  public EmpireRankList(Context context, AttributeSet attrs) {
    super(context, attrs);
    if (isInEditMode()) {
      return;
    }

    mContext = context;
    mRankListAdapter = new RankListAdapter();
    setAdapter(mRankListAdapter);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (isInEditMode()) {
      return;
    }
    ShieldManager.eventBus.register(mEventHandler);
    EmpireManager.eventBus.register(mEventHandler);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (isInEditMode()) {
      return;
    }
    ShieldManager.eventBus.unregister(mEventHandler);
    EmpireManager.eventBus.unregister(mEventHandler);
  }

  public void setEmpires(List<Empire> empires, boolean addGaps) {
    mRankListAdapter.setEmpires(empires, addGaps);
  }

  public void setEmpireRanks(List<EmpireRank> ranks) {
    mRankListAdapter.setEmpireRanks(ranks, true);
  }

  public Empire getEmpireAt(int position) {
    RankListAdapter.ItemEntry entry =
        (RankListAdapter.ItemEntry) mRankListAdapter.getItem(position);
    if (entry != null) {
      return entry.getEmpire();
    }
    return null;
  }

  private Object mEventHandler = new Object() {
    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      mRankListAdapter.notifyDataSetChanged();
    }

    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      mRankListAdapter.onEmpireUpdated(empire);
    }
  };

  private class RankListAdapter extends BaseAdapter {
    private ArrayList<ItemEntry> mEntries;
    private BackgroundRunner<ArrayList<ItemEntry>> mEmpireFetcher;
    private ArrayList<ItemEntry> mWaitingFetch;

    public RankListAdapter() {
      mWaitingFetch = new ArrayList<>();
    }

    public void setEmpires(List<Empire> empires, boolean addGaps) {
      ArrayList<ItemEntry> entries = new ArrayList<>();
      for (Empire empire : empires) {
        if (empire.getRank() == null) {
          continue;
        }
        entries.add(new ItemEntry(empire));
      }

      setEntries(entries, addGaps);
    }

    public void setEmpireRanks(List<EmpireRank> ranks, boolean addGaps) {
      ArrayList<ItemEntry> entries = new ArrayList<>();
      for (EmpireRank rank : ranks) {
        entries.add(new ItemEntry(rank));
      }

      setEntries(entries, addGaps);

    }

    public void onEmpireUpdated(Empire empire) {
      if (mEntries == null) {
        return;
      }

      boolean refreshedAll = true;
      for (ItemEntry entry : mEntries) {
        if (entry.getRank() != null && entry.getRank().getEmpireKey().equals(empire.getKey())) {
          entry.setEmpire(empire);
        }
        if (entry.getEmpire() == null) {
          refreshedAll = false;
        }
      }

      // if we've fetched them all, then refresh the data set
      if (refreshedAll) {
        notifyDataSetChanged();
      }
    }

    private void setEntries(List<ItemEntry> entries, boolean addGaps) {
      mEntries = new ArrayList<>();

      Collections.sort(entries, new Comparator<ItemEntry>() {
        @Override
        public int compare(ItemEntry lhs, ItemEntry rhs) {
          if (lhs.getRank() == null || rhs.getRank() == null) {
            if (lhs.getEmpire() == null || rhs.getEmpire() == null) {
              return 0;
            } else {
              return lhs.getEmpire().getDisplayName().compareTo(rhs.getEmpire().getDisplayName());
            }
          }

          int lhsRank = lhs.getRank().getRank();
          int rhsRank = rhs.getRank().getRank();
          return lhsRank - rhsRank;
        }
      });

      int lastRank = 0;
      for (ItemEntry entry : entries) {
        if (entry.getRank() == null) {
          continue;
        }
        if (lastRank != 0 && entry.getRank().getRank() != lastRank + 1 && addGaps) {
          mEntries.add(new ItemEntry());
        }
        lastRank = entry.getRank().getRank();
        mEntries.add(entry);
      }

      notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() {
      return 2;
    }

    @Override
    public int getItemViewType(int position) {
      if (mEntries == null) {
        return 0;
      }

      if (mEntries.get(position).getEmpire() == null) {
        return 1;
      }
      return 0;
    }

    @Override
    public boolean isEnabled(int position) {
      return mEntries != null && mEntries.get(position).getEmpire() != null;
    }

    @Override
    public int getCount() {
      if (mEntries == null) {
        return 0;
      }
      return mEntries.size();
    }

    @Override
    public Object getItem(int position) {
      if (mEntries == null) {
        return null;
      }
      return mEntries.get(position);
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
        view = View.inflate(mContext, R.layout.empire_rank_list_ctrl_row, null);
      }

      TextView rankView = (TextView) view.findViewById(R.id.rank);
      ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
      TextView empireName = (TextView) view.findViewById(R.id.empire_name);
      TextView lastSeen = (TextView) view.findViewById(R.id.last_seen);
      TextView totalPopulation = (TextView) view.findViewById(R.id.total_population);
      TextView totalStars = (TextView) view.findViewById(R.id.total_stars);
      TextView totalColonies = (TextView) view.findViewById(R.id.total_colonies);
      TextView totalShips = (TextView) view.findViewById(R.id.total_ships);
      TextView totalBuildings = (TextView) view.findViewById(R.id.total_buildings);
      TextView allianceName = (TextView) view.findViewById(R.id.alliance_name);
      ImageView allianceIcon = (ImageView) view.findViewById(R.id.alliance_icon);

      Empire empire = entry.getEmpire();
      EmpireRank rank = entry.getRank();

      if (empire != null) {
        empireName.setText(empire.getDisplayName());
        empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(mContext, empire));
        if (empire.getLastSeen() == null) {
          lastSeen.setText(Html.fromHtml("Last seen: <i>never</i>"));
        } else {
          lastSeen.setText(
              String.format("Last seen: %s", TimeFormatter.create().format(empire.getLastSeen())));
        }

        Alliance alliance = (Alliance) empire.getAlliance();
        if (alliance != null) {
          allianceName.setText(alliance.getName());
          allianceIcon.setImageBitmap(AllianceShieldManager.i.getShield(mContext, alliance));
          allianceName.setVisibility(View.VISIBLE);
          allianceIcon.setVisibility(View.VISIBLE);
        } else {
          allianceName.setVisibility(View.GONE);
          allianceIcon.setVisibility(View.GONE);
        }
      } else {
        empireName.setText("???");
        empireIcon.setImageDrawable(null);
        scheduleEmpireFetch(entry);
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
        if (rank.getTotalStars() > 10 || (empire != null && empire.getKey()
            .equals(myEmpire.getKey()))) {
          totalShips.setText(Html.fromHtml(
              String.format("Ships: <b>%s</b>", formatter.format(rank.getTotalShips()))));
          totalBuildings.setText(Html.fromHtml(
              String.format("Buildings: <b>%s</b>", formatter.format(rank.getTotalBuildings()))));
        } else {
          totalShips.setText("");
          totalBuildings.setText("");
        }
      }

      return view;
    }

    private void scheduleEmpireFetch(ItemEntry entry) {
      synchronized (this) {
        mWaitingFetch.add(entry);
        if (mEmpireFetcher == null) {
          mEmpireFetcher = new BackgroundRunner<ArrayList<ItemEntry>>() {
            @Override
            protected ArrayList<ItemEntry> doInBackground() {
              // wait 150ms to see if there's any more empires to fetch
              try {
                Thread.sleep(150);
              } catch (InterruptedException e) {
                // Ignore.
              }

              ArrayList<ItemEntry> toFetch;
              synchronized (RankListAdapter.this) {
                toFetch = mWaitingFetch;
                mWaitingFetch = new ArrayList<>();
              }

              return toFetch;
            }

            @Override
            protected void onComplete(final ArrayList<ItemEntry> toFetch) {
              ArrayList<Integer> empireIDs = new ArrayList<>();
              for (ItemEntry entry : toFetch) {
                if (entry.getRank() != null) {
                  empireIDs.add(entry.getRank().getEmpireID());
                }
              }

              EmpireManager.i.refreshEmpires(empireIDs);

              mEmpireFetcher = null;
            }
          };
          mEmpireFetcher.execute();
        }
      }
    }

    public class ItemEntry {
      @Nullable private Empire empire;
      @Nullable private EmpireRank rank;

      public ItemEntry() {
      }

      public ItemEntry(Empire empire) {
        this.empire = Preconditions.checkNotNull(empire);
        this.rank = Preconditions.checkNotNull((EmpireRank) empire.getRank());
      }

      public ItemEntry(EmpireRank rank) {
        this.rank = Preconditions.checkNotNull(rank);
      }

      @Nullable public Empire getEmpire() {
        return empire;
      }

      public void setEmpire(@Nonnull Empire empire) {
        this.empire = Preconditions.checkNotNull(empire);
      }

      @Nullable public EmpireRank getRank() {
        return rank;
      }
    }
  }
}
