package au.com.codeka.warworlds.game.alliance;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.TabFragmentActivity;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;

public class AllianceActivity extends TabFragmentActivity {
    private Context mContext = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getTabManager().addTab(mContext, new TabInfo("Overview", OverviewFragment.class, null));
    }


    public static class BaseFragment extends Fragment {
        /**
         * Gets a view to display if we're still loading the empire details.
         */
        protected View getLoadingView(LayoutInflater inflator) {
            return inflator.inflate(R.layout.empire_loading_tab, null);
        }
    }

    public static class OverviewFragment extends BaseFragment {
        private View mView;
        private RankListAdapter mRankListAdapter;

        @Override
        public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
            mView = inflator.inflate(R.layout.alliance_overview_tab, null);
            mRankListAdapter = new RankListAdapter();

            MyEmpire empire = EmpireManager.getInstance().getEmpire();

            final Button createBtn = (Button) mView.findViewById(R.id.create_alliance_btn);
            createBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onAllianceCreate();
                }
            });

            return mView;
        }

        private void onAllianceCreate() {
            AllianceCreateDialog dialog = new AllianceCreateDialog();
            dialog.show(getActivity().getSupportFragmentManager(), "");
        }

        private class RankListAdapter extends BaseAdapter {
            private ArrayList<ItemEntry> mEntries;

            @Override
            public int getCount() {
                if (mEntries == null)
                    return 0;
                return mEntries.size();
            }

            @Override
            public Object getItem(int position) {
                if (mEntries == null)
                    return null;
                return mEntries.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return null;
            }

            public class ItemEntry {
                public Empire empire;

                public ItemEntry(Empire empire) {
                    this.empire = empire;
                }
            }
        }
    }

}
