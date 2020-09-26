package au.com.codeka.warworlds.game.alliance;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.AllianceManager;

public class JoinRequestDialog extends DialogFragment {
  private View view;
  private int allianceID;

  public void setAllianceID(int allianceID) {
    this.allianceID = allianceID;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Activity activity = getActivity();
    LayoutInflater inflater = activity.getLayoutInflater();
    view = inflater.inflate(R.layout.alliance_request_dlg, null);

    View amount = view.findViewById(R.id.amount);
    amount.setVisibility(View.GONE);

    return new StyledDialog.Builder(getActivity())
        .setView(view)
        .setTitle("Join Alliance")
        .setPositiveButton("Join", (dialog, which) -> onAllianceJoin())
        .setNegativeButton("Cancel", null)
        .create();
  }

  private void onAllianceJoin() {
    TextView message = (TextView) view.findViewById(R.id.message);
    AllianceManager.i.requestJoin(allianceID, message.getText().toString());

    new StyledDialog.Builder(getActivity())
        .setMessage("The request to join this alliance has been sent, you'll get a notification when your application is approved.")
        .setNeutralButton("OK", null)
        .create().show();

    dismiss();
  }
}

