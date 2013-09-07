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
import au.com.codeka.common.model.Empire;
import au.com.codeka.common.model.EmpireRank;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.EmpireHelper;
import au.com.codeka.warworlds.model.EmpireManager;

public class EmpireRankList extends ListView {
    private RankListAdapter mRankListAdapter;
    private Context mContext;

    public EmpireRankList(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mRankListAdapter = new RankListAdapter();
        setAdapter(mRankListAdapter);
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
                if (empire.rank == null) {
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
                            return lhs.empire.display_name.compareTo(rhs.empire.display_name);
                        }
                    }

                    int lhsRank = lhs.rank.rank;
                    int rhsRank = rhs.rank.rank;
                    return lhsRank - rhsRank;
                }
            });

            int lastRank = 0;
            for (ItemEntry entry : entries) {
                if (lastRank != 0 && entry.rank.rank != lastRank + 1 && addGaps) {
                    mEntries.add(new ItemEntry());
                }
                lastRank = entry.rank.rank;
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

            Empire empire = entry.empire;
            EmpireRank rank = entry.rank;

            if (empire != null) {
                empireName.setText(empire.display_name);
                empireIcon.setImageBitmap(EmpireHelper.getShield(mContext, empire));
            } else {
                empireName.setText("???");
                empireIcon.setImageDrawable(null);
                scheduleEmpireFetch(entry);
            }

            DecimalFormat formatter = new DecimalFormat("#,##0");
            rankView.setText(formatter.format(rank.rank));
            totalPopulation.setText(Html.fromHtml(String.format("Population: <b>%s</b>",
                    formatter.format(rank.total_population))));
            totalStars.setText(Html.fromHtml(String.format("Stars: <b>%s</b>",
                    formatter.format(rank.total_stars))));
            totalColonies.setText(Html.fromHtml(String.format("Colonies: <b>%s</b>",
                    formatter.format(rank.total_colonies))));

            Empire myEmpire = EmpireManager.i.getEmpire();
            if (rank.total_stars > 10 || 
                    (empire != null && empire.key.equals(myEmpire.key))) {
                totalShips.setText(Html.fromHtml(String.format("Ships: <b>%s</b>",
                       formatter.format(rank.total_ships))));
                totalBuildings.setText(Html.fromHtml(String.format("Buildings: <b>%s</b>",
                       formatter.format(rank.total_buildings))));
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
                                empireKeys.add(entry.rank.empire_key);
                            }

                            EmpireManager.i.fetchEmpires(empireKeys, new EmpireManager.EmpireFetchedHandler() {
                                @Override
                                public void onEmpireFetched(Empire empire) {
                                    boolean refreshedAll = true;
                                    for (ItemEntry entry : toFetch) {
                                        if (entry.rank.empire_key.equals(empire.key)) {
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
                this.rank = (EmpireRank) empire.rank;
            }
            public ItemEntry(EmpireRank rank) {
                this.rank = rank;
            }
        }
    }
}
