package au.com.codeka.warworlds.ctrl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.BackgroundRunner;
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
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (isInEditMode()) {
            return;
        }
        ShieldManager.eventBus.unregister(mEventHandler);
    }

    public void setEmpires(List<Empire> empires, boolean addGaps) {
        mRankListAdapter.setEmpires(empires, addGaps);
    }

    public void setEmpireRanks(List<EmpireRank> ranks) {
        mRankListAdapter.setEmpireRanks(ranks, true);
    }

    public Empire getEmpireAt(int position) {
        RankListAdapter.ItemEntry entry = (RankListAdapter.ItemEntry) mRankListAdapter.getItem(position);
        if (entry != null) {
            return entry.empire;
        }
        return null;
    }

    private Object mEventHandler = new Object() {
        @EventHandler
        public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
            mRankListAdapter.notifyDataSetChanged();
        }
    };

    private class RankListAdapter extends BaseAdapter {
        private ArrayList<ItemEntry> mEntries;
        private BackgroundRunner<ArrayList<ItemEntry>> mEmpireFetcher;
        private ArrayList<ItemEntry> mWaitingFetch;

        public RankListAdapter() {
            mWaitingFetch = new ArrayList<ItemEntry>();
        }

        public void setEmpires(List<Empire> empires, boolean addGaps) {
            ArrayList<ItemEntry> entries = new ArrayList<ItemEntry>();
            for (Empire empire : empires) {
                if (empire.getRank() == null) {
                    continue;
                }
                entries.add(new ItemEntry(empire));
            }

            setEntries(entries, addGaps);
        }

        public void setEmpireRanks(List<EmpireRank> ranks, boolean addGaps) {
            ArrayList<ItemEntry> entries = new ArrayList<ItemEntry>();
            for (EmpireRank rank : ranks) {
                entries.add(new ItemEntry(rank));
            }

            setEntries(entries, addGaps);

        }

        private void setEntries(List<ItemEntry> entries, boolean addGaps) {
            mEntries = new ArrayList<ItemEntry>();

            Collections.sort(entries, new Comparator<ItemEntry>() {
                @Override
                public int compare(ItemEntry lhs, ItemEntry rhs) {
                    // should never happen, but just in case...
                    if (lhs.rank == null || rhs.rank == null) {
                        if (lhs.empire == null || rhs.empire == null) {
                            return 0;
                        } else {
                            return lhs.empire.getDisplayName().compareTo(rhs.empire.getDisplayName());
                        }
                    }

                    int lhsRank = lhs.rank.getRank();
                    int rhsRank = rhs.rank.getRank();
                    return lhsRank - rhsRank;
                }
            });

            int lastRank = 0;
            for (ItemEntry entry : entries) {
                if (lastRank != 0 && entry.rank.getRank() != lastRank + 1 && addGaps) {
                    mEntries.add(new ItemEntry());
                }
                lastRank = entry.rank.getRank();
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
            if (mEntries == null)
                return 0;

            if (mEntries.get(position).empire == null)
                return 1;
            return 0;
        }

        @Override
        public boolean isEnabled(int position) {
            if (mEntries == null)
                return false;
            if (mEntries.get(position).empire == null)
                return false;
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
                if (entry.rank == null) {
                    view = new View(mContext);
                    view.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 10));
                } else {
                    view = inflater.inflate(R.layout.empire_rank_list_ctrl_row, null);
                }
            }
            if (entry.rank == null) {
                return view;
            }

            TextView rankView = (TextView) view.findViewById(R.id.rank);
            ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
            TextView empireName = (TextView) view.findViewById(R.id.empire_name);
            TextView totalPopulation = (TextView) view.findViewById(R.id.total_population);
            TextView totalStars = (TextView) view.findViewById(R.id.total_stars);
            TextView totalColonies = (TextView) view.findViewById(R.id.total_colonies);
            TextView totalShips = (TextView) view.findViewById(R.id.total_ships);
            TextView totalBuildings = (TextView) view.findViewById(R.id.total_buildings);
            TextView allianceName = (TextView) view.findViewById(R.id.alliance_name);
            ImageView allianceIcon = (ImageView) view.findViewById(R.id.alliance_icon);

            Empire empire = entry.empire;
            EmpireRank rank = entry.rank;

            if (empire != null) {
                empireName.setText(empire.getDisplayName());
                empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(mContext, empire));

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

            DecimalFormat formatter = new DecimalFormat("#,##0");
            rankView.setText(formatter.format(rank.getRank()));
            totalPopulation.setText(Html.fromHtml(String.format("Population: <b>%s</b>",
                    formatter.format(rank.getTotalPopulation()))));
            totalStars.setText(Html.fromHtml(String.format("Stars: <b>%s</b>",
                    formatter.format(rank.getTotalStars()))));
            totalColonies.setText(Html.fromHtml(String.format("Colonies: <b>%s</b>",
                    formatter.format(rank.getTotalColonies()))));

            MyEmpire myEmpire = EmpireManager.i.getEmpire();
            if (rank.getTotalStars() > 10 || 
                    (empire != null && empire.getKey().equals(myEmpire.getKey()))) {
                totalShips.setText(Html.fromHtml(String.format("Ships: <b>%s</b>",
                       formatter.format(rank.getTotalShips()))));
                totalBuildings.setText(Html.fromHtml(String.format("Buildings: <b>%s</b>",
                       formatter.format(rank.getTotalBuildings()))));
            } else {
                totalShips.setText("");
                totalBuildings.setText("");
            }

            return view;
        }

        private void scheduleEmpireFetch(ItemEntry entry) {
            synchronized(this) {
                mWaitingFetch.add(entry);
                if (mEmpireFetcher == null) {
                    mEmpireFetcher = new BackgroundRunner<ArrayList<ItemEntry>>() {
                        @Override
                        protected ArrayList<ItemEntry> doInBackground() {
                            // wait 150ms to see if there's any more empires to fetch
                            try {
                                Thread.sleep(150);
                            } catch (InterruptedException e) {
                            }

                            ArrayList<ItemEntry> toFetch;
                            synchronized(RankListAdapter.this) {
                                toFetch = mWaitingFetch;
                                mWaitingFetch = new ArrayList<ItemEntry>();
                            }

                            return toFetch;
                        }

                        @Override
                        protected void onComplete(final ArrayList<ItemEntry> toFetch) {
                            ArrayList<String> empireKeys = new ArrayList<String>();
                            for (ItemEntry entry : toFetch) {
                                empireKeys.add(entry.rank.getEmpireKey());
                            }

                            EmpireManager.i.fetchEmpires(empireKeys, new EmpireManager.EmpireFetchedHandler() {
                                @Override
                                public void onEmpireFetched(Empire empire) {
                                    boolean refreshedAll = true;
                                    for (ItemEntry entry : toFetch) {
                                        if (entry.rank.getEmpireKey().equals(empire.getKey())) {
                                            entry.empire = empire;
                                        }
                                        if (entry.empire == null) {
                                            refreshedAll = false;
                                        }
                                    }

                                    // if we've fetched them all, then refresh the data set
                                    if (refreshedAll) {
                                        notifyDataSetChanged();
                                    }
                                }
                            });

                            mEmpireFetcher = null;
                        }
                    };
                    mEmpireFetcher.execute();
                }
            }
        }

        public class ItemEntry {
            public Empire empire;
            public EmpireRank rank;

            public ItemEntry() {
            }
            public ItemEntry(Empire empire) {
                this.empire = empire;
                this.rank = (EmpireRank) empire.getRank();
            }
            public ItemEntry(EmpireRank rank) {
                this.rank = rank;
            }
        }
    }
}
