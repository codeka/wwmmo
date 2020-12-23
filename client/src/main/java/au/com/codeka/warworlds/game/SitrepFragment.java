package au.com.codeka.warworlds.game;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import au.com.codeka.common.Log;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.concurrency.Threads;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.solarsystem.SolarSystemFragmentArgs;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.RealmManager;
import au.com.codeka.warworlds.model.ShieldManager;
import au.com.codeka.warworlds.model.SituationReport;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.notifications.Notifications;
import au.com.codeka.warworlds.ui.BaseFragment;

public class SitrepFragment extends BaseFragment {
  private static final Log log = new Log("SitrepActivity");
  private SituationReportAdapter situationReportAdapter;
  private String cursor;
  private Messages.SituationReportFilter filter;
  private boolean showOldItems;

  private ProgressBar progressBar;
  private ListView reportItems;


  @Nullable private SitrepFragmentArgs args;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.sitrep, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    if (getArguments() != null) {
      args = SitrepFragmentArgs.fromBundle(requireArguments());
    }

    progressBar = view.findViewById(R.id.progress_bar);
    reportItems = view.findViewById(R.id.report_items);

    situationReportAdapter = new SituationReportAdapter();

    final ListView reportItems = view.findViewById(R.id.report_items);
    reportItems.setAdapter(situationReportAdapter);
    reportItems.setOnItemClickListener((parent, view1, position, id) -> {
      SituationReport sitrep = situationReportAdapter.getSituationReport(position);

      SolarSystemFragmentArgs.Builder argsBuilder =
          new SolarSystemFragmentArgs.Builder(Integer.parseInt(sitrep.getStarKey()));

      if (sitrep.getPlanetIndex() >= 0) {
        argsBuilder.setPlanetIndex(sitrep.getPlanetIndex());
      }

      SituationReport.MoveCompleteRecord mcr = sitrep.getMoveCompleteRecord();
      if (mcr != null) {
        if (mcr.getScoutReportKey() != null
            && mcr.getScoutReportKey().length() > 0) {
          // if there's a scout report, we'll want to show that
          argsBuilder.setShowScoutReport(true);
        }
      }

      String combatReportKey = null;
      SituationReport.FleetUnderAttackRecord fuar = sitrep
          .getFleetUnderAttackRecord();
      if (fuar != null) {
        combatReportKey = fuar.getCombatReportKey();
      }
      SituationReport.FleetDestroyedRecord fdr = sitrep
          .getFleetDestroyedRecord();
      if (fdr != null) {
        combatReportKey = fdr.getCombatReportKey();
      }
      SituationReport.FleetVictoriousRecord fvr = sitrep
          .getFleetVictoriousRecord();
      if (fvr != null) {
        combatReportKey = fvr.getCombatReportKey();
      }

      if (combatReportKey != null) {
        argsBuilder.setShowCombatReport(true)
            .setCombatReportID(Integer.parseInt(combatReportKey));
      }

      NavHostFragment.findNavController(this).navigate(
          R.id.solarSystemFragment, argsBuilder.build().toBundle());
    });

