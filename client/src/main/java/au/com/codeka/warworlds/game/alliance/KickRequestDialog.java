package au.com.codeka.warworlds.game.alliance;

import java.util.Locale;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.Empire;

public class KickRequestDialog extends DialogFragment {
  private View view;
  private Alliance alliance;
  private Empire empire;

  public void setup(Alliance alliance, Empire empire) {
    this.alliance = alliance;
    this.empire = empire;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Activity activity = getActivity();
    LayoutInflater inflater = activity.getLayoutInflater();
    view = inflater.inflate(R.layout.alliance_request_dlg, null);

    TextView instructions = view.findViewById(R.id.instructions);
    instructions.setText(String.format(
        Locale.ENGLISH,
        "Are you sure you want to kick \"%s\" out of %s? Give a reason below for other members to vote on.",
        empire.getDisplayName(), alliance.getName()));

    View amount = view.findViewById(R.id.amount);
    amount.setVisibility(View.GONE);

    return new StyledDialog.Builder(getActivity())
        .setView(view)
        .setTitle("Kick Empire")
        .setPositiveButton("Kick", (dialog, which) -> onWithdraw())
        .setNegativeButton("Cancel", null)
        .create();
  }

  private void onWithdraw() {
    TextView message = view.findViewById(R.id.message);
    AllianceManager.i.requestKick(alliance.getID(), empire.getKey(), message.getText().toString());

    dismiss();
  }
}

