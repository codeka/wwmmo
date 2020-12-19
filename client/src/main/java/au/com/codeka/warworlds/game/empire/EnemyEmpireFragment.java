package au.com.codeka.warworlds.game.empire;

import java.text.DecimalFormat;
import java.util.Locale;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import au.com.codeka.Cash;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.BaseEmpireRank;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.alliance.KickRequestDialog;
import au.com.codeka.warworlds.game.chat.ChatFragmentArgs;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceShieldManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.ShieldManager;
import au.com.codeka.warworlds.ui.BaseFragment;

public class EnemyEmpireFragment extends BaseFragment {
  @Nullable private Empire empire;
  private View rootView;

  private EnemyEmpireFragmentArgs args;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.enemy_empire, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    args = EnemyEmpireFragmentArgs.fromBundle(requireArguments());
    rootView = view;

    Button viewBtn = view.findViewById(R.id.view_btn);
    viewBtn.setOnClickListener(v -> onEmpireViewClick());

    Button privateMsgBtn = view.findViewById(R.id.private_message_btn);
    privateMsgBtn.setOnClickListener(v -> onPrivateMessageClick());

    Button kickBtn = view.findViewById(R.id.kick_btn);
    kickBtn.setOnClickListener(v -> onKickClick());
  }

  @Override
  public void onResume() {
    super.onResume();

    ServerGreeter.waitForHello(requireActivity(), (success, greeting) -> {
      if (!success) {
        // TODO:??
      } else {
        empire = EmpireManager.i.getEmpire(args.getEmpireID());
        if (empire != null) {
          // Make sure it's update to date as well.
          EmpireManager.i.refreshEmpire(args.getEmpireID(), true);
          refresh();
        }
      }
    });
  }

  @Override
  public void onStart() {
    super.onStart();
    ShieldManager.eventBus.register(eventHandler);
    EmpireManager.eventBus.register(eventHandler);
  }

  @Override
  public void onStop() {
    super.onStop();
    ShieldManager.eventBus.unregister(eventHandler);
    EmpireManager.eventBus.unregister(eventHandler);
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      refresh();
    }

    @EventHandler
    public void onEmpireFetched(Empire empire) {
      if (args.getEmpireID() == empire.getID()) {
        EnemyEmpireFragment.this.empire = empire;
        refresh();
      }
    }
  };

  private void refresh() {
    MyEmpire myEmpire = EmpireManager.i.getEmpire();
    if (empire == null) {
      return;
    }
    TextView empireName = rootView.findViewById(R.id.empire_name);
    ImageView empireIcon = rootView.findViewById(R.id.empire_icon);

    empireName.setText(empire.getDisplayName());
    empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(requireContext(), empire));

    TextView lastSeenView = rootView.findViewById(R.id.last_seen);
    if (empire.getLastSeen() == null) {
      lastSeenView.setText(Html.fromHtml("<i>never</i>"));
    } else {
      lastSeenView.setText(TimeFormatter.create().withMaxDays(30).format(empire.getLastSeen()));
    }

    TextView tv = rootView.findViewById(R.id.private_message_btn_msg);
    tv.setText(String.format(tv.getText().toString(), empire.getDisplayName()));

    tv = rootView.findViewById(R.id.view_msg);
    tv.setText(String.format(tv.getText().toString(), empire.getDisplayName()));

    TextView allianceName = rootView.findViewById(R.id.alliance_name);
    ImageView allianceIcon = rootView.findViewById(R.id.alliance_icon);

    Alliance alliance = (Alliance) empire.getAlliance();
    if (alliance != null) {
      allianceName.setText(alliance.getName());
      allianceIcon.setImageBitmap(AllianceShieldManager.i.getShield(requireContext(), alliance));
    } else {
      allianceName.setText("");
      allianceIcon.setImageBitmap(null);
    }

    TextView totalStars = rootView.findViewById(R.id.total_stars);
    TextView totalColonies = rootView.findViewById(R.id.total_colonies);
    TextView totalShips = rootView.findViewById(R.id.total_ships);
    TextView totalBuildings = rootView.findViewById(R.id.total_buildings);
    TextView rankNumber = rootView.findViewById(R.id.rank);
    TextView taxes = rootView.findViewById(R.id.taxes);
    DecimalFormat formatter = new DecimalFormat("#,##0");

    if (empire.getTaxCollectedPerHour() != null) {
      float taxesPerHour = (float) (double) empire.getTaxCollectedPerHour();
      float taxesPerDay = taxesPerHour * 24;
      taxes.setVisibility(View.VISIBLE);
      taxes.setText(Html.fromHtml(String.format(Locale.ENGLISH,
          "Taxes: <b>%s</b> / day", Cash.format(taxesPerDay))));
    }

    BaseEmpireRank rank = empire.getRank();
    if (rank != null) {
      rankNumber.setText(formatter.format(rank.getRank()));

      totalStars.setText(Html.fromHtml(String.format("Stars: <b>%s</b>",
          formatter.format(rank.getTotalStars()))));
      totalColonies.setText(Html.fromHtml(String.format("Colonies: <b>%s</b>",
          formatter.format(rank.getTotalColonies()))));

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
      rankNumber.setText("");
      totalStars.setText("");
      totalColonies.setText("");
      totalShips.setText("");
      totalBuildings.setText("");
    }

    View horzSep = rootView.findViewById(R.id.horz_sep_4);
    TextView kickInfo = rootView.findViewById(R.id.kick_info);
    Button kickBtn = rootView.findViewById(R.id.kick_btn);
    if (empire.getAlliance() != null && myEmpire.getAlliance() != null &&
        empire.getID() != myEmpire.getID() &&
        empire.getAlliance().getKey().equals(myEmpire.getAlliance().getKey())) {
      horzSep.setVisibility(View.VISIBLE);
      kickInfo.setVisibility(View.VISIBLE);
      kickBtn.setVisibility(View.VISIBLE);
    } else {
      horzSep.setVisibility(View.GONE);
      kickInfo.setVisibility(View.GONE);
      kickBtn.setVisibility(View.GONE);
    }
  }

  private void onKickClick() {
    KickRequestDialog dialog = new KickRequestDialog();
    dialog.setup((Alliance) empire.getAlliance(), empire);
    dialog.show(getChildFragmentManager(), "");
  }

  public void onEmpireViewClick() {
    if (empire != null && empire.getHomeStar() != null) {
      BaseStar homeStar = empire.getHomeStar();
      NavHostFragment.findNavController(this).navigate(
          EnemyEmpireFragmentDirections.actionEnemyEmpireFragmentToStarfieldFragment()/* TODO: args */);
//      Intent intent = new Intent(requireContext(), StarfieldFragment.class);
//      intent.putExtra("au.com.codeka.warworlds.StarKey", homeStar.getKey());
//      intent.putExtra("au.com.codeka.warworlds.SectorX", homeStar.getSectorX());
//      intent.putExtra("au.com.codeka.warworlds.SectorY", homeStar.getSectorY());
//      intent.putExtra("au.com.codeka.warworlds.OffsetX", homeStar.getOffsetX());
//      intent.putExtra("au.com.codeka.warworlds.OffsetY", homeStar.getOffsetY());
//      startActivity(intent);
    }
  }

  public void onPrivateMessageClick() {
    if (empire != null) {
      // TODO
      NavHostFragment.findNavController(this).navigate(
          R.id.chatFragment,
          new ChatFragmentArgs.Builder().setEmpireID(args.getEmpireID()).build().toBundle());
    }
  }
}
