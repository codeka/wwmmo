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
import android.os.Handler;
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
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.ImageHelper;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.TabFragmentActivity;
import au.com.codeka.warworlds.TabManager;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.AllianceRequest;
import au.com.codeka.warworlds.model.AllianceShieldManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.ShieldManager;

public class AllianceActivity extends TabFragmentActivity {
    private Context mContext = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                if (!success) {
                    startActivity(new Intent(AllianceActivity.this, WarWorldsActivity.class));
                } else {
                    MyEmpire myEmpire = EmpireManager.i.getEmpire();
                    if (myEmpire.getAlliance() != null) {
                        getTabManager().addTab(mContext, new TabInfo(AllianceActivity.this, "Overview",
                                AllianceDetailsFragment.class, null));
                    }

                    getTabManager().addTab(mContext, new TabInfo(AllianceActivity.this, "Alliances",
                            AllianceListFragment.class, null));

                    if (myEmpire.getAlliance() != null) {
                        Integer numPendingRequests = myEmpire.getAlliance().getNumPendingRequests();
                        String pending = "";
                        if (numPendingRequests != null && numPendingRequests > 0) {
                            pending = " (<font color=\"red\">" + numPendingRequests + "</font>)";
                        }
                        getTabManager().addTab(mContext, new TabInfo(AllianceActivity.this,
                                "Requests" + pending,
                                RequestsFragment.class, null));
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        EmpireManager.eventBus.register(mEventHandler);
    }

    @Override
    public void onStop() {
        super.onStop();
        EmpireManager.eventBus.unregister(mEventHandler);
    }

    private Object mEventHandler = new Object() {
        @EventHandler
        public void onEmpireUpdated(Empire empire) {
            MyEmpire myEmpire = EmpireManager.i.getEmpire();
            if (myEmpire.getID() == empire.getID()) {
                getTabManager().reloadTab();
            }
        }
    };

    public static class BaseFragment extends Fragment {
    }

    public static class AllianceListFragment extends BaseFragment
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
                        intent.putExtra("au.com.codeka.warworlds.AllianceKey", item.alliance.getKey());

                        Messages.Alliance.Builder alliance_pb = Messages.Alliance.newBuilder();
                        item.alliance.toProtocolBuffer(alliance_pb);
                        intent.putExtra("au.com.codeka.warworlds.Alliance", alliance_pb.build().toByteArray());

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
            ShieldManager.eventBus.register(mEventHandler);
        }

        @Override
        public void onDetach() {
            super.onDetach();
            AllianceManager.i.removeAllianceUpdatedHandler(this);
            ShieldManager.eventBus.unregister(mEventHandler);
        }

        @Override
        public void onAllianceUpdated(Alliance alliance) {
            refresh();
        }

