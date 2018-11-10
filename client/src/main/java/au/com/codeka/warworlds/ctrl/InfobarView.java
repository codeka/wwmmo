package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.Cash;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.api.RequestManagerState;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;

/**
 * The "infobar" control displays the current empire name, your cash level and
 * few other indicators that we want visible (almost) everywhere.
 */
public class InfobarView extends FrameLayout {
  private Handler handler;
  private View view;

  // The cash value we're currently displaying, so we can animate to the "real"
  // value as it's
  // updated.
  private static double sCurrCash;
  private static double sRealCash;
  private static int sEmpireID; // if this changes, we update the cash instantly

  public InfobarView(Context context, AttributeSet attrs) {
    super(context, attrs);

    view = inflate(context, R.layout.infobar_ctrl, null);
    this.addView(view);
  }

  public void hideEmpireName() {
    view.findViewById(R.id.empire_name).setVisibility(View.GONE);
  }

  private Runnable updateCashRunnable = new Runnable() {
    @Override
    public void run() {
      boolean increasing = sRealCash > sCurrCash;
      double newCash = sCurrCash + (increasing ? 125f : -125f);
      if ((increasing && newCash > sRealCash) || (!increasing && newCash < sRealCash)) {
        newCash = sRealCash;
      }
      TextView cash = view.findViewById(R.id.cash);
      cash.setText(Cash.format(newCash));
      sCurrCash = newCash;

      if (sCurrCash != sRealCash) {
        handler.post(updateCashRunnable);
      }
    }
  };

  /** Must be run on the UI thread. */
  private void refresh(RequestManagerState requestManagerState) {
    if (isInEditMode()) {
      return;
    }

    ProgressBar working = view.findViewById(R.id.working);
    if (requestManagerState.numInflightRequests > 0) {
      working.setVisibility(View.VISIBLE);
    } else {
      working.setVisibility(View.GONE);
    }
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (isInEditMode()) {
      return;
    }
    handler = new Handler();

    EmpireManager.eventBus.register(eventHandler);
    RequestManager.eventBus.register(eventHandler);

    refreshEmpire(EmpireManager.i.getEmpire());
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (isInEditMode()) {
      return;
    }

    RequestManager.eventBus.unregister(eventHandler);
    EmpireManager.eventBus.unregister(eventHandler);
  }

  private void refreshEmpire(Empire empire) {
    MyEmpire myEmpire = EmpireManager.i.getEmpire();
    if (empire != null && myEmpire != null && myEmpire.getID() == empire.getID()) {
      sRealCash = empire.getCash();
      if (sCurrCash == 0.0f || sEmpireID != myEmpire.getID()) {
        sCurrCash = sRealCash;
        sEmpireID = myEmpire.getID();
      } else if (sRealCash != sCurrCash) {
        handler.post(updateCashRunnable);
      }
      TextView cash = view.findViewById(R.id.cash);
      cash.setText(Cash.format(sCurrCash));

      TextView empireName = view.findViewById(R.id.empire_name);
      empireName.setText(empire.getDisplayName());
    }

    // set up the initial state
    refresh(RequestManager.i.getCurrentState());
  }

  private Object eventHandler = new Object() {
    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      refreshEmpire(empire);
    }

    @EventHandler()
    public void onRequestManagerStateChanged(RequestManagerState requestManagerState) {
      refresh(requestManagerState);
    }
  };
}
