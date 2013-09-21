package au.com.codeka.warworlds.game;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;

/**
 * This is an implementation of ListAdapter which you can use to auto-complete empire names.
 *
 * Initially, this class will populate the list with empires we have cached (since they're
 * the ones you're likely interacting with anyway), but we'll also do a search in the background
 * on the server to get more if you wait a bit.
 */
public class EmpireNameAutoCompleteAdapter extends BaseAdapter implements Filterable {
    private Context mContext;
    private EmpireFilter mFilter = new EmpireFilter();
    private List<Empire> mEmpires;

    public EmpireNameAutoCompleteAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        if (mEmpires == null) {
            return 0;
        }
        return mEmpires.size();
    }

    @Override
    public Object getItem(int position) {
        return mEmpires.get(position);
    }

    @Override
    public long getItemId(int position) {
        return Long.parseLong(mEmpires.get(position).getKey());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView v = (TextView) convertView;
        if (v == null) {
            v = new TextView(mContext);
        }

        v.setText(mEmpires.get(position).getDisplayName());
        return v;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    private class EmpireFilter extends Filter {
        /**
         * Performs the actual filters. We actually do two "levels" of filters. First, we
         * fetch any empires that are cached which match the filter, at the same time we
         * fire off a query to the server to fetch any others that may not be cached.
         */
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint != null && !constraint.equals("")) {
                mEmpires = EmpireManager.i.getMatchingEmpiresFromCache(constraint.toString());
                results.values = mEmpires;
                results.count = mEmpires.size();
            }
            return results;
        }

        /** Called when we're done fetching results. */
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results != null) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
        
    }
}
