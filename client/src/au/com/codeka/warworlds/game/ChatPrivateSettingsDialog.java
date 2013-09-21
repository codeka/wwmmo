package au.com.codeka.warworlds.game;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;

public class ChatPrivateSettingsDialog extends DialogFragment {
    private View mView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.chat_private_settings, null);

        final AutoCompleteTextView inviteEmpireName = (AutoCompleteTextView) mView.findViewById(R.id.empire_name);
        inviteEmpireName.setAdapter(new EmpireNameAutoCompleteAdapter(getActivity()));

        return new StyledDialog.Builder(getActivity())
            .setTitle("Chat Settings")
            .setView(mView)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                }
            })
            .setNegativeButton("Cancel", null)
            .create();
    }
}
