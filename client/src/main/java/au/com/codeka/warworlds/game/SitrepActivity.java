package au.com.codeka.warworlds.game;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.Notifications;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.solarsystem.SolarSystemActivity;
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
import au.com.codeka.warworlds.model.StarSummary;

public class SitrepActivity extends BaseActivity {
  private static final Log log = new Log("SitrepActivity");
  private Context context = this;
  private SituationReportAdapter situationReportAdapter;
  private String starKey;
  private Handler handler;
  private String cursor;
  private Messages.SituationReportFilter filter;
  private boolean showOldItems;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sitrep);

    situationReportAdapter = new SituationReportAdapter();

    starKey = getIntent().getStringExtra("au.com.codeka.warworlds.StarKey");
    handler = new Handler();

    final ListView reportItems = (ListView) findViewById(R.id.report_items);
    reportItems.setAdapter(situationReportAdapter);
    reportItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        SituationReport sitrep = situationReportAdapter
            .getSituationReport(position);

        Intent intent = new Intent(context, SolarSystemActivity.class);
        intent.putExtra("au.com.codeka.warworlds.StarKey", sitrep.getStarKey());

        if (sitrep.getPlanetIndex() >= 0) {
          intent.putExtra("au.com.codeka.warworlds.PlanetIndex",
              sitrep.getPlanetIndex());
        }

        SituationReport.MoveCompleteRecord mcr = sitrep.getMoveCompleteRecord();
        if (mcr != null) {
          if (mcr.getScoutReportKey() != null
              && mcr.getScoutReportKey().length() > 0) {
            // if there's a scout report, we'll want to show that
            intent.putExtra("au.com.codeka.warworlds.ShowScoutReport", true);
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
          intent.putExtra("au.com.codeka.warworlds.CombatReportKey",
              combatReportKey);
        }

        startActivity(intent);
      }
    });

    final Spinner filterSpinner = (Spinner) findViewById(R.id.filter);
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

    final Button markReadBtn = (Button) findViewById(R.id.mark_read);
    if (starKey != null) {
      markReadBtn.setVisibility(View.GONE);
    } else {
      markReadBtn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          markAsRead();
        }
      });
    }

    final CheckBox showOldItemsChk = (CheckBox) findViewById(R.id.show_read);
    showOldItemsChk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView,
              boolean isChecked) {
            showOldItems = showOldItemsChk.isChecked();
            refreshReportItems();
          }
        });

    int realmID = getIntent().getIntExtra("au.com.codeka.warworlds.RealmID", 0);
    if (realmID != 0) {
      RealmManager.i.selectRealm(realmID);
    }
  }

  @Override
  public void onResumeFragments() {
    super.onResumeFragments();

    ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
      @Override
      public void onHelloComplete(boolean success, ServerGreeting greeting) {
        if (!success) {
          startActivity(new Intent(SitrepActivity.this, WarWorldsActivity.class));
        }
        refresh();
      }
    });
  }

  @Override
  protected void onStart() {
    super.onStart();
    ShieldManager.eventBus.register(mEventHandler);
    StarManager.eventBus.register(mEventHandler);
  }

  @Override
  protected void onStop() {
    super.onStop();
    ShieldManager.eventBus.unregister(mEventHandler);
    StarManager.eventBus.unregister(mEventHandler);
  }

  private Object mEventHandler = new Object() {
    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      refreshTitle();
    }

    @EventHandler
    public void onStarUpdated(Star star) {
      if (starKey != null && star.getID() == Integer.parseInt(starKey)) {
        refreshTitle();
      }
      situationReportAdapter.notifyDataSetChanged();
    }
  };

  private void refreshTitle() {
    final TextView empireName = (TextView) findViewById(R.id.empire_name);
    final ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);

    if (starKey == null) {
      // clear all our notifications
      Notifications.clearNotifications();

      MyEmpire empire = EmpireManager.i.getEmpire();

      if (empire != null) {
        // TODO: add an "empire updated" listener here!
        empireName.setText(empire.getDisplayName());
        empireIcon
            .setImageBitmap(EmpireShieldManager.i.getShield(this, empire));
      }
    } else {
      Star star = StarManager.i.getStar(Integer.parseInt(starKey));
      if (star != null) {
        empireName.setText(star.getName());
        Sprite starSprite = StarImageManager.getInstance().getSprite(
            star, empireIcon.getWidth(), true);
        empireIcon.setImageDrawable(new SpriteDrawable(starSprite));
      }
    }

  }

  private void refreshReportItems() {
    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
    final ListView reportItems = (ListView) findViewById(R.id.report_items);

    progressBar.setVisibility(View.VISIBLE);
    reportItems.setVisibility(View.GONE);

    cursor = null;
    fetchReportItems(new FetchItemsCompleteHandler() {
      @Override
      public void onItemsFetched(List<SituationReport> items, boolean hasMore) {
        progressBar.setVisibility(View.GONE);
        reportItems.setVisibility(View.VISIBLE);

        situationReportAdapter.setItems(items, hasMore);
      }
    });
  }

  private void fetchNextReportItems() {
    fetchReportItems(new FetchItemsCompleteHandler() {
      @Override
      public void onItemsFetched(List<SituationReport> items, boolean hasMore) {
        if (items == null || items.size() == 0) {
          // if there's no more, we set the cursor to null so the adapter knows
          // there's no more
          SitrepActivity.this.cursor = null;
        }

        situationReportAdapter.appendItems(items, hasMore);
      }
    });
  }

  private void fetchReportItems(final FetchItemsCompleteHandler handler) {
    new BackgroundRunner<List<SituationReport>>() {
      private boolean mHasMore;

      @Override
      protected List<SituationReport> doInBackground() {
        String url = "sit-reports";
        if (starKey != null) {
          url = String.format("stars/%s/sit-reports", starKey);
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

          ArrayList<SituationReport> items = new ArrayList<SituationReport>();
          for (Messages.SituationReport srpb : pb.getSituationReportsList()) {
            items.add(SituationReport.fromProtocolBuffer(srpb));
          }

          // grab the cursor we'll need to fetch the next batch
          mHasMore = pb.hasCursor() && pb.getCursor() != null
              && !pb.getCursor().equals("");
          if (mHasMore) {
            log.debug("Fetched %d items, cursor=%s",
                pb.getSituationReportsCount(), pb.getCursor());
            cursor = pb.getCursor();
          } else {
            log.debug("Fetched %d items, cursor=<null>",
                pb.getSituationReportsCount());
            cursor = null;
          }

          return items;
        } catch (ApiException e) {
          log.error("Error occured fetching situation reports.", e);
          return null;
        }
      }

      @Override
      protected void onComplete(List<SituationReport> items) {
        handler.onItemsFetched(items, mHasMore);
      }
    }.execute();
  }

  private void markAsRead() {
    new BackgroundRunner<Boolean>() {
      @Override
      protected Boolean doInBackground() {
        String url = "sit-reports/read";

        try {
          ApiClient.postProtoBuf(url, null, null);
        } catch (ApiException e) {
          log.error("Error occured fetching situation reports.", e);
        }

        return true;
      }

      @Override
      protected void onComplete(Boolean success) {
        refreshReportItems();
      }
    }.execute();
  }

  private interface FetchItemsCompleteHandler {
    public void onItemsFetched(List<SituationReport> items, boolean hasMore);
  }

  private void refresh() {
    ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
      @Override
      public void onHelloComplete(boolean success, ServerGreeting greeting) {
        if (!success) {
          startActivity(new Intent(SitrepActivity.this, WarWorldsActivity.class));
        } else {
          refreshReportItems();
          refreshTitle();
        }
      }
    });
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
        LayoutInflater inflater = (LayoutInflater) getSystemService(
            Context.LAYOUT_INFLATER_SERVICE);
        if (position < items.size()) {
          view = inflater.inflate(R.layout.sitrep_row, parent, false);
        } else {
          view = inflater.inflate(R.layout.sitrep_row_loading, parent, false);
        }
      }

      if (position >= items.size()) {
        // note: once this view comes into... view, we'll want to load the next
        // lot of
        // reports
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            fetchNextReportItems();
          }
        }, 100);

        return view;
      }

      SituationReport sitrep = items.get(position);
      Star star = StarManager.i.getStar(Integer.parseInt(sitrep.getStarKey()));
      String msg = sitrep.getDescription(star);

      TextView reportTitle = (TextView) view.findViewById(R.id.report_title);
      TextView reportContent = (TextView) view
          .findViewById(R.id.report_content);
      TextView reportTime = (TextView) view.findViewById(R.id.report_time);
      ImageView starIcon = (ImageView) view.findViewById(R.id.star_icon);
      ImageView overlayIcon = (ImageView) view.findViewById(R.id.overlay_icon);

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

      Sprite designSprite = sitrep.getDesignSprite();
      if (designSprite != null) {
        overlayIcon.setImageDrawable(new SpriteDrawable(designSprite));
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

    private TextView getCommonView(int position, View convertView,
        ViewGroup parent) {
      TextView view;
      if (convertView != null) {
        view = (TextView) convertView;
      } else {
        view = new TextView(context);
        view.setGravity(Gravity.CENTER_VERTICAL);
      }

      String displayValue = filterDescriptions[position];
      view.setText(displayValue);
      return view;
    }
  }

}
