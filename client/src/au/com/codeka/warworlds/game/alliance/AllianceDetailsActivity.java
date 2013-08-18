package au.com.codeka.warworlds.game.alliance;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.com.codeka.Cash;
import au.com.codeka.common.model.BaseAllianceMember;
import au.com.codeka.common.model.BaseEmpireRank;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;

public class AllianceDetailsActivity extends BaseActivity
                                     implements AllianceManager.AllianceUpdatedHandler,
                                                EmpireManager.EmpireFetchedHandler {
    private Handler mHandler;
    private boolean mRefreshPosted;
    private int mAllianceID;
    private Alliance mAlliance;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mHandler = new Handler();

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAllianceID = Integer.parseInt(extras.getString("au.com.codeka.warworlds.AllianceKey"));
            byte[] alliance_bytes = extras.getByteArray("au.com.codeka.warworlds.Alliance");
            if (alliance_bytes != null) {
                try {
                    Messages.Alliance alliance_pb = Messages.Alliance.parseFrom(alliance_bytes);
                    mAlliance = new Alliance();
                    mAlliance.fromProtocolBuffer(alliance_pb);
                } catch (InvalidProtocolBufferException e) {
                }
            }

            AllianceManager.i.fetchAlliance(mAllianceID, new AllianceManager.FetchAllianceCompleteHandler() {
                @Override
                public void onAllianceFetched(Alliance alliance) {
                    mAlliance = alliance;
                    refreshAlliance();
                }
            });
        }

        fullRefresh();
    }

    @Override
    public void onStart() {
        super.onStart();
        AllianceManager.i.addAllianceUpdatedHandler(this);
        EmpireManager.i.addEmpireUpdatedListener(null, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        AllianceManager.i.removeAllianceUpdatedHandler(this);
        EmpireManager.i.removeEmpireUpdatedListener(this);
    }

    @Override
    public void onAllianceUpdated(Alliance alliance) {
        if (alliance.getID() == mAllianceID) {
            mAlliance = alliance;
            fullRefresh();
        }
    }

    @Override
    public void onEmpireFetched(Empire empire) {
        MyEmpire myEmpire = EmpireManager.i.getEmpire();
        if (myEmpire.getKey().equals(empire.getKey())) {
            fullRefresh();
        }
    }

    private void fullRefresh() {
        Alliance myAlliance = (Alliance) EmpireManager.i.getEmpire().getAlliance();
        if (myAlliance == null) {
            setContentView(R.layout.alliance_details_potential);
        } else if (myAlliance.getID() == mAllianceID) {
            setContentView(R.layout.alliance_details_mine);
        } else {
            setContentView(R.layout.alliance_details_enemy);
        }

        Button depositBtn = (Button) findViewById(R.id.bank_deposit);
        if (depositBtn != null) depositBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DepositRequestDialog dialog = new DepositRequestDialog();
                dialog.setAllianceID(mAllianceID);
                dialog.show(getSupportFragmentManager(), "");
            }
        });

        Button withdrawBtn = (Button) findViewById(R.id.bank_withdraw);
        if (withdrawBtn != null) withdrawBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WithdrawRequestDialog dialog = new WithdrawRequestDialog();
                dialog.setAllianceID(mAllianceID);
                dialog.show(getSupportFragmentManager(), "");
            }
        });

        Button joinBtn = (Button) findViewById(R.id.join_btn);
        if (joinBtn != null) joinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JoinRequestDialog dialog = new JoinRequestDialog();
                dialog.setAllianceID(mAllianceID);
                dialog.show(getSupportFragmentManager(), "");
            }
        });

        Button leaveBtn = (Button) findViewById(R.id.leave_btn);
        if (leaveBtn != null) leaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LeaveConfirmDialog dialog = new LeaveConfirmDialog();
                dialog.show(getSupportFragmentManager(), "");
            }
        });

        if (mAlliance != null) {
            refreshAlliance();
        }
    }

    private void refreshAlliance() {
        mRefreshPosted = false;

        TextView allianceName = (TextView) findViewById(R.id.alliance_name);
        allianceName.setText(mAlliance.getName());

        TextView bankBalance = (TextView) findViewById(R.id.bank_balance);
        if (bankBalance != null) {
            bankBalance.setText(Cash.format((float) mAlliance.getBankBalance()));
        }

        TextView allianceMembers = (TextView) findViewById(R.id.alliance_num_members);
        allianceMembers.setText(String.format("Members: %d", mAlliance.getNumMembers()));

        if (mAlliance.getMembers() != null) {
            ArrayList<Empire> members = new ArrayList<Empire>();
            ArrayList<String> missingMembers = new ArrayList<String>();
            for (BaseAllianceMember am : mAlliance.getMembers()) {
                Empire member = EmpireManager.i.getEmpire(am.getEmpireKey());
                if (member == null) {
                    missingMembers.add(am.getEmpireKey());
                } else {
                    members.add(member);
                }
            }
            LinearLayout membersList = (LinearLayout) findViewById(R.id.members);
            membersList.removeAllViews();
            populateEmpireList(membersList, members);

            if (missingMembers.size() > 0) {
                EmpireManager.i.refreshEmpires(missingMembers, new EmpireManager.EmpireFetchedHandler() {
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

    private void populateEmpireList(LinearLayout parent, List<Empire> empires) {
        Collections.sort(empires, new Comparator<Empire>() {
            @Override
            public int compare(Empire lhs, Empire rhs) {
                return lhs.getDisplayName().compareTo(rhs.getDisplayName());
            }
        });

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Empire empire = (Empire) v.getTag();

                Intent intent = new Intent(AllianceDetailsActivity.this, AllianceMemberDetailsActivity.class);
                intent.putExtra("au.com.codeka.warworlds.AllianceKey", mAlliance.getKey());
                Messages.Alliance.Builder alliance_pb = Messages.Alliance.newBuilder();
                mAlliance.toProtocolBuffer(alliance_pb);
                intent.putExtra("au.com.codeka.warworlds.Alliance", alliance_pb.build().toByteArray());
                intent.putExtra("au.com.codeka.warworlds.EmpireKey", empire.getKey());

                startActivity(intent);
            }
        };

        for (Empire empire : empires) {
            View view = inflater.inflate(R.layout.alliance_empire_row, null);
            view.setTag(empire);

            ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
            TextView empireName = (TextView) view.findViewById(R.id.empire_name);
            TextView totalStars = (TextView) view.findViewById(R.id.total_stars);
            TextView totalColonies = (TextView) view.findViewById(R.id.total_colonies);
            TextView totalShips = (TextView) view.findViewById(R.id.total_ships);
            TextView totalBuildings = (TextView) view.findViewById(R.id.total_buildings);
            view.findViewById(R.id.rank).setVisibility(View.INVISIBLE);

            DecimalFormat formatter = new DecimalFormat("#,##0");
            empireName.setText(empire.getDisplayName());
            empireIcon.setImageBitmap(empire.getShield(this));

            BaseEmpireRank rank = empire.getRank();
            if (rank != null) {
                totalStars.setText(Html.fromHtml(String.format("Stars: <b>%s</b>",
                        formatter.format(rank.getTotalStars()))));
                totalColonies.setText(Html.fromHtml(String.format("Colonies: <b>%s</b>",
                        formatter.format(rank.getTotalColonies()))));

                MyEmpire myEmpire = EmpireManager.i.getEmpire();
                if (empire.getKey().equals(myEmpire.getKey()) || rank.getTotalStars() >= 10) {
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

            // they all get the same instance...
            view.setOnClickListener(onClickListener);

            parent.addView(view);
        }
    }
}
