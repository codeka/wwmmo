package au.com.codeka.warworlds.game.alliance;

import java.util.Locale;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.Empire;

public class KickRequestDialog extends DialogFragment {
    private View mView;
    private Alliance mAlliance;
    private Empire mEmpire;

    public void setup(Alliance alliance, Empire empire) {
        mAlliance = alliance;
        mEmpire = empire;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        mView = inflater.inflate(R.layout.alliance_request_dlg, null);

        TextView instructions = (TextView) mView.findViewById(R.id.instructions);
        instructions.setText(String.format(Locale.ENGLISH, "Are you sure you want to kick \"%s\" out of %s? Give a reason below for other members to vote on.",
                mEmpire.getDisplayName(), mAlliance.getName()));

        View amount = mView.findViewById(R.id.amount);
        amount.setVisibility(View.GONE);

        return new StyledDialog.Builder(getActivity())
                           .setView(mView)
                           .setTitle("Kick Empire")
                           .setPositiveButton("Kick", new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {
                                   onWithdraw();
                               }
                           })
                           .setNegativeButton("Cancel", null)
                           .create();
    }

    private void onWithdraw() {
        TextView message = (TextView) mView.findViewById(R.id.message);
        AllianceManager.i.requestKick(mAlliance.getID(), mEmpire.getKey(), message.getText().toString());

        dismiss();
    }
}

