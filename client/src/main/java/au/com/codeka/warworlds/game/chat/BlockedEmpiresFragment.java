package au.com.codeka.warworlds.game.chat;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.joda.time.DateTime;

import java.util.ArrayList;

import au.com.codeka.common.Log;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceShieldManager;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.ui.BaseFragment;

public class BlockedEmpiresFragment extends BaseFragment {
  private static final Log log = new Log("BlockedEmpiresActivity");

  private EmpireAdapter adapter;

  private View loading;
  private View noEmpiresView;
  private ListView empiresList;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.chat_blocked, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    loading = view.findViewById(R.id.loading);
    noEmpiresView = view.findViewById(R.id.no_empires_msg);
    empiresList = view.findViewById(R.id.empires_list);

    adapter = new EmpireAdapter();
    empiresList.setAdapter(adapter);

    empiresList.setOnItemClickListener((parent, view1, position, id) -> {
      final Empire empire = adapter.getEmpire(position);

      new StyledDialog.Builder(requireContext())
          .setTitle("Unblock empire")
          .setMessage("Do you want to unblock " + empire.getDisplayName() + "? You will"
              + " see all messages they have posted in the past as well. You can always block"
              + " then again later if you want.")
          .setPositiveButton("Unblock", (dialog, which) -> {
            doUnblock(empire);
            dialog.dismiss();
          }).setNegativeButton("Cancel", null)
          .create().show();
    });
  }

  private void doUnblock(final Empire empire) {
    ChatManager.i.unblockEmpire(requireContext(), empire, this::refresh);
  }

  @Override
  public void onResume() {
    super.onResume();

    refresh();
  }

  private void refresh() {
    loading.setVisibility(View.VISIBLE);
    noEmpiresView.setVisibility(View.GONE);
    empiresList.setVisibility(View.GONE);

    RequestManager.i.sendRequest(new ApiRequest.Builder("chat/blocks", "GET")
        .completeCallback(request -> {
          loading.setVisibility(View.GONE);

          Messages.ChatBlocks chatBlocks = request.body(Messages.ChatBlocks.class);
          if (chatBlocks == null || chatBlocks.getBlockedEmpireCount() == 0) {
            noEmpiresView.setVisibility(View.VISIBLE);
          } else {
            adapter.refresh(chatBlocks);
            empiresList.setVisibility(View.VISIBLE);
          }
        })
        .build());
  }

  public class EmpireAdapter extends BaseAdapter {
    private final ArrayList<Entry> rows = new ArrayList<>();

    void refresh(Messages.ChatBlocks chatBlocks) {
      rows.clear();
      for (Messages.ChatBlockedEmpire blockedEmpire : chatBlocks.getBlockedEmpireList()) {
        Empire empire = new Empire();
        empire.fromProtocolBuffer(blockedEmpire.getEmpire());

        rows.add(new Entry(empire, new DateTime(blockedEmpire.getBlockTime() * 1000)));
      }

      notifyDataSetChanged();
    }

    @Override
    public int getCount() {
      log.info("getCount() returning: %d", rows.size());
      return rows.size();
    }

    @Override
    public Object getItem(int position) {
      return rows.get(position);
    }

    public Empire getEmpire(int position) {
      return rows.get(position).empire;
    }

    @Override
    public long getItemId(int position) {
      return rows.get(position).empire.getID();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View view = convertView;
      if (view == null) {
        view =
            LayoutInflater.from(requireContext()).inflate(R.layout.chat_blocked_row, parent, false);
      }

      ImageView empireIcon = view.findViewById(R.id.empire_icon);
      TextView empireName = view.findViewById(R.id.empire_name);
      TextView lastSeen = view.findViewById(R.id.last_seen);
      TextView allianceName = view.findViewById(R.id.alliance_name);
      ImageView allianceIcon = view.findViewById(R.id.alliance_icon);

      Empire empire = rows.get(position).empire;
      empireName.setText(empire.getDisplayName());
      empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(requireContext(), empire));
      if (empire.getLastSeen() == null) {
        lastSeen.setText(Html.fromHtml("Last seen: <i>never</i>"));
      } else {
        lastSeen.setText(
            String.format("Last seen: %s", TimeFormatter.create().format(empire.getLastSeen())));
      }

      Alliance alliance = (Alliance) empire.getAlliance();
      if (alliance != null) {
        allianceName.setText(alliance.getName());
        allianceIcon.setImageBitmap(AllianceShieldManager.i.getShield(requireContext(), alliance));
        allianceName.setVisibility(View.VISIBLE);
        allianceIcon.setVisibility(View.VISIBLE);
      } else {
        allianceName.setVisibility(View.GONE);
        allianceIcon.setVisibility(View.GONE);
      }

      TextView blockedSince = view.findViewById(R.id.blocked_since);
      blockedSince.setText(
          String.format(
              "Blocked: %s",
              TimeFormatter.create().format(rows.get(position).blockedTime)));

      return view;
    }
  }

  private static final class Entry {
    Empire empire;
    DateTime blockedTime;

    Entry(Empire empire, DateTime blockedTime) {
      this.empire = empire;
      this.blockedTime = blockedTime;
    }
  }
}
