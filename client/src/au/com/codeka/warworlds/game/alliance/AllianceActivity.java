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
import au.com.codeka.common.model.Alliance;
import au.com.codeka.common.model.AllianceRequest;
import au.com.codeka.common.model.Empire;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.TabFragmentActivity;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.AllianceRequestHelper;
import au.com.codeka.warworlds.model.EmpireHelper;
import au.com.codeka.warworlds.model.EmpireManager;

public class AllianceActivity extends TabFragmentActivity
                              implements EmpireManager.EmpireFetchedHandler {
    private Context mContext = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getTabManager().addTab(mContext, new TabInfo(this, "Overview", OverviewFragment.class, null));
        Empire myEmpire = EmpireManager.i.getEmpire();
        if (myEmpire.alliance != null) {
            getTabManager().addTab(mContext, new TabInfo(this, "Requests", RequestsFragment.class, null));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EmpireManager.i.addEmpireUpdatedListener(null, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EmpireManager.i.removeEmpireUpdatedListener(this);
    }

    @Override
    public void onEmpireFetched(Empire empire) {
        Empire myEmpire = EmpireManager.i.getEmpire();
        if (myEmpire.key.equals(empire.key)) {
            getTabManager().reloadTab();
        }
    }

    public static class BaseFragment extends Fragment {
    }

    public static class OverviewFragment extends BaseFragment
                                         implements AllianceManager.AllianceUpdatedHandler {
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

            ListView alliancesList = (ListView) mView.findViewById(R.id.alliances);
            alliancesList.setAdapter(mRankListAdapter);
            alliancesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    RankListAdapter.ItemEntry item = (RankListAdapter.ItemEntry) mRankListAdapter.getItem(position);
                    if (item.alliance != null) {
                        Intent intent = new Intent(getActivity(), AllianceDetailsActivity.class);
                        intent.putExtra("au.com.codeka.warworlds.AllianceKey", item.alliance.key);
                        intent.putExtra("au.com.codeka.warworlds.Alliance", item.alliance.toByteArray());

                        getActivity().startActivity(intent);
                    }
                }
            });

            refresh();
            return mView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            AllianceManager.i.addAllianceUpdatedHandler(this);
        }

        @Override
        public void onDetach() {
            super.onDetach();
            AllianceManager.i.removeAllianceUpdatedHandler(this);
        }

        @Override
        public void onAllianceUpdated(Alliance alliance) {
            refresh();
        }

        private void onAllianceCreate() {
            AllianceCreateDialog dialog = new AllianceCreateDialog();
            dialog.show(getActivity().getSupportFragmentManager(), "");
        }

        private void refresh() {
            final ProgressBar progressBar = (ProgressBar) mView.findViewById(R.id.loading);
            final ListView alliancesList = (ListView) mView.findViewById(R.id.alliances);
            alliancesList.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

            AllianceManager.i.fetchAlliances(new AllianceManager.FetchAlliancesCompleteHandler() {
                @Override
                public void onAlliancesFetched(List<Alliance> alliances) {
                    mRankListAdapter.setAlliances(alliances);

                    alliancesList.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }
            });
        }

        private class RankListAdapter extends BaseAdapter {
            private ArrayList<ItemEntry> mEntries;

            public void setAlliances(List<Alliance> alliances) {
                Alliance myAlliance = (Alliance) EmpireManager.i.getEmpire().alliance;
                // remove my alliance from the list, it'll always go at the front
                if (myAlliance != null) {
                    for (int i = 0; i < alliances.size(); i++) {
                        if (alliances.get(i).key.equals(myAlliance.key)) {
                            myAlliance = alliances.get(i); // this'll ensure it's the most recent
                            alliances.remove(i);
                            break;
                        }
                    }
                }

                Collections.sort(alliances, new Comparator<Alliance>() {
                    @Override
                    public int compare(Alliance lhs, Alliance rhs) {
                        if (lhs.num_members == rhs.num_members) {
                            return lhs.name.compareTo(rhs.name);
                        } else {
                            return lhs.num_members - rhs.num_members;
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
                        allianceName.setText(entry.alliance.name);

                        TextView allianceMembers = (TextView) view.findViewById(R.id.alliance_num_members);
                        allianceMembers.setText(String.format("Members: %d", entry.alliance.num_members));
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

    public static class RequestsFragment extends BaseFragment
                                         implements AllianceManager.AllianceUpdatedHandler {
        private View mView;
        private RequestListAdapter mRequestListAdapter;
        private Alliance mAlliance;

        @Override
        public void onAllianceUpdated(Alliance alliance) {
            if (mAlliance == null || mAlliance.key.equals(alliance.key)) {
                mAlliance = alliance;
            }
            refreshRequests();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            AllianceManager.i.addAllianceUpdatedHandler(this);
        }

        @Override
        public void onDetach() {
            super.onDetach();
            AllianceManager.i.removeAllianceUpdatedHandler(this);
        }

        @Override
        public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
            mView = inflator.inflate(R.layout.alliance_requests_tab, null);
            mRequestListAdapter = new RequestListAdapter();

            ListView joinRequestsList = (ListView) mView.findViewById(R.id.join_requests);
            joinRequestsList.setAdapter(mRequestListAdapter);

            joinRequestsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    RequestListAdapter.ItemEntry entry = (RequestListAdapter.ItemEntry) mRequestListAdapter.getItem(position);
                    RequestVoteDialog dialog = new RequestVoteDialog();
                    dialog.setRequest(mAlliance, entry.request);
                    dialog.show(getActivity().getSupportFragmentManager(), "");
                }
            });

            refresh();
            return mView;
        }

        private void refresh() {
            final ProgressBar progressBar = (ProgressBar) mView.findViewById(R.id.loading);
            final ListView joinRequestsList = (ListView) mView.findViewById(R.id.join_requests);
            joinRequestsList.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

            if (mAlliance == null) {
                Empire myEmpire = EmpireManager.i.getEmpire();
                if (myEmpire != null && myEmpire.alliance != null) {
                    AllianceManager.i.fetchAlliance(Integer.parseInt(myEmpire.alliance.key), null);
                }
            } else {
                refreshRequests();
            }
        }

        private void refreshRequests() {
            final ProgressBar progressBar = (ProgressBar) mView.findViewById(R.id.loading);
            final ListView joinRequestsList = (ListView) mView.findViewById(R.id.join_requests);

            AllianceManager.i.fetchRequests(mAlliance.key,
                    new AllianceManager.FetchRequestsCompleteHandler() {
                        @Override
                        public void onRequestsFetched(Map<Integer, Empire> empires, List<AllianceRequest> requests) {
                            mRequestListAdapter.setRequests(empires, requests);

                            joinRequestsList.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                        }
                });
        }

        private class RequestListAdapter extends BaseAdapter {
            private ArrayList<ItemEntry> mEntries;

            public void setRequests(Map<Integer, Empire> empires, List<AllianceRequest> requests) {
                mEntries = new ArrayList<ItemEntry>();
                for (AllianceRequest request : requests) {
                    Empire empire;
                    if (request.target_empire_id != null) {
                        empire = empires.get(request.target_empire_id);
                    } else {
                        empire = empires.get(request.request_empire_id);
                    }
                    mEntries.add(new ItemEntry(empire, request));
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
                if (mEntries.get(position).request.state == AllianceRequest.RequestState.PENDING)
                    return true;
                return false;
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
                    view = inflater.inflate(R.layout.alliance_requests_row, null);
                }

                TextView empireName = (TextView) view.findViewById(R.id.empire_name);
                ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
                TextView requestDescription = (TextView) view.findViewById(R.id.request_description);
                ImageView requestStatus = (ImageView) view.findViewById(R.id.request_status);
                TextView requestVotes = (TextView) view.findViewById(R.id.request_votes);
                TextView message = (TextView) view.findViewById(R.id.message);

                empireName.setText(entry.empire.display_name);
                empireIcon.setImageBitmap(EmpireHelper.getShield(activity, entry.empire));
                requestDescription.setText(String.format(Locale.ENGLISH, "%s requested %s",
                        AllianceRequestHelper.getDescription(entry.request), TimeInHours.format(entry.request.request_date)));
                message.setText(entry.request.message);

                if (entry.request.state.equals(AllianceRequest.RequestState.PENDING)) {
                    requestStatus.setVisibility(View.GONE);
                    requestVotes.setVisibility(View.VISIBLE);
                    if (entry.request.votes == 0) {
                        requestVotes.setText("0");
                    } else {
                        requestVotes.setText(String.format(Locale.ENGLISH, "%s%d",
                            entry.request.votes < 0 ? "-" : "+", Math.abs(entry.request.votes)));
                    }
                } else if (entry.request.state.equals(AllianceRequest.RequestState.ACCEPTED)) {
                    requestStatus.setVisibility(View.VISIBLE);
                    requestVotes.setVisibility(View.GONE);
                    requestStatus.setImageResource(R.drawable.tick);
                } else if (entry.request.state.equals(AllianceRequest.RequestState.REJECTED)) {
                    requestStatus.setVisibility(View.VISIBLE);
                    requestVotes.setVisibility(View.GONE);
                    requestStatus.setImageResource(R.drawable.cross);
                }

                return view;
            }

            public class ItemEntry {
                public Empire empire;
                public AllianceRequest request;

                public ItemEntry(Empire empire, AllianceRequest request) {
                    this.empire = empire;
                    this.request = request;
                }
            }
        }
    }
}
