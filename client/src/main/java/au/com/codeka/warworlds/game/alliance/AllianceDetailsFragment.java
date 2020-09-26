package au.com.codeka.warworlds.game.alliance;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import au.com.codeka.Cash;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.BaseAllianceMember;
import au.com.codeka.common.model.BaseEmpireRank;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.TabManager;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.empire.EnemyEmpireActivity;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.AllianceShieldManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.ShieldManager;

import com.google.protobuf.InvalidProtocolBufferException;

import javax.annotation.Nonnull;

public class AllianceDetailsFragment extends Fragment implements TabManager.Reloadable {
  private Handler handler;
  private Activity activity;
  private LayoutInflater layoutInflater;
  private View view;
  private boolean refreshPosted;
  private int allianceID;
  private Alliance alliance;

  @Override
  public View onCreateView(
      @Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    layoutInflater = inflater;
    handler = new Handler();
    activity = getActivity();

    Bundle args = getArguments();
    if (args != null) {
      String key = args.getString("au.com.codeka.warworlds.AllianceKey");
      if (key != null) {
        allianceID = Integer.parseInt(key);
        byte[] alliance_bytes = args.getByteArray("au.com.codeka.warworlds.Alliance");
        if (alliance_bytes != null) {
          try {
            Messages.Alliance alliance_pb = Messages.Alliance.parseFrom(alliance_bytes);
            alliance = new Alliance();
            alliance.fromProtocolBuffer(alliance_pb);
          } catch (InvalidProtocolBufferException e) {
            // Ignore.
          }
        }
      }
    }

    if (alliance == null && allianceID == 0) {
      MyEmpire myEmpire = EmpireManager.i.getEmpire();
      alliance = (Alliance) myEmpire.getAlliance();
      if (alliance != null) {
        allianceID = alliance.getID();
        alliance = null;
      }
    }

    Alliance myAlliance = (Alliance) EmpireManager.i.getEmpire().getAlliance();
    if (myAlliance == null) {
      view = inflater.inflate(R.layout.alliance_details_potential, container, false);
    } else if (myAlliance.getID() == allianceID) {
      view = inflater.inflate(R.layout.alliance_details_mine, container, false);
    } else {
      view = inflater.inflate(R.layout.alliance_details_enemy, container, false);
    }

    AllianceManager.i.fetchAlliance(allianceID, alliance -> {
      AllianceDetailsFragment.this.alliance = alliance;
      refreshAlliance();
    });

    fullRefresh();
    return view;
  }

  @Override
  public void onStart() {
    super.onStart();
    AllianceManager.eventBus.register(mEventHandler);
    EmpireManager.eventBus.register(mEventHandler);
    ShieldManager.eventBus.register(mEventHandler);
  }

  @Override
  public void onStop() {
    super.onStop();
    AllianceManager.eventBus.unregister(mEventHandler);
    EmpireManager.eventBus.unregister(mEventHandler);
    ShieldManager.eventBus.unregister(mEventHandler);
  }

