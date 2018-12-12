package au.com.codeka.warworlds.client.game.starsearch;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ListView;
import android.widget.RelativeLayout;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.common.proto.Star;

public class StarSearchLayout extends RelativeLayout {
  interface Callback {
    void onStarClick(Star star);
  }

  private final StarSearchListAdapter adapter;

  public StarSearchLayout(Context context, Callback callback) {
    super(context);
    inflate(context, R.layout.starsearch, this);

    setBackgroundColor(context.getResources().getColor(R.color.default_background));

    ListView lv = findViewById(R.id.search_result);
    adapter = new StarSearchListAdapter(LayoutInflater.from(context));
    lv.setAdapter(adapter);
    adapter.setCursor(StarManager.i.getMyStars());

    lv.setOnItemClickListener((adapterView, view, position, l) -> {
      Star star = adapter.getStar(position);
      callback.onStarClick(star);
    });
  }
}
