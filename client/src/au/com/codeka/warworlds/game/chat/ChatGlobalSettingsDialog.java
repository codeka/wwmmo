package au.com.codeka.warworlds.game.chat;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;

public class ChatGlobalSettingsDialog extends DialogFragment {
    private View mView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.chat_global_settings, null);

        final CheckBox autoTranslate = (CheckBox) mView.findViewById(R.id.auto_translate);
        autoTranslate.setChecked(new GlobalOptions().autoTranslateChatMessages());

        return new StyledDialog.Builder(getActivity())
            .setTitle("Chat Settings")
            .setView(mView)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new GlobalOptions().autoTranslateChatMessages(autoTranslate.isChecked());
                    dismiss();
                }
            })
            .setNegativeButton("Cancel", null)
            .create();
    }
}
