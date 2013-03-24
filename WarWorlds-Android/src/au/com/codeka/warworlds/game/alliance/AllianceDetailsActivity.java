package au.com.codeka.warworlds.game.alliance;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.AllianceMember;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireRank;
import au.com.codeka.warworlds.model.MyEmpire;

public class AllianceDetailsActivity extends BaseActivity {
    private Context mContext = this;
    private EmpireListAdapter mEmpireListAdapter;
    private Handler mHandler;
    private boolean mRefreshPosted;
    private String mAllianceKey;
    private Alliance mAlliance;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mHandler = new Handler();

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAllianceKey = extras.getString("au.com.codeka.warworlds.AllianceKey");

            Alliance myAlliance = EmpireManager.getInstance().getEmpire().getAlliance();
            if (myAlliance == null) {
                setContentView(R.layout.alliance_details_potential);
            } else if (myAlliance.getKey().equals(mAllianceKey)) {
                setContentView(R.layout.alliance_details_mine);
            } else {
                setContentView(R.layout.alliance_details_enemy);
            }

            mEmpireListAdapter = new EmpireListAdapter();
            ListView members = (ListView) findViewById(R.id.members);
            members.setAdapter(mEmpireListAdapter);

            mAlliance = (Alliance) extras.getParcelable("au.com.codeka.warworlds.Alliance");
            if (mAlliance != null) {
                refreshAlliance();
            }

            AllianceManager.getInstance().fetchAlliance(mAllianceKey, new AllianceManager.FetchAllianceCompleteHandler() {
                @Override
                public void onAllianceFetched(Alliance alliance) {
                    mAlliance = alliance;
                    refreshAlliance();
                }
            });
        }

        Button joinBtn = (Button) findViewById(R.id.join_btn);
        if (joinBtn != null) joinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JoinRequestDialog dialog = new JoinRequestDialog();
                dialog.setAllianceKey(mAllianceKey);
                dialog.show(getSupportFragmentManager(), "");
            }
        });
    }

    private void refreshAlliance() {
        mRefreshPosted = false;

        TextView allianceName = (TextView) findViewById(R.id.alliance_name);
        allianceName.setText(mAlliance.getName());

        TextView allianceMembers = (TextView) findViewById(R.id.alliance_num_members);
        allianceMembers.setText(String.format("Members: %d", mAlliance.getNumMembers()));

        if (mAlliance.getMembers() != null) {
            ArrayList<Empire> members = new ArrayList<Empire>();
            ArrayList<String> missingMembers = new ArrayList<String>();
            for (AllianceMember am : mAlliance.getMembers()) {
                Empire member = EmpireManager.getInstance().getEmpire(this, am.getEmpireKey());
                if (member == null) {
                    missingMembers.add(am.getEmpireKey());
                } else {
                    members.add(member);
                }
            }
            mEmpireListAdapter.setEmpires(members, false);

            if (missingMembers.size() > 0) {
                EmpireManager.getInstance().refreshEmpires(mContext, missingMembers, new EmpireManager.EmpireFetchedHandler() {
                    @Override
                    public void onEmpireFetched(Empire empire) {
                        if (mRefreshPosted) {
                            return;
                        }
                        mRefreshPosted = true;
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                refreshAlliance();
                            }
                        }, 250);
                    }
                });
            }
        }
    }


    private class EmpireListAdapter extends BaseAdapter {
        private ArrayList<ItemEntry> mEntries;

        public void setEmpires(List<Empire> empires, boolean addGaps) {
            mEntries = new ArrayList<ItemEntry>();

            Collections.sort(empires, new Comparator<Empire>() {
                @Override
                public int compare(Empire lhs, Empire rhs) {
                    return lhs.getDisplayName().compareTo(rhs.getDisplayName());
                }
            });

            for (Empire empire : empires) {
                mEntries.add(new ItemEntry(empire));
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
            View view = convertView;

            Activity activity = AllianceDetailsActivity.this;

            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) activity.getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                if (entry.empire == null) {
                    view = new View(activity);
                    view.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 10));
                } else {
                    view = inflater.inflate(R.layout.empire_overview_rank_row, null);
                }
            }
            if (entry.empire == null) {
                return view;
            }

            ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
            TextView empireName = (TextView) view.findViewById(R.id.empire_name);
            TextView totalStars = (TextView) view.findViewById(R.id.total_stars);
            TextView totalColonies = (TextView) view.findViewById(R.id.total_colonies);
            TextView totalShips = (TextView) view.findViewById(R.id.total_ships);
            TextView totalBuildings = (TextView) view.findViewById(R.id.total_buildings);
            view.findViewById(R.id.rank).setVisibility(View.INVISIBLE);

            DecimalFormat formatter = new DecimalFormat("#,##0");
            empireName.setText(entry.empire.getDisplayName());
            empireIcon.setImageBitmap(entry.empire.getShield(activity));

            EmpireRank rank = entry.empire.getRank();
            if (rank != null) {
                totalStars.setText(Html.fromHtml(String.format("Stars: <b>%s</b>",
                        formatter.format(rank.getTotalStars()))));
                totalColonies.setText(Html.fromHtml(String.format("Colonies: <b>%s</b>",
                        formatter.format(rank.getTotalColonies()))));

                MyEmpire myEmpire = EmpireManager.getInstance().getEmpire();
                if (entry.empire.getKey().equals(myEmpire.getKey()) || rank.getTotalStars() >= 10) {
                    totalShips.setText(Html.fromHtml(String.format("Ships: <b>%s</b>",
                           formatter.format(rank.getTotalShips()))));
                    totalBuildings.setText(Html.fromHtml(String.format("Buildings: <b>%s</b>",
                           formatter.format(rank.getTotalBuildings()))));
                } else {
                    totalShips.setText("");
                    totalBuildings.setText("");
                }
            } else {
                totalStars.setText("");
                totalColonies.setText("");
                totalShips.setText("");
                totalBuildings.setText("");
            }

            return view;
        }

        public class ItemEntry {
            public Empire empire;

            public ItemEntry(Empire empire) {
                this.empire = empire;
            }
        }
    }
}
