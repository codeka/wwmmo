package au.com.codeka.warworlds.game.alliance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.TabFragmentActivity;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.EmpireManager;

public class AllianceActivity extends TabFragmentActivity {
    private Context mContext = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getTabManager().addTab(mContext, new TabInfo("Overview", OverviewFragment.class, null));
    }

    public static class BaseFragment extends Fragment {
    }

    public static class OverviewFragment extends BaseFragment {
        private View mView;
        private RankListAdapter mRankListAdapter;

        @Override
        public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
            mView = inflator.inflate(R.layout.alliance_overview_tab, null);
            mRankListAdapter = new RankListAdapter();

            final Button createBtn = (Button) mView.findViewById(R.id.create_alliance_btn);
            createBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onAllianceCreate();
                }
            });

            final ProgressBar progressBar = (ProgressBar) mView.findViewById(R.id.loading);
            final ListView alliancesList = (ListView) mView.findViewById(R.id.alliances);
            alliancesList.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            alliancesList.setAdapter(mRankListAdapter);

            AllianceManager.getInstance().fetchAlliances(new AllianceManager.FetchAlliancesCompleteHandler() {
                @Override
                public void onAlliancesFetched(List<Alliance> alliances) {
                    mRankListAdapter.setAlliances(alliances);

                    alliancesList.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }
            });

            alliancesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    RankListAdapter.ItemEntry item = (RankListAdapter.ItemEntry) mRankListAdapter.getItem(position);
                    if (item.alliance != null) {
                        Intent intent = new Intent(getActivity(), AllianceDetailsActivity.class);
                        intent.putExtra("au.com.codeka.warworlds.AllianceKey", item.alliance.getKey());
                        intent.putExtra("au.com.codeka.warworlds.Alliance", item.alliance);
                        getActivity().startActivity(intent);
                    }
                }
            });

            return mView;
        }

        private void onAllianceCreate() {
            AllianceCreateDialog dialog = new AllianceCreateDialog();
            dialog.show(getActivity().getSupportFragmentManager(), "");
        }

        private class RankListAdapter extends BaseAdapter {
            private ArrayList<ItemEntry> mEntries;

            public void setAlliances(List<Alliance> alliances) {
                Alliance myAlliance = EmpireManager.getInstance().getEmpire().getAlliance();
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

                Collections.sort(alliances, new Comparator<Alliance>() {
                    @Override
                    public int compare(Alliance lhs, Alliance rhs) {
                        if (lhs.getNumMembers() == rhs.getNumMembers()) {
                            return lhs.getName().compareTo(rhs.getName());
                        } else {
                            return lhs.getNumMembers() - rhs.getNumMembers();
                        }
                    }
                });

                mEntries = new ArrayList<ItemEntry>();
                if (myAlliance != null) {
                    mEntries.add(new ItemEntry(myAlliance));
                    mEntries.add(new ItemEntry(null));
                }
                for (Alliance alliance : alliances) {
                    mEntries.add(new ItemEntry(alliance));
                }

                notifyDataSetChanged();
            }

            @Override
            public int getCount() {
                if (mEntries == null)
                    return 0;
                return mEntries.size();
            }

            @Override
            public boolean isEnabled(int position) {
                if (mEntries == null)
                    return false;
                return (mEntries.get(position).alliance != null);
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
                Activity activity = getActivity();
                View view = convertView;

                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) activity.getSystemService
                            (Context.LAYOUT_INFLATER_SERVICE);
                    if (entry.alliance == null) {
                        view = new View(activity);
                        view.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 10));
                    } else {
                        view = inflater.inflate(R.layout.alliance_overview_rank_row, null);
                    }

                    if (entry.alliance != null) {
                        TextView allianceName = (TextView) view.findViewById(R.id.alliance_name);
                        allianceName.setText(entry.alliance.getName());

                        TextView allianceMembers = (TextView) view.findViewById(R.id.alliance_num_members);
                        allianceMembers.setText(String.format("Members: %d", entry.alliance.getNumMembers()));
                    }
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

}
