package au.com.codeka.warworlds.game.alliance;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

import au.com.codeka.common.Log;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.warworlds.ImageHelper;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.TabManager;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.AllianceRequest;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.ShieldManager;

public class RequestsFragment extends Fragment implements TabManager.Reloadable {
  private final static Log log = new Log("RequestsFragment");

  private View view;
  private RequestListAdapter requestListAdapter;
  private Alliance alliance;
  private Handler handler = new Handler();
  private String cursor;
  private boolean fetching;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    AllianceManager.eventBus.register(eventHandler);
    ShieldManager.eventBus.register(eventHandler);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    AllianceManager.eventBus.unregister(eventHandler);
    ShieldManager.eventBus.unregister(eventHandler);
  }

  @Override
  public View onCreateView(
      @Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    view = inflater.inflate(R.layout.alliance_requests_tab, container, false);
    requestListAdapter = new RequestListAdapter();

    ListView joinRequestsList = view.findViewById(R.id.join_requests);
    joinRequestsList.setAdapter(requestListAdapter);

    joinRequestsList.setOnItemClickListener((parent, view, position, id) -> {
      RequestListAdapter.ItemEntry entry =
          (RequestListAdapter.ItemEntry) requestListAdapter.getItem(position);
      RequestVoteDialog dialog = RequestVoteDialog.newInstance(alliance, entry.request);
      Activity activity = getActivity();
      if (activity != null) {
        dialog.show(getActivity().getSupportFragmentManager(), "");
      }
    });

    refresh();
    return view;
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      requestListAdapter.notifyDataSetChanged();
    }

    @EventHandler
    public void onAllianceUpdated(Alliance a) {
      if (alliance == null || alliance.getKey().equals(a.getKey())) {
        alliance = a;
      }
      refreshRequests();
    }
  };

  private void refresh() {
    final ProgressBar progressBar = view.findViewById(R.id.loading);
    final ListView joinRequestsList = view.findViewById(R.id.join_requests);
    joinRequestsList.setVisibility(View.GONE);
    progressBar.setVisibility(View.VISIBLE);

    if (alliance == null) {
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
    if (fetching) {
      return;
    }
    fetching = true;

    final ProgressBar progressBar = view.findViewById(R.id.loading);
    final ListView joinRequestsList = view.findViewById(R.id.join_requests);

    if (clear) {
      cursor = null;
      requestListAdapter.clearRequests();
    }

    AllianceManager.i.fetchRequests(Integer.parseInt(alliance.getKey()), cursor,
        (empires, requests, cursor) -> {
          fetching = false;
          this.cursor = cursor;
          requestListAdapter.appendRequests(empires, requests);

          joinRequestsList.setVisibility(View.VISIBLE);
          progressBar.setVisibility(View.GONE);
        });
  }

  private class RequestListAdapter extends BaseAdapter {
    private ArrayList<ItemEntry> entries;

    public RequestListAdapter() {
      entries = new ArrayList<>();
    }

    public void clearRequests() {
      entries = new ArrayList<>();
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
        if (empire == null) {
          log.error("Empire for %d not found!", request.getTargetEmpireID());
          continue;
        }
        entries.add(new ItemEntry(empire, request));
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
      if (entries == null)
        return 0;
      return entries.size() + (Strings.isNullOrEmpty(cursor) ? 0 : 1);
    }

    @Override
    public Object getItem(int position) {
      if (entries == null)
        return null;
      if (entries.size() <= position) {
        return null;
      }
      return entries.get(position);
    }

    @Override
    public int getItemViewType(int position) {
      if (getItem(position) == null) {
        return 1;
      } else {
        return 0;
      }
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
      return getItem(position) != null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ItemEntry entry = (ItemEntry) getItem(position);
      Activity activity = getActivity();
      if (activity == null) {
        return null;
      }
      View view = convertView;

      if (view == null) {
        LayoutInflater inflater =
            (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (entry != null) {
          view = inflater.inflate(R.layout.alliance_requests_row, parent, false);
        } else {
          view = inflater.inflate(R.layout.alliance_requests_row_loading, parent, false);
        }
      }

      if (entry == null) {
        //  once this view comes into... view, we'll want to load the next
        // lot of requests
        handler.postDelayed(RequestsFragment.this::fetchNextRequests, 100);

        return view;
      }

      TextView empireName = view.findViewById(R.id.empire_name);
      ImageView empireIcon = view.findViewById(R.id.empire_icon);
      TextView requestDescription = view.findViewById(R.id.request_description);
      ImageView requestStatus = view.findViewById(R.id.request_status);
      TextView requestVotes = view.findViewById(R.id.request_votes);
      TextView message = view.findViewById(R.id.message);
      ImageView pngImage = view.findViewById(R.id.png_image);

      if (entry.empire == null) {
        empireName.setText("...");
        empireIcon.setImageBitmap(null);
      } else {
        empireName.setText(entry.empire.getDisplayName());
        empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), entry.empire));
      }
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
              entry.request.getNumVotes() < 0
                  ? "-"
                  : "+", Math.abs(entry.request.getNumVotes())));
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
      // May be null if we have to refresh the empire.
      @Nullable
      public Empire empire;
      public AllianceRequest request;

      public ItemEntry(@Nullable Empire empire, AllianceRequest request) {
        this.empire = empire;
        this.request = request;
      }
    }
  }

  @Override
  public void reloadTab() {
  }
}
