package au.com.codeka.warworlds.game.alliance;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.com.codeka.Cash;
import au.com.codeka.common.model.BaseAllianceMember;
import au.com.codeka.common.model.BaseEmpireRank;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;

public class AllianceDetailsFragment extends Fragment
                                     implements AllianceManager.AllianceUpdatedHandler,
                                     EmpireManager.EmpireFetchedHandler,
                                     EmpireShieldManager.ShieldUpdatedHandler {
    private Handler mHandler;
    private Activity mActivity;
    private LayoutInflater mLayoutInflater;
    private View mView;
    private boolean mRefreshPosted;
    private int mAllianceID;
    private Alliance mAlliance;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mLayoutInflater = inflater;
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        mActivity = getActivity();

        Bundle args = getArguments();
        if (args != null) {
            mAllianceID = Integer.parseInt(args.getString("au.com.codeka.warworlds.AllianceKey"));
            byte[] alliance_bytes = args.getByteArray("au.com.codeka.warworlds.Alliance");
            if (alliance_bytes != null) {
                try {
                    Messages.Alliance alliance_pb = Messages.Alliance.parseFrom(alliance_bytes);
                    mAlliance = new Alliance();
                    mAlliance.fromProtocolBuffer(alliance_pb);
                } catch (InvalidProtocolBufferException e) {
                }
            }
        }

        if (mAlliance == null && mAllianceID == 0) {
            MyEmpire myEmpire = EmpireManager.i.getEmpire();
            mAlliance = (Alliance) myEmpire.getAlliance();
            if (mAlliance != null) {
                mAllianceID = mAlliance.getID();
                mAlliance = null;
            }
        }

        Alliance myAlliance = (Alliance) EmpireManager.i.getEmpire().getAlliance();
        if (myAlliance == null) {
            mView = inflater.inflate(R.layout.alliance_details_potential, null);
        } else if (myAlliance.getID() == mAllianceID) {
            mView = inflater.inflate(R.layout.alliance_details_mine, null);
        } else {
            mView = inflater.inflate(R.layout.alliance_details_enemy, null);
        }

        AllianceManager.i.fetchAlliance(mAllianceID, new AllianceManager.FetchAllianceCompleteHandler() {
            @Override
            public void onAllianceFetched(Alliance alliance) {
                mAlliance = alliance;
                refreshAlliance();
            }
        });

        fullRefresh();
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        AllianceManager.i.addAllianceUpdatedHandler(this);
        EmpireManager.i.addEmpireUpdatedListener(null, this);
        EmpireShieldManager.i.addShieldUpdatedHandler(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        AllianceManager.i.removeAllianceUpdatedHandler(this);
        EmpireManager.i.removeEmpireUpdatedListener(this);
        EmpireShieldManager.i.removeShieldUpdatedHandler(this);
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

    /** Called when an empire's shield is updated, we'll have to refresh the list. */
    @Override
    public void onShieldUpdated(int empireID) {
        refreshAlliance();
    }

    private void fullRefresh() {
        Button depositBtn = (Button) mView.findViewById(R.id.bank_deposit);
        if (depositBtn != null) depositBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DepositRequestDialog dialog = new DepositRequestDialog();
                dialog.setAllianceID(mAllianceID);
                dialog.show(getFragmentManager(), "");
            }
        });

        Button withdrawBtn = (Button) mView.findViewById(R.id.bank_withdraw);
        if (withdrawBtn != null) withdrawBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WithdrawRequestDialog dialog = new WithdrawRequestDialog();
                dialog.setAllianceID(mAllianceID);
                dialog.show(getFragmentManager(), "");
            }
        });

        Button changeBtn = (Button) mView.findViewById(R.id.change_details_btn);
        if (changeBtn != null) changeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mActivity, AllianceChangeDetailsActivity.class));
            }
        });

        Button joinBtn = (Button) mView.findViewById(R.id.join_btn);
        if (joinBtn != null) joinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JoinRequestDialog dialog = new JoinRequestDialog();
                dialog.setAllianceID(mAllianceID);
                dialog.show(getFragmentManager(), "");
            }
        });

        Button leaveBtn = (Button) mView.findViewById(R.id.leave_btn);
        if (leaveBtn != null) leaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LeaveConfirmDialog dialog = new LeaveConfirmDialog();
                dialog.show(getFragmentManager(), "");
            }
        });

        if (mAlliance != null) {
            refreshAlliance();
        }
    }

    private void refreshAlliance() {
        mRefreshPosted = false;

        TextView allianceName = (TextView) mView.findViewById(R.id.alliance_name);
        allianceName.setText(mAlliance.getName());

        TextView bankBalance = (TextView) mView.findViewById(R.id.bank_balance);
        if (bankBalance != null) {
            bankBalance.setText(Cash.format((float) mAlliance.getBankBalance()));
        }

        TextView allianceMembers = (TextView) mView.findViewById(R.id.alliance_num_members);
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
            LinearLayout membersList = (LinearLayout) mView.findViewById(R.id.members);
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

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Empire empire = (Empire) v.getTag();

                Intent intent = new Intent(mActivity, AllianceMemberDetailsActivity.class);
                intent.putExtra("au.com.codeka.warworlds.AllianceKey", mAlliance.getKey());
                Messages.Alliance.Builder alliance_pb = Messages.Alliance.newBuilder();
                mAlliance.toProtocolBuffer(alliance_pb);
                intent.putExtra("au.com.codeka.warworlds.Alliance", alliance_pb.build().toByteArray());
                intent.putExtra("au.com.codeka.warworlds.EmpireKey", empire.getKey());

                startActivity(intent);
            }
        };

        for (Empire empire : empires) {
            View view = mLayoutInflater.inflate(R.layout.alliance_empire_row, null);
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
            empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(mActivity, empire));

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
