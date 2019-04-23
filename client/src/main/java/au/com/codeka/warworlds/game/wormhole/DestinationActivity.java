package au.com.codeka.warworlds.game.wormhole;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;

import static com.google.common.base.Preconditions.checkNotNull;

public class DestinationActivity extends BaseActivity {
  private static final Log log = new Log("DestinatonDialog");
  private StyledDialog dialog;
  private Star srcWormhole;
  private Star destWormhole;

  public static Intent newStartIntent(Context context, Star srcWormhole) {
    Messages.Star.Builder starbuilder = Messages.Star.newBuilder();
    srcWormhole.toProtocolBuffer(starbuilder);

    Intent intent = new Intent(context, DestinationActivity.class);
    intent.putExtra("srcWormhole", starbuilder.build().toByteArray());
    return intent;
  }

  private Star getSrcWormhole() {
    if (srcWormhole == null) {
      Bundle args = checkNotNull(getIntent().getExtras());
      srcWormhole = new Star();
      try {
        Messages.Star starmessage = Messages.Star.parseFrom(args.getByteArray("srcWormhole"));
        srcWormhole.fromProtocolBuffer(starmessage);
      } catch (InvalidProtocolBufferException e) {
        log.error("Failed to load srcWormhole from Protocol Buffer", e);
      }
    }

    return srcWormhole;
  }

