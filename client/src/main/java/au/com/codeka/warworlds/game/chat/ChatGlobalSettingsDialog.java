package au.com.codeka.warworlds.game.chat;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import androidx.fragment.app.DialogFragment;

import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;

public class ChatGlobalSettingsDialog extends DialogFragment {
  private View view;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    LayoutInflater inflater = getActivity().getLayoutInflater();
    view = inflater.inflate(R.layout.chat_global_settings, null);

    final CheckBox autoTranslate = view.findViewById(R.id.auto_translate);
    autoTranslate.setChecked(new GlobalOptions().autoTranslateChatMessages());

    return new StyledDialog.Builder(getActivity())
        .setTitle("Chat Settings")
        .setView(view)
        .setPositiveButton("OK", (dialog, which) -> {
          new GlobalOptions().autoTranslateChatMessages(autoTranslate.isChecked());
          dismiss();
        })
        .setNegativeButton("Cancel", null)
        .create();
  }
}
