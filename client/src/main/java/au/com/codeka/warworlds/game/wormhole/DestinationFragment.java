package au.com.codeka.warworlds.game.wormhole;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.concurrency.Threads;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.ui.BaseFragment;

public class DestinationFragment extends BaseFragment {
  private static final Log log = new Log("DestinationActivity");
  private Star srcWormhole;
  private Star destWormhole;
  private DestinationRecyclerViewHelper recyclerViewHelper;

  private EditText search;

  private int searchTextChangeCount;
  private boolean searchTextChangePosted;
  private final Handler handler = new Handler();
  private static final int SEARCH_DELAY_MS = 500;

  @Nullable private String searchQuery;

  private Star getSrcWormhole() {
    if (srcWormhole == null) {
      Bundle args = requireArguments();
      srcWormhole = new Star();
      try {
        Messages.Star starMsg = Messages.Star.parseFrom(args.getByteArray("srcWormhole"));
        srcWormhole.fromProtocolBuffer(starMsg);
      } catch (InvalidProtocolBufferException e) {
        log.error("Failed to load srcWormhole from Protocol Buffer", e);
      }
    }

    return srcWormhole;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.wormhole_destination, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    final View progressBar = view.findViewById(R.id.progress_bar);
    final RecyclerView wormholes = view.findViewById(R.id.wormholes);
    final View noWormholesMsg = view.findViewById(R.id.no_wormholes_msg);
    final TextView tuneTime = view.findViewById(R.id.tune_time);
    search = view.findViewById(R.id.search);

    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    recyclerViewHelper = new DestinationRecyclerViewHelper(
        wormholes,
        getSrcWormhole(),
        new DestinationRecyclerViewHelper.Callbacks() {
      @Override
      public void onWormholeClick(Star wormhole) {
        destWormhole = wormhole;
        view.findViewById(R.id.tune_btn).setEnabled(true);
      }

      @Override
      public void fetchRows(
          final int startPosition,
          final int count,
          final DestinationRecyclerViewHelper.RowsFetchCallback callback) {
        if (myEmpire.getAlliance() != null) {
          AllianceManager.i.fetchWormholes(
              Integer.parseInt(myEmpire.getAlliance().getKey()),
              startPosition,
              count,
              searchQuery,
              wormholes1 -> {
                final View progressBar1 = view.findViewById(R.id.progress_bar);
                final RecyclerView wormholesList = view.findViewById(R.id.wormholes);
                final View noWormholesMsg1 = view.findViewById(R.id.no_wormholes_msg);

                // Remove the current wormhole, since obviously you can't tune to that.
                for (int i = 0; i < wormholes1.size(); i++) {
                  if (wormholes1.get(i).getID() == getSrcWormhole().getID()) {
                    wormholes1.remove(i);
                    break;
                  }
                }

                progressBar1.setVisibility(View.GONE);
                if (startPosition == 0 && wormholes1.isEmpty()) {
                  wormholesList.setVisibility(View.GONE);
                  noWormholesMsg1.setVisibility(View.VISIBLE);
                } else {
                  wormholesList.setVisibility(View.VISIBLE);
                  noWormholesMsg1.setVisibility(View.GONE);
                  tuneTime.setVisibility(View.VISIBLE);
                }

                callback.onRowsFetched(wormholes1);
              });
        } else {
          // TODO: support wormholes in your own empire at least...
        }
      }
    });

    progressBar.setVisibility(View.VISIBLE);
    wormholes.setVisibility(View.GONE);
    tuneTime.setVisibility(View.GONE);
    noWormholesMsg.setVisibility(View.GONE);

    TextView starName = view.findViewById(R.id.star_name);
    starName.setText(getSrcWormhole().getName());

    ImageView starIcon = view.findViewById(R.id.star_icon);
    Sprite starSprite = StarImageManager.getInstance().getSprite(getSrcWormhole(), 60, true);
    starIcon.setImageDrawable(new SpriteDrawable(starSprite));

    int tuneTimeHours =
        getSrcWormhole().getWormholeExtra() == null
            ? 0
            : getSrcWormhole().getWormholeExtra().getTuneTimeHours();
    tuneTime.setText(
        String.format(
            Locale.ENGLISH,
            "Tune time: %d hr%s",
            tuneTimeHours,
            tuneTimeHours == 1 ? "" : "s"));

    final EditText search = view.findViewById(R.id.search);
    search.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int before, int count) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int count, int after) {
        searchTextChangeCount ++;
        if (!searchTextChangePosted) {
          handler.postDelayed(
              new SearchTextChangedRunnable(searchTextChangeCount), SEARCH_DELAY_MS);
        }
      }

      @Override
      public void afterTextChanged(Editable s) {}
    });

    view.findViewById(R.id.search_btn).setOnClickListener(v -> {
       searchQuery = search.getText().toString();
       recyclerViewHelper.refresh();
    });

    view.findViewById(R.id.tune_btn).setOnClickListener(v -> onTuneClicked());

    view.findViewById(R.id.cancel_btn).setOnClickListener(
        v -> NavHostFragment.findNavController(this).popBackStack());

    EmpireManager.eventBus.register(eventHandler);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    EmpireManager.eventBus.unregister(eventHandler);
  }

  private void onTuneClicked() {
    if (destWormhole == null) {
      log.warning("No wormhole selected, tuning unavailable.");
      return;
    }

    App.i.getTaskRunner().runTask(() -> {
      String url = "stars/" + getSrcWormhole().getKey() + "/wormhole/tune";
      try {
        Messages.WormholeTuneRequest request_pb = Messages.WormholeTuneRequest.newBuilder()
            .setSrcStarId(Integer.parseInt(getSrcWormhole().getKey()))
            .setDestStarId(Integer.parseInt(destWormhole.getKey())).build();
        Messages.Star pb = ApiClient.postProtoBuf(url, request_pb, Messages.Star.class);
        Star star = new Star();
        star.fromProtocolBuffer(pb);
        return star;
      } catch (ApiException e) {
        log.error("Error tuning.", e);
        return null;
      }
    }, Threads.BACKGROUND)
    .then((star) -> {
      if (star != null) {
        StarManager.i.notifyStarUpdated(star);
        NavHostFragment.findNavController(this).popBackStack();
      }
    }, Threads.UI);
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onEmpireUpdated(Empire empire) {/*
      final LinearLayout wormholeItemContainer = findViewById(R.id.wormholes);
      for (int i = 0; i < wormholeItemContainer.getChildCount(); i++) {
        View itemView = wormholeItemContainer.getChildAt(i);
        Star star = (Star) itemView.getTag();
        if (star.getWormholeExtra().getEmpireID() == empire.getID()) {
          refreshWormhole(itemView);
        }
      }*/
    }
  };

  /**
   * Runnable that is posted delayed to perform a search when the text in the search query changes.
   */
  private class SearchTextChangedRunnable implements Runnable {
    private final int changeCount;

    SearchTextChangedRunnable(int changeCount) {
      this.changeCount = changeCount;
    }

    @Override
    public void run() {
      searchQuery = search.getText().toString();
      recyclerViewHelper.refresh();

      if (searchTextChangeCount != changeCount) {
        // The text has changed again since we were posted, wait a bit again and post
        handler.postDelayed(new SearchTextChangedRunnable(searchTextChangeCount), SEARCH_DELAY_MS);
      }
    }
  }
}