  @SuppressLint("InflateParams")
  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.wormhole_destination_dlg);

    final View progressBar = findViewById(R.id.progress_bar);
    final LinearLayout wormholeItemContainer = findViewById(R.id.wormholes);
    final View noWormholesMsg = findViewById(R.id.no_wormholes_msg);
    final TextView tuneTime = findViewById(R.id.tune_time);

    progressBar.setVisibility(View.VISIBLE);
    wormholeItemContainer.setVisibility(View.GONE);
    tuneTime.setVisibility(View.GONE);
    noWormholesMsg.setVisibility(View.GONE);

    TextView starName = findViewById(R.id.star_name);
    starName.setText(getSrcWormhole().getName());

    ImageView starIcon = findViewById(R.id.star_icon);
    Sprite starSprite = StarImageManager.getInstance().getSprite(getSrcWormhole(), 60, true);
    starIcon.setImageDrawable(new SpriteDrawable(starSprite));

    MyEmpire myEmpire = EmpireManager.i.getEmpire();
    if (myEmpire.getAlliance() != null) {
      AllianceManager.i.fetchWormholes(Integer.parseInt(myEmpire.getAlliance().getKey()),
          new AllianceManager.FetchWormholesCompleteHandler() {
            @Override
            public void onWormholesFetched(List<Star> wormholes) {
              DestinationActivity.this.onWormholesFetched(wormholes);
            }
          });
    } else {
      // TODO: support wormholes in your own empire at least...
    }

    int tuneTimeHours = getSrcWormhole().getWormholeExtra() == null ? 0 : getSrcWormhole()
        .getWormholeExtra().getTuneTimeHours();
    tuneTime.setText(String.format(Locale.ENGLISH, "Tune time: %d hr%s", tuneTimeHours,
        tuneTimeHours == 1 ? "" : "s"));

    /*
    b.setPositiveButton("Tune", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface d, int id) {
        onTuneClicked();
        d.dismiss();
      }
    });
    b.setNegativeButton("Cancel", null);

    dialog = b.create();
    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override
      public void onShow(DialogInterface d) {
        dialog.getPositiveButton().setEnabled(false);
      }
    });
*/
    EmpireManager.eventBus.register(eventHandler);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    EmpireManager.eventBus.unregister(eventHandler);
  }

  private final View.OnClickListener itemClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      destWormhole = (Star) v.getTag();
      refreshWormholes();
      dialog.getPositiveButton().setEnabled(true);
    }
  };

  private void onWormholesFetched(List<Star> wormholes) {
    final View progressBar = findViewById(R.id.progress_bar);
    final LinearLayout wormholeItemContainer = findViewById(R.id.wormholes);
    final View tuneTime = findViewById(R.id.tune_time);
    final View noWormholesMsg = findViewById(R.id.no_wormholes_msg);

    // Remove the current wormhole, since obviously you can't tune to that.
    for (int i = 0; i < wormholes.size(); i++) {
      if (wormholes.get(i).getID() == getSrcWormhole().getID()) {
        wormholes.remove(i);
        break;
      }
    }

    progressBar.setVisibility(View.GONE);
    if (wormholes.isEmpty()) {
      wormholeItemContainer.setVisibility(View.GONE);
      noWormholesMsg.setVisibility(View.VISIBLE);
    } else {
      wormholeItemContainer.setVisibility(View.VISIBLE);
      noWormholesMsg.setVisibility(View.GONE);
      tuneTime.setVisibility(View.VISIBLE);

      LayoutInflater inflater = getLayoutInflater();
      for (Star wormhole : wormholes) {
        View itemView = inflater.inflate(R.layout.wormhole_destination_entry_row,
            wormholeItemContainer, false);

        itemView.setTag(wormhole);
        itemView.setOnClickListener(itemClickListener);
        refreshWormhole(itemView);

        wormholeItemContainer.addView(itemView);
      }
    }
  }

  private void refreshWormholes() {
    final LinearLayout wormholeItemContainer = findViewById(R.id.wormholes);
    for (int i = 0; i < wormholeItemContainer.getChildCount(); i++) {
      View itemView = wormholeItemContainer.getChildAt(i);
      refreshWormhole(itemView);
    }
  }

  private void refreshWormhole(View itemView) {
    ImageView starIcon = itemView.findViewById(R.id.star_icon);
    ImageView empireIcon = itemView.findViewById(R.id.empire_icon);
    TextView wormholeName = itemView.findViewById(R.id.wormhole_name);
    TextView empireName = itemView.findViewById(R.id.empire_name);
    TextView distance = itemView.findViewById(R.id.distance);

    Star wormhole = (Star) itemView.getTag();
    Empire empire = EmpireManager.i.getEmpire(wormhole.getWormholeExtra().getEmpireID());
    if (empire == null) {
      empireIcon.setImageBitmap(null);
      empireName.setText("");
    } else {
      Bitmap bmp = EmpireShieldManager.i.getShield(this, empire);
      empireIcon.setImageBitmap(bmp);
      empireName.setText(empire.getDisplayName());
    }

    Sprite starSprite = StarImageManager.getInstance().getSprite(wormhole, 20, true);
    starIcon.setImageDrawable(new SpriteDrawable(starSprite));

    wormholeName.setText(wormhole.getName());

    float distanceInPc = Sector.distanceInParsecs(getSrcWormhole(), wormhole);
    distance.setText(String.format(Locale.ENGLISH, "%s %.1f pc", wormhole.getCoordinateString(),
        distanceInPc));

    if (destWormhole != null && destWormhole.getKey().equals(wormhole.getKey())) {
      itemView.setBackgroundResource(R.color.list_item_selected);
    } else {
      itemView.setBackgroundResource(android.R.color.transparent);
    }
  }

  private void onTuneClicked() {
    if (destWormhole == null) {
      return;
    }

    new BackgroundRunner<Star>() {
      @Override
      protected Star doInBackground() {
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
          return null;
        }
      }

      @Override
      protected void onComplete(Star star) {
        if (star != null) {
          StarManager.i.notifyStarUpdated(star);
        }
      }
    }.execute();
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      final LinearLayout wormholeItemContainer = findViewById(R.id.wormholes);
      for (int i = 0; i < wormholeItemContainer.getChildCount(); i++) {
        View itemView = wormholeItemContainer.getChildAt(i);
        Star star = (Star) itemView.getTag();
        if (star.getWormholeExtra().getEmpireID() == empire.getID()) {
          refreshWormhole(itemView);
        }
      }
    }
  };
}
