package au.com.codeka.warworlds.client.ctrl;


import android.databinding.tool.util.Preconditions;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import javax.annotation.Nonnull;

import au.com.codeka.warworlds.client.world.StarCollection;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Base adapter class for adapter which show an expandable list of stars which you can expand and
 * collapse and show other things inside of.
 */
public abstract class ExpandableStarListAdapter extends BaseExpandableListAdapter {
  private final StarCollection stars;

  public ExpandableStarListAdapter(@Nonnull StarCollection stars) {
    this.stars = stars;
  }

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
    return stars.size();
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
    return stars.get(index);
  }
}