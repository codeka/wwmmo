package au.com.codeka.warworlds.game.wormhole;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.protobuf.Messages;
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

public class DestinationDialog extends DialogFragment {
    private Star mSrcWormhole;
    private Star mDestWormhole;
    private View mView;
    private WormholeAdapter mWormholeAdapter;

    public void loadWormholes(Star srcWormhole) {
        mSrcWormhole = srcWormhole;
    }

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        mView = inflater.inflate(R.layout.wormhole_destination_dlg, null);

        final View progressBar = mView.findViewById(R.id.progress_bar);
        final ListView wormholeItems = (ListView) mView.findViewById(R.id.wormholes);

        progressBar.setVisibility(View.VISIBLE);
        wormholeItems.setVisibility(View.GONE);

        mWormholeAdapter = new WormholeAdapter();
        wormholeItems.setAdapter(mWormholeAdapter);

        MyEmpire myEmpire = EmpireManager.i.getEmpire();
        if (myEmpire.getAlliance() != null) {
            AllianceManager.i.fetchWormholes(Integer.parseInt(myEmpire.getAlliance().getKey()),
                new AllianceManager.FetchWormholesCompleteHandler() {
                    @Override
                    public void onWormholesFetched(List<Star> wormholes) {
                        DestinationDialog.this.onWormholesFetched(wormholes);
                    }
                });
        } else {
            // TODO: support wormholes in your own empire at least...
        }

        TextView tuneTime = (TextView) mView.findViewById(R.id.tune_time);
        int tuneTimeHours = mSrcWormhole.getWormholeExtra() == null
                ? 0 : mSrcWormhole.getWormholeExtra().getTuneTimeHours();
        tuneTime.setText(String.format(Locale.ENGLISH, "Tune time: %d hr%s",
                tuneTimeHours, tuneTimeHours == 1 ? "" : "s"));

        StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
        b.setView(mView);
        b.setPositiveButton("Tune", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int id) {
                onTuneClicked();
                d.dismiss();
            }
        });
        b.setNegativeButton("Cancel", null);

        final StyledDialog dialog = b.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getPositiveButton().setEnabled(false);
            }
        });

        wormholeItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Star star = (Star) mWormholeAdapter.getItem(position);
                mDestWormhole = star;
                mWormholeAdapter.notifyDataSetChanged();
                dialog.getPositiveButton().setEnabled(true);
            }
        });

        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EmpireManager.eventBus.register(eventHandler);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EmpireManager.eventBus.unregister(eventHandler);
    }

    private void onWormholesFetched(List<Star> wormholes) {
        final View progressBar = mView.findViewById(R.id.progress_bar);
        final ListView wormholeItems = (ListView) mView.findViewById(R.id.wormholes);

        progressBar.setVisibility(View.GONE);
        wormholeItems.setVisibility(View.VISIBLE);

        TreeSet<Integer> empireIDs = new TreeSet<Integer>();
        for (Star wormhole : wormholes){
            int empireID = wormhole.getWormholeExtra().getEmpireID();
            if (!empireIDs.contains(empireID)) {
                empireIDs.add(empireID);
            }
        }
        for (Integer empireID : empireIDs) {
            Empire empire = EmpireManager.i.getEmpire(empireID);
            if (empire != null) {
                mWormholeAdapter.notifyDataSetChanged();
            }
        }

        mWormholeAdapter.setWormholes(wormholes);
    }

    public void onTuneClicked() {
        if (mDestWormhole == null) {
            return;
        }

        new BackgroundRunner<Star>() {
            @Override
            protected Star doInBackground() {
                String url = "stars/"+mSrcWormhole.getKey()+"/wormhole/tune";
                try {
                    Messages.WormholeTuneRequest request_pb = Messages.WormholeTuneRequest.newBuilder()
                            .setSrcStarId(Integer.parseInt(mSrcWormhole.getKey()))
                            .setDestStarId(Integer.parseInt(mDestWormhole.getKey()))
                            .build();
                    Messages.Star pb = ApiClient.postProtoBuf(url, request_pb, Messages.Star.class);
                    Star star = new Star();
                    star.fromProtocolBuffer(pb);
                    return star;
                } catch(ApiException e) {
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
            mWormholeAdapter.notifyDataSetChanged();
        }
    };

    private class WormholeAdapter extends BaseAdapter {
        private List<Star> mWormholes;

        public void setWormholes(List<Star> wormholes) {
            ArrayList<Star> availableWormholes = new ArrayList<Star>();
            for (Star wormhole : wormholes) {
                if (wormhole.getKey().equals(mSrcWormhole.getKey())) {
                    continue;
                }
                availableWormholes.add(wormhole);
            }

            mWormholes = availableWormholes;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return (mWormholes == null ? 0 : mWormholes.size());
        }

        @Override
        public Object getItem(int position) {
            return mWormholes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Star wormhole = mWormholes.get(position);
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                view = inflater.inflate(R.layout.wormhole_destination_entry_row, parent, false);
            }

            ImageView starIcon = (ImageView) view.findViewById(R.id.star_icon);
            ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
            TextView wormholeName = (TextView) view.findViewById(R.id.wormhole_name);
            TextView empireName = (TextView) view.findViewById(R.id.empire_name);
            TextView distance = (TextView) view.findViewById(R.id.distance);

            Empire empire = EmpireManager.i.getEmpire(wormhole.getWormholeExtra().getEmpireID());
            if (empire == null) {
                empireIcon.setImageBitmap(null);
                empireName.setText("");
            } else {
                Bitmap bmp = EmpireShieldManager.i.getShield(getActivity(), empire);
                empireIcon.setImageBitmap(bmp);
                empireName.setText(empire.getDisplayName());
            }

            Sprite starSprite = StarImageManager.getInstance().getSprite(wormhole, 20, true);
            starIcon.setImageDrawable(new SpriteDrawable(starSprite));

            wormholeName.setText(wormhole.getName());

            float distanceInPc = Sector.distanceInParsecs(mSrcWormhole, wormhole);
            distance.setText(String.format(Locale.ENGLISH, "%s %.1f pc", 
                    wormhole.getCoordinateString(), distanceInPc));

            if (mDestWormhole != null && mDestWormhole.getKey().equals(wormhole.getKey())) {
                view.setBackgroundColor(0xff0c6476);
            } else {
                view.setBackgroundColor(0xff000000);
            }

            return view;
        }
    }
}