    final Spinner filterSpinner = view.findViewById(R.id.filter);
    filterSpinner.setAdapter(new FilterAdapter());
    filterSpinner
        .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view,
              int position, long id) {
            filter = (Messages.SituationReportFilter) filterSpinner
                .getSelectedItem();
            refreshReportItems();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {
          }
        });

    final Button markReadBtn = view.findViewById(R.id.mark_read);
    if (args != null && args.getStarID() > 0) {
      markReadBtn.setVisibility(View.GONE);
    } else {
      markReadBtn.setOnClickListener(v -> markAsRead());
    }

    final CheckBox showOldItemsChk = view.findViewById(R.id.show_read);
    showOldItemsChk.setOnCheckedChangeListener((buttonView, isChecked) -> {
      showOldItems = showOldItemsChk.isChecked();
      refreshReportItems();
    });

    if (args != null && args.getRealmID() > 0) {
      RealmManager.i.selectRealm(args.getRealmID());
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    refresh();
  }

  @Override
  public void onStart() {
    super.onStart();
    ShieldManager.eventBus.register(eventHandler);
    StarManager.eventBus.register(eventHandler);
  }

  @Override
  public void onStop() {
    super.onStop();
    ShieldManager.eventBus.unregister(eventHandler);
    StarManager.eventBus.unregister(eventHandler);
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      refreshTitle();
    }

    @EventHandler
    public void onStarUpdated(Star star) {
      if (args != null && args.getStarID() > 0 && star.getID() == args.getStarID()) {
        refreshTitle();
      }
      situationReportAdapter.notifyDataSetChanged();
    }
  };

  private void refreshTitle() {
    ActionBar actionBar = requireMainActivity().requireSupportActionBar();
    actionBar.setSubtitle("Situation Report");

    if (args == null || args.getStarID() < 0) {
      // clear all our notifications
      Notifications.clearNotifications();

      MyEmpire empire = EmpireManager.i.getEmpire();

      if (empire != null) {
        // TODO: add an "empire updated" listener here!
        actionBar.setTitle(empire.getDisplayName());
        actionBar.setIcon(
            new BitmapDrawable(
                requireContext().getResources(),
                EmpireShieldManager.i.getShield(requireContext(), empire)));
      }
    } else {
      Star star = StarManager.i.getStar(args.getStarID());
      if (star != null) {
        actionBar.setTitle(star.getName());
        Sprite starSprite = StarImageManager.getInstance().getSprite(star, 64, true);
        actionBar.setIcon(new SpriteDrawable(starSprite));
      }
    }

  }

  private void refreshReportItems() {
    progressBar.setVisibility(View.VISIBLE);
    reportItems.setVisibility(View.GONE);

    cursor = null;
    fetchReportItems((items, hasMore) -> {
      progressBar.setVisibility(View.GONE);
      reportItems.setVisibility(View.VISIBLE);

      situationReportAdapter.setItems(items, hasMore);
    });
  }

  private void fetchNextReportItems() {
    fetchReportItems((items, hasMore) -> {
      if (items == null || items.size() == 0) {
        // if there's no more, we set the cursor to null so the adapter knows
        // there's no more
        SitrepFragment.this.cursor = null;
      }

      situationReportAdapter.appendItems(items, hasMore);
    });
  }

  private void fetchReportItems(final FetchItemsCompleteHandler handler) {
    App.i.getTaskRunner().runTask(() -> {
      String url = "sit-reports";
      if (args != null && args.getStarID() > 0) {
        url = String.format(Locale.ENGLISH, "stars/%d/sit-reports", args.getStarID());
      }
      boolean hasQuery = false;
      if (cursor != null) {
        url += "?cursor=" + cursor;
        hasQuery = true;
      }
      if (filter != null
          && filter != Messages.SituationReportFilter.ShowAll) {
        if (hasQuery) {
          url += "&";
        } else {
          url += "?";
        }
        url += "filter=" + filter;
        hasQuery = true;
      }
      if (showOldItems) {
        if (hasQuery) {
          url += "&";
        } else {
          url += "?";
        }
        url += "show-old-items=1";
        hasQuery = true;
      }
      log.debug("Fetching: %s", url);

      try {
        Messages.SituationReports pb = ApiClient.getProtoBuf(url,
            Messages.SituationReports.class);

        ArrayList<SituationReport> items = new ArrayList<>();
        for (Messages.SituationReport srpb : pb.getSituationReportsList()) {
          items.add(SituationReport.fromProtocolBuffer(srpb));
        }

        // grab the cursor we'll need to fetch the next batch
        boolean hasMore = pb.hasCursor() && pb.getCursor() != null && !pb.getCursor().equals("");
        if (hasMore) {
          log.debug("Fetched %d items, cursor=%s", pb.getSituationReportsCount(), pb.getCursor());
          cursor = pb.getCursor();
        } else {
          log.debug("Fetched %d items, cursor=<null>", pb.getSituationReportsCount());
          cursor = null;
        }

        App.i.getTaskRunner().runTask(() -> handler.onItemsFetched(items, hasMore), Threads.UI);
      } catch (ApiException e) {
        log.error("Error occurred fetching situation reports.", e);
      }
    }, Threads.BACKGROUND);
  }

  private void markAsRead() {
    App.i.getTaskRunner().runTask(() -> {
      String url = "sit-reports/read";

      try {
        ApiClient.postProtoBuf(url, null, null);
      } catch (ApiException e) {
        log.error("Error occured fetching situation reports.", e);
      }
    }, Threads.BACKGROUND).then(this::refreshReportItems, Threads.UI);
  }

  private interface FetchItemsCompleteHandler {
    void onItemsFetched(List<SituationReport> items, boolean hasMore);
  }

  private void refresh() {
    refreshReportItems();
    refreshTitle();
  }

  private class SituationReportAdapter extends BaseAdapter {
    private List<SituationReport> items;
    private boolean hasMore;

    public SituationReportAdapter() {
      items = new ArrayList<>();
    }

    public SituationReport getSituationReport(int position) {
      return items.get(position);
    }

    public void setItems(List<SituationReport> items, boolean hasMore) {
      if (items == null) {
        items = new ArrayList<>();
      }

      if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
        throw new RuntimeException("Called from non-UI thread!");
      }

      this.items = items;
      this.hasMore = hasMore;
      notifyDataSetChanged();
    }

    public void appendItems(List<SituationReport> items, boolean hasMore) {
      if (items == null) {
        items = new ArrayList<>();
      }

      if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
        throw new RuntimeException("Called from non-UI thread!");
      }

      this.items.addAll(items);
      this.hasMore = hasMore;
      notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() {
      // The other type is the "please wait..." at the bottom
      return 2;
    }

    @Override
    public int getCount() {
      if (!hasMore) {
        return items.size();
      }

      return items.size() + 1;
    }

    @Override
    public Object getItem(int position) {
      if (position == items.size())
        return null;
      return items.get(position);
    }

    @Override
    public int getItemViewType(int position) {
      if (getItem(position) == null) {
        return 1;
      }
      return 0;
    }

    @Override
    public boolean isEnabled(int position) {
      if (getItem(position) == null) {
        return false;
      }

      return true;
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View view = convertView;

      if (view == null) {
        LayoutInflater inflater = (LayoutInflater) requireContext().getSystemService(
            Context.LAYOUT_INFLATER_SERVICE);
        if (position < items.size()) {
          view = inflater.inflate(R.layout.sitrep_row, parent, false);
        } else {
          view = inflater.inflate(R.layout.sitrep_row_loading, parent, false);
        }
      }

      if (position >= items.size()) {
        // note: once this view comes into... view, we'll want to load the next lot of reports.
        App.i.getTaskRunner().runTask(SitrepFragment.this::fetchNextReportItems, Threads.UI, 100);
        return view;
      }

      SituationReport sitrep = items.get(position);
      Star star = StarManager.i.getStar(Integer.parseInt(sitrep.getStarKey()));
      String msg = sitrep.getDescription(star);

      TextView reportTitle = view.findViewById(R.id.report_title);
      TextView reportContent = view.findViewById(R.id.report_content);
      TextView reportTime = view.findViewById(R.id.report_time);
      ImageView starIcon = view.findViewById(R.id.star_icon);
      ImageView overlayIcon = view.findViewById(R.id.overlay_icon);

      if (star != null) {
        int imageSize = (int) (star.getSize() * star.getStarType().getImageScale() * 2);
        Sprite starSprite = StarImageManager.getInstance().getSprite(star, imageSize, true);
        starIcon.setImageDrawable(new SpriteDrawable(starSprite));
      } else {
        starIcon.setImageBitmap(null);
      }

      reportTime.setText(TimeFormatter.create().format(sitrep.getReportTime()));
      reportContent.setText(msg);
      reportTitle.setText(sitrep.getTitle());

      Design design = sitrep.getDesign();
      if (design != null) {
        DesignHelper.setDesignIcon(design, overlayIcon);
      }

      return view;
    }
  }

  public class FilterAdapter extends BaseAdapter implements SpinnerAdapter {
    Messages.SituationReportFilter[] filters;
    String[] filterDescriptions;

    public FilterAdapter() {
      filters = new Messages.SituationReportFilter[] {
          Messages.SituationReportFilter.ShowAll,
          Messages.SituationReportFilter.MoveComplete,
          Messages.SituationReportFilter.BuildCompleteAny,
          Messages.SituationReportFilter.BuildCompleteShips,
          Messages.SituationReportFilter.BuildCompleteBuilding,
          Messages.SituationReportFilter.FleetAttacked,
          Messages.SituationReportFilter.FleetDestroyed,
          Messages.SituationReportFilter.FleetVictorious,
          Messages.SituationReportFilter.ColonyAttacked,
          Messages.SituationReportFilter.ColonyDestroyed, };
      filterDescriptions = new String[] { "Show All", "Move Complete",
          "Build Complete", "Build Complete (Ships)",
          "Build Complete (Building)", "Fleet Attacked", "Fleet Destroyed",
          "Fleet Victorious", "Colony Attacked", "Colony Destroyed", };
    }

    @Override
    public int getCount() {
      return filters.length;
    }

    @Override
    public Object getItem(int position) {
      return filters[position];
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      TextView view = getCommonView(position, convertView, parent);

      view.setTextColor(Color.WHITE);
      return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
      TextView view = getCommonView(position, convertView, parent);

      ViewGroup.LayoutParams lp = new AbsListView.LayoutParams(
          LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
      lp.height = 80;
      view.setLayoutParams(lp);
      view.setTextColor(Color.WHITE);
      view.setText("  " + view.getText().toString());
      return view;
    }

    private TextView getCommonView(int position, View convertView, ViewGroup parent) {
      TextView view;
      if (convertView != null) {
        view = (TextView) convertView;
      } else {
        view = new TextView(requireContext());
        view.setGravity(Gravity.CENTER_VERTICAL);
      }

      String displayValue = filterDescriptions[position];
      view.setText(displayValue);
      return view;
    }
  }
}