        private Object mEventHandler = new Object() {
            @EventHandler
            public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
                mRankListAdapter.notifyDataSetChanged();
            }
        };

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

                ArrayList<Alliance> sorted = new ArrayList<Alliance>(alliances);
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

                mEntries = new ArrayList<ItemEntry>();
                if (myAlliance != null) {
                    mEntries.add(new ItemEntry(myAlliance));
                    mEntries.add(new ItemEntry(null));
                }
                for (Alliance alliance : sorted) {
                    mEntries.add(new ItemEntry(alliance));
                }

                notifyDataSetChanged();
            }

            @Override
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public int getItemViewType(int position) {
                ItemEntry entry = mEntries.get(position);
                return (entry.alliance == null ? 0 : 1);
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
                }

                if (entry.alliance != null) {
                    TextView allianceName = (TextView) view.findViewById(R.id.alliance_name);
                    allianceName.setText(entry.alliance.getName());

                    TextView allianceMembers = (TextView) view.findViewById(R.id.alliance_num_members);
                    allianceMembers.setText(String.format("Members: %d", entry.alliance.getNumMembers()));

                    ImageView allianceIcon = (ImageView) view.findViewById(R.id.alliance_icon);
                    allianceIcon.setImageBitmap(AllianceShieldManager.i.getShield(getActivity(), entry.alliance));
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
                                         implements AllianceManager.AllianceUpdatedHandler,
                                                    TabManager.Reloadable {
        private View mView;
        private RequestListAdapter mRequestListAdapter;
        private Alliance mAlliance;
        private Handler mHandler = new Handler();
        private String mCursor;
        private boolean mFetching;

        @Override
        public void onAllianceUpdated(Alliance alliance) {
            if (mAlliance == null || mAlliance.getKey().equals(alliance.getKey())) {
                mAlliance = alliance;
            }
            refreshRequests();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            AllianceManager.i.addAllianceUpdatedHandler(this);
            ShieldManager.eventBus.register(mEventHandler);
        }

        @Override
        public void onDetach() {
            super.onDetach();
            AllianceManager.i.removeAllianceUpdatedHandler(this);
            ShieldManager.eventBus.unregister(mEventHandler);
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

        private Object mEventHandler = new Object() {
            @EventHandler
            public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
                mRequestListAdapter.notifyDataSetChanged();
            }
        };

        private void refresh() {
            final ProgressBar progressBar = (ProgressBar) mView.findViewById(R.id.loading);
            final ListView joinRequestsList = (ListView) mView.findViewById(R.id.join_requests);
            joinRequestsList.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

            if (mAlliance == null) {
                MyEmpire myEmpire = EmpireManager.i.getEmpire();
                if (myEmpire != null && myEmpire.getAlliance() != null) {
                    AllianceManager.i.fetchAlliance(Integer.parseInt(myEmpire.getAlliance().getKey()), null);
                }
            } else {
                refreshRequests();
            }
        }

        private void refreshRequests() {
            fetchRequests(true);
        }

        private void fetchNextRequests() {
            fetchRequests(false);
        }

        private void fetchRequests(boolean clear) {
            if (mFetching) {
                return;
            }
            mFetching = true;

            final ProgressBar progressBar = (ProgressBar) mView.findViewById(R.id.loading);
            final ListView joinRequestsList = (ListView) mView.findViewById(R.id.join_requests);

            if (clear) {
                mCursor = null;
                mRequestListAdapter.clearRequests();
            }

            AllianceManager.i.fetchRequests(mAlliance.getKey(), mCursor,
                    new AllianceManager.FetchRequestsCompleteHandler() {
                        @Override
                        public void onRequestsFetched(Map<Integer, Empire> empires,
                                List<AllianceRequest> requests, String cursor) {
                            mFetching = false;
                            mCursor = cursor;
                            mRequestListAdapter.appendRequests(empires, requests);

                            joinRequestsList.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                        }
                });
        }

        private class RequestListAdapter extends BaseAdapter {
            private ArrayList<ItemEntry> mEntries;

            public RequestListAdapter() {
                mEntries = new ArrayList<ItemEntry>();
            }

            public void clearRequests() {
                mEntries = new ArrayList<ItemEntry>();
                notifyDataSetChanged();
            }

            public void appendRequests(Map<Integer, Empire> empires, List<AllianceRequest> requests) {
                for (AllianceRequest request : requests) {
                    Empire empire;
                    if (request.getTargetEmpireID() != null) {
                        empire = empires.get(request.getTargetEmpireID());
                    } else {
                        empire = empires.get(request.getRequestEmpireID());
                    }
                    mEntries.add(new ItemEntry(empire, request));
                }

                notifyDataSetChanged();
            }

            @Override
            public int getViewTypeCount() {
                // The other type is the "please wait..." at the bottom
                return 2;
            }

            @Override
            public int getCount() {
                if (mEntries == null)
                    return 0;
                return mEntries.size() + (mCursor == null ? 0 : 1);
            }

            @Override
            public Object getItem(int position) {
                if (mEntries == null)
                    return null;
                if (mEntries.size() <= position) {
                    return null;
                }
                return mEntries.get(position);
            }

            @Override
            public long getItemId(int position) {
                ItemEntry entry = (ItemEntry) getItem(position);
                if (entry == null) {
                    return 0;
                }
                return entry.request.getID();
            }

            @Override
            public boolean isEnabled(int position) {
                if (getItem(position) == null) {
                    return false;
                }

                return true;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ItemEntry entry = (ItemEntry) getItem(position);
                Activity activity = getActivity();
                View view = convertView;

                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    if (entry != null) {
                        view = inflater.inflate(R.layout.alliance_requests_row, null);
                    } else {
                        view = inflater.inflate(R.layout.alliance_requests_row_loading, null);
                    }
                }

                if (entry == null) {
                    //  once this view comes into... view, we'll want to load the next
                    // lot of requests
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            fetchNextRequests();
                        }
                    }, 100);

                    return view;
                }

                TextView empireName = (TextView) view.findViewById(R.id.empire_name);
                ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
                TextView requestDescription = (TextView) view.findViewById(R.id.request_description);
                ImageView requestStatus = (ImageView) view.findViewById(R.id.request_status);
                TextView requestVotes = (TextView) view.findViewById(R.id.request_votes);
                TextView message = (TextView) view.findViewById(R.id.message);
                ImageView pngImage = (ImageView) view.findViewById(R.id.png_image);

                empireName.setText(entry.empire.getDisplayName());
                empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), entry.empire));
                requestDescription.setText(String.format(Locale.ENGLISH, "%s requested %s",
                        entry.request.getDescription(),
                        TimeFormatter.create().format(entry.request.getRequestDate())));
                message.setText(entry.request.getMessage());

                if (entry.request.getPngImage() != null) {
                    pngImage.setVisibility(View.VISIBLE);
                    pngImage.setImageBitmap(new ImageHelper(entry.request.getPngImage()).getImage());
                } else {
                    pngImage.setVisibility(View.GONE);
                }

                if (entry.request.getState().equals(AllianceRequest.RequestState.PENDING)) {
                    requestStatus.setVisibility(View.GONE);
                    requestVotes.setVisibility(View.VISIBLE);
                    if (entry.request.getNumVotes() == 0) {
                        requestVotes.setText("0");
                    } else {
                        requestVotes.setText(String.format(Locale.ENGLISH, "%s%d",
                            entry.request.getNumVotes() < 0 ? "-" : "+", Math.abs(entry.request.getNumVotes())));
                    }
                } else if (entry.request.getState().equals(AllianceRequest.RequestState.ACCEPTED)) {
                    requestStatus.setVisibility(View.VISIBLE);
                    requestVotes.setVisibility(View.GONE);
                    requestStatus.setImageResource(R.drawable.tick);
                } else if (entry.request.getState().equals(AllianceRequest.RequestState.REJECTED)) {
                    requestStatus.setVisibility(View.VISIBLE);
                    requestVotes.setVisibility(View.GONE);
                    requestStatus.setImageResource(R.drawable.cross);
                } else if (entry.request.getState().equals(AllianceRequest.RequestState.WITHDRAWN)) {
                    requestStatus.setVisibility(View.VISIBLE);
                    requestVotes.setVisibility(View.GONE);
                    // TODO: use a different graphic
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

        @Override
        public void reloadTab() {
            // TODO Auto-generated method stub
            
        }
    }
}
