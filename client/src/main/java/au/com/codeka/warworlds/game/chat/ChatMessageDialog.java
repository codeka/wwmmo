package au.com.codeka.warworlds.game.chat;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import au.com.codeka.Clipboard;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.game.empire.EmpireFragment;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;

import com.google.protobuf.InvalidProtocolBufferException;

public class ChatMessageDialog extends DialogFragment {
  private View view;
  private ChatMessage chatMessage;
  private Empire empire;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    LayoutInflater inflater = getActivity().getLayoutInflater();
    view = inflater.inflate(R.layout.chat_popup_dlg, null);

    Bundle args = requireArguments();
    empire = new Empire();
    chatMessage = new ChatMessage();
    try {
      empire.fromProtocolBuffer(Messages.Empire.parseFrom(args.getByteArray("au.com.codeka.warworlds.Empire")));
      chatMessage.fromProtocolBuffer(Messages.ChatMessage.parseFrom(args.getByteArray("au.com.codeka.warworlds.ChatMessage")));
    } catch (InvalidProtocolBufferException e) {
    }

    Button copyMessageTextBtn = view.findViewById(R.id.copy_text_btn);
    copyMessageTextBtn.setOnClickListener(v -> {
      Clipboard.copyText(getActivity(), "message", chatMessage.getMessage());
      Toast.makeText(getActivity(), "Message copied to clipboard.", Toast.LENGTH_SHORT).show();
      dismiss();
    });

    Button viewEmpireBtn = view.findViewById(R.id.view_empire_btn);
    viewEmpireBtn.setOnClickListener(v -> {
      // TODO
      if (empire.getKey().equals(EmpireManager.i.getEmpire().getKey())) {
//        Intent intent = new Intent(getActivity(), EmpireFragment.class);
//        startActivity(intent);
      } else {
//        NavHostFragment.findNavController(this).navigate(R.id.empireFragment, TODO);
//        Intent intent = new Intent(getActivity(), EnemyEmpireActivity.class);
//        intent.putExtra("au.com.codeka.warworlds.EmpireKey", mEmpire.getKey());
//        getActivity().startActivity(intent);
      }

      dismiss();
    });

    Button blockBtn = view.findViewById(R.id.block_btn);
    blockBtn.setOnClickListener(v -> {
      onBlockClick();
      dismiss();
    });

    Button privateMessageBtn = view.findViewById(R.id.private_message_btn);
    privateMessageBtn.setOnClickListener(v -> {
      ChatManager.i.startConversation(empire.getID());
      dismiss();
    });

    return new StyledDialog.Builder(getActivity())
        .setTitle(empire.getDisplayName())
        .setView(view)
        .create();
  }

  private void onBlockClick() {
    final Context context = getActivity();
    new StyledDialog.Builder(context)
        .setTitle("Block empire")
        .setMessage("Blocking an empire means you won't hear from this empire any more. You can "
            + " unblock them again from the 'Blocked empires' button above."
            + " Are you sure you want to block " + empire.getDisplayName() + "?")
        .setPositiveButton("Block", (dialog, which) -> {
          doBlock(context);
          dialog.dismiss();
        }).setNegativeButton("Cancel", null)
        .create().show();
  }

  private void doBlock(final Context context) {
    ChatManager.i.blockEmpire(context, chatMessage, () -> new StyledDialog.Builder(context)
        .setTitle("Blocked")
        .setMessage(
            String.format(
                "%s has been blocked, you will not see any more messages from them.",
                empire.getDisplayName()))
        .setPositiveButton("OK", null)
        .create()
        .show());
  }
}
