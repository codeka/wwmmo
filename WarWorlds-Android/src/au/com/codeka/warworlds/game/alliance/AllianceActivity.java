package au.com.codeka.warworlds.game.alliance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.TimeInHours;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.TabFragmentActivity;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceJoinRequest;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;

public class AllianceActivity extends TabFragmentActivity {
    private Context mContext = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getTabManager().addTab(mContext, new TabInfo("Overview", OverviewFragment.class, null));
        getTabManager().addTab(mContext, new TabInfo("Join Requests", JoinRequestsFragment.class, null));
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

    public static class JoinRequestsFragment extends BaseFragment {
        private View mView;
        private JoinRequestListAdapter mJoinRequestListAdapter;

        @Override
        public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
            mView = inflator.inflate(R.layout.alliance_join_requests_tab, null);
            mJoinRequestListAdapter = new JoinRequestListAdapter();

            AllianceActivity activity = (AllianceActivity) getActivity();

            final ProgressBar progressBar = (ProgressBar) mView.findViewById(R.id.loading);
            final ListView joinRequestsList = (ListView) mView.findViewById(R.id.join_requests);
            joinRequestsList.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            joinRequestsList.setAdapter(mJoinRequestListAdapter);

            MyEmpire myEmpire = EmpireManager.getInstance().getEmpire();
            if (myEmpire != null && myEmpire.getAlliance() != null) {
                AllianceManager.getInstance().fetchJoinRequests(activity, myEmpire.getAlliance().getKey(),
                    new AllianceManager.FetchJoinRequestsCompleteHandler() {
                        @Override
                        public void onJoinRequestsFetched(Map<String, Empire> empires, List<AllianceJoinRequest> joinRequests) {
                            mJoinRequestListAdapter.setJoinRequests(empires, joinRequests);

                            joinRequestsList.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                        }
                });
            }

            return mView;
        }

        private class JoinRequestListAdapter extends BaseAdapter {
            private ArrayList<ItemEntry> mEntries;

            public void setJoinRequests(Map<String, Empire> empires, List<AllianceJoinRequest> joinRequests) {
                mEntries = new ArrayList<ItemEntry>();
                for (AllianceJoinRequest joinRequest : joinRequests) {
                    Empire empire = empires.get(joinRequest.getEmpireKey());
                    mEntries.add(new ItemEntry(empire, joinRequest));
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
                    LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.alliance_join_requests_row, null);
                }

                TextView empireName = (TextView) view.findViewById(R.id.empire_name);
                ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
                TextView requestDate = (TextView) view.findViewById(R.id.request_date);
                ImageView requestStatus = (ImageView) view.findViewById(R.id.request_status);
                TextView message = (TextView) view.findViewById(R.id.message);

                empireName.setText(entry.empire.getDisplayName());
                empireIcon.setImageBitmap(entry.empire.getShield(activity));
                requestDate.setText(String.format(Locale.ENGLISH, "Requested: %s", TimeInHours.format(entry.joinRequest.getTimeRequested())));
                message.setText(entry.joinRequest.getMessage());

                if (entry.joinRequest.getState().equals(AllianceJoinRequest.RequestState.PENDING)) {
                    requestStatus.setImageResource(R.drawable.question);
                } else if (entry.joinRequest.getState().equals(AllianceJoinRequest.RequestState.ACCEPTED)) {
                    requestStatus.setImageResource(R.drawable.tick);
                } else if (entry.joinRequest.getState().equals(AllianceJoinRequest.RequestState.REJECTED)) {
                    requestStatus.setImageResource(R.drawable.cross);
                }

                return view;
            }

            public class ItemEntry {
                public Empire empire;
                public AllianceJoinRequest joinRequest;

                public ItemEntry(Empire empire, AllianceJoinRequest joinRequest) {
                    this.empire = empire;
                    this.joinRequest = joinRequest;
                }
            }
        }
    }
}
