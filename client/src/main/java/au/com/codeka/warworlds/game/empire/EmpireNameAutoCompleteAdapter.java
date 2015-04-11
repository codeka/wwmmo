package au.com.codeka.warworlds.game.empire;

import java.util.List;

import android.content.Context;
import android.os.Handler;
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
 */
public class EmpireNameAutoCompleteAdapter extends BaseAdapter implements Filterable {
  private Context context;
  private EmpireFilter filter = new EmpireFilter();
  private List<Empire> empires;
  private Handler handler;

  public EmpireNameAutoCompleteAdapter(Context context) {
    this.context = context;
    handler = new Handler();
  }

  @Override
  public int getCount() {
    if (empires == null) {
      return 0;
    }
    return empires.size();
  }

  @Override
  public Object getItem(int position) {
    return empires.get(position).getDisplayName();
  }

  @Override
  public long getItemId(int position) {
    return Long.parseLong(empires.get(position).getKey());
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    TextView v = (TextView) convertView;
    if (v == null) {
      v = new TextView(context);
      v.setPadding(10, 20, 10, 20);
    }

    v.setText(empires.get(position).getDisplayName());
    return v;
  }

  @Override
  public Filter getFilter() {
    return filter;
  }

  private class EmpireFilter extends Filter {
    private boolean isSearching;
    private String scheduleAgainConstraint;

    /** Performs the actual filter. */
    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
      FilterResults results = new FilterResults();
      if (constraint != null && !constraint.equals("")) {
        fetchFromServer(constraint.toString());
      }
      return results;
    }

    /**
     * Along with fetching from the cache, we'll do a more thorough search from the server.
     */
    private void fetchFromServer(final String constraint) {
      if (isSearching) {
        scheduleAgainConstraint = constraint;
        return;
      }
      isSearching = true;

      handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          EmpireManager.i.searchEmpires(constraint, new EmpireManager.SearchCompleteHandler() {
            @Override
            public void onSearchComplete(List<Empire> empires) {
              EmpireNameAutoCompleteAdapter.this.empires = empires;
              notifyDataSetChanged();

              isSearching = false;
              if (scheduleAgainConstraint != null) {
                fetchFromServer(scheduleAgainConstraint);
                scheduleAgainConstraint = null;
              }
            }
          });
        }
      }, 100); // wait 100ms to avoid overloading the server
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
