package au.com.codeka.warworlds.game.alliance;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.EmpireManager;

public class AllianceCreateDialog extends DialogFragment {
    private View mView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        mView = inflater.inflate(R.layout.alliance_create_dlg, null);

        return new StyledDialog.Builder(getActivity())
                           .setView(mView)
                           .setTitle("Create Alliance")
                           .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {
                                   onAllianceCreate();
                               }
                           })
                           .setNegativeButton("Cancel", null)
                           .create();
    }

    private void onAllianceCreate() {
        TextView allianceNameView = (TextView) mView.findViewById(R.id.alliance_name);
        final String allianceName = allianceNameView.getText().toString();

        final Context context = getActivity();
        dismiss();

        new BackgroundRunner<Boolean>() {
            private String mErrorMsg;

            @Override
            protected Boolean doInBackground() {
                Messages.Alliance alliance_pb = Messages.Alliance.newBuilder()
                               .setName(allianceName)
                               .build();

                try {
                    return ApiClient.postProtoBuf("alliances", alliance_pb);
                } catch (ApiException e) {
                    mErrorMsg = e.getServerErrorMessage();
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                if (!success) {
                    StyledDialog.showErrorMessage(context, mErrorMsg);
                } else {
                    EmpireManager.i.refreshEmpire();
                }
            }
        }.execute();
    }
}