  private Object mEventHandler = new Object() {
    @EventHandler
    public void onAllianceUpdated(Alliance alliance) {
      if (alliance.getID() == allianceID) {
        AllianceDetailsFragment.this.alliance = alliance;
        fullRefresh();
      }
    }

    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      refreshAlliance();
    }

    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      MyEmpire myEmpire = EmpireManager.i.getEmpire();
      if (myEmpire.getKey().equals(empire.getKey())) {
        fullRefresh();
      } else {
        // because we send off a bunch of these at once, we can end up getting lots
        // returning at the same time. So we delay the updating for a few milliseconds
        // to ensure we don't refresh 100 times in a row...
        if (!refreshPosted) {
          refreshPosted = true;
          handler.postDelayed(() -> {
            if (alliance != null) {
              refreshAlliance();
            }
          }, 250);
        }
      }
    }
  };

  @Override
  public void reloadTab() {
    // TODO: ignore?
  }

  private void fullRefresh() {
    Button depositBtn = view.findViewById(R.id.bank_deposit);
    if (depositBtn != null) depositBtn.setOnClickListener(v -> {
      DepositRequestDialog dialog = new DepositRequestDialog();
      dialog.setAllianceID(allianceID);
      dialog.show(getParentFragmentManager(), "");
    });

    Button withdrawBtn = view.findViewById(R.id.bank_withdraw);
    if (withdrawBtn != null) withdrawBtn.setOnClickListener(v -> {
      WithdrawRequestDialog dialog = new WithdrawRequestDialog();
      dialog.setAllianceID(allianceID);
      dialog.show(getParentFragmentManager(), "");
    });

    Button changeBtn = view.findViewById(R.id.change_details_btn);
    if (changeBtn != null) {
      changeBtn.setOnClickListener(
          v -> startActivity(new Intent(activity, AllianceChangeDetailsActivity.class)));
    }

    Button joinBtn = view.findViewById(R.id.join_btn);
    if (joinBtn != null) joinBtn.setOnClickListener(v -> {
      JoinRequestDialog dialog = new JoinRequestDialog();
      dialog.setAllianceID(allianceID);
      dialog.show(getParentFragmentManager(), "");
    });

    Button leaveBtn = view.findViewById(R.id.leave_btn);
    if (leaveBtn != null) leaveBtn.setOnClickListener(v -> {
      LeaveConfirmDialog dialog = new LeaveConfirmDialog();
      dialog.show(getParentFragmentManager(), "");
    });

    if (alliance != null) {
      refreshAlliance();
    }
  }

  private void refreshAlliance() {
    if (alliance == null) {
      return;
    }
    refreshPosted = false;

    TextView allianceName = view.findViewById(R.id.alliance_name);
    allianceName.setText(alliance.getName());

    TextView allianceDescription = view.findViewById(R.id.description);
    if (alliance.getDescription() != null && !alliance.getDescription().trim().isEmpty()) {
      allianceDescription.setText(alliance.getDescription());
      allianceDescription.setVisibility(View.VISIBLE);
    } else {
      allianceDescription.setVisibility(View.GONE);
    }

    TextView bankBalance = view.findViewById(R.id.bank_balance);
    if (bankBalance != null) {
      bankBalance.setText(Cash.format(alliance.getBankBalance()));
    }

    TextView allianceMembers = view.findViewById(R.id.alliance_num_members);
    allianceMembers.setText(String.format(Locale.US, "Members: %d", alliance.getNumMembers()));

    ImageView allianceIcon = view.findViewById(R.id.alliance_icon);
    allianceIcon.setImageBitmap(AllianceShieldManager.i.getShield(activity, alliance));

    if (alliance.getMembers() != null) {
      ArrayList<Empire> members = new ArrayList<>();
      ArrayList<Integer> missingMembers = new ArrayList<>();
      for (BaseAllianceMember am : alliance.getMembers()) {
        Empire member = EmpireManager.i.getEmpire(am.getEmpireID());
        if (member == null) {
          missingMembers.add(am.getEmpireID());
        } else {
          members.add(member);
        }
      }
      LinearLayout membersList = view.findViewById(R.id.members);
      membersList.removeAllViews();
      populateEmpireList(membersList, members);

      if (missingMembers.size() > 0) {
        EmpireManager.i.refreshEmpires(missingMembers, false);
      }
    }
  }

  private void populateEmpireList(LinearLayout parent, List<Empire> empires) {
    Collections.sort(empires, (lhs, rhs) -> lhs.getDisplayName().compareTo(rhs.getDisplayName()));

    View.OnClickListener onClickListener = v -> {
      Empire empire = (Empire) v.getTag();
      if (empire.getKey().equals(EmpireManager.i.getEmpire().getKey())) {
        // don't show your own empire...
        return;
      }

      Intent intent = new Intent(activity, EnemyEmpireActivity.class);
      intent.putExtra("au.com.codeka.warworlds.EmpireKey", empire.getKey());

      startActivity(intent);
    };

    for (Empire empire : empires) {
      View view = layoutInflater.inflate(R.layout.alliance_empire_row, null);
      view.setTag(empire);

      ImageView empireIcon = view.findViewById(R.id.empire_icon);
      TextView empireName = view.findViewById(R.id.empire_name);
      TextView lastSeen = view.findViewById(R.id.last_seen);
      TextView totalStars = view.findViewById(R.id.total_stars);
      TextView totalColonies = view.findViewById(R.id.total_colonies);
      TextView totalShips = view.findViewById(R.id.total_ships);
      TextView totalBuildings = view.findViewById(R.id.total_buildings);
      view.findViewById(R.id.rank).setVisibility(View.INVISIBLE);

      DecimalFormat formatter = new DecimalFormat("#,##0");
      empireName.setText(empire.getDisplayName());
      empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(activity, empire));

      if (empire.getLastSeen() == null) {
        lastSeen.setText(Html.fromHtml("Last seen: <i>never</i>"));
      } else {
        lastSeen.setText(String.format("Last seen: %s",
            TimeFormatter.create().withMaxDays(30).format(empire.getLastSeen())));
      }

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
