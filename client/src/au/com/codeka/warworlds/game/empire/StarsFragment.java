package au.com.codeka.warworlds.game.empire;

import android.support.v4.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import au.com.codeka.warworlds.model.EmpireStarsFetcher;
import au.com.codeka.warworlds.model.Star;

/**
 * This is the base fragment for fragments which shows lists of stars (so that would be the
 * colonies, fleets and build queue fragments).
 */
public class StarsFragment extends BaseFragment {

    /** Base adapter class for our various tabs. */
    public static abstract class StarsListAdapter extends BaseExpandableListAdapter {
        private EmpireStarsFetcher fetcher;

        // should never have > 40 visible at once...
        private LruCache<Integer, Star> mStarCache = new LruCache<Integer, Star>(40);

        public StarsListAdapter(EmpireStarsFetcher fetcher) {
            this.fetcher = fetcher;
        }

        // TODO: ?
        @Override
        public boolean hasStableIds() {
            return false;
        }
     
        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public int getGroupCount() {
            return fetcher.getNumStars();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return getStar(groupPosition);
        }

        protected abstract int getNumChildren(Star star);
        protected abstract Object getChild(Star star, int index);
        protected abstract View getStarView(Star star, View convertView, ViewGroup parent);
        protected abstract View getChildView(Star star, int index, View convertView,
                ViewGroup parent);

        @Override
        public int getChildrenCount(int groupPosition) {
            Star star = getStar(groupPosition);
            if (star == null) {
                return 0;
            }

            return getNumChildren(star);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            Star star = getStar(groupPosition);
            if (star == null) {
                return null;
            }

            return getChild(star, childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
            Star star = getStar(groupPosition);
            return getStarView(star, convertView, parent);
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            Star star = getStar(groupPosition);
            return getChildView(star, childPosition, convertView, parent);
        }

        private Star getStar(int index) {
            Star star = mStarCache.get(index);
            if (star == null) {
                star = fetcher.getStar(index);
                if (star != null) {
                    mStarCache.put(index, star);
                }
            }
            return star;
        }
    }
}
