package au.com.codeka.warworlds.game.chat;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import androidx.fragment.app.DialogFragment;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.game.empire.EmpireNameAutoCompleteAdapter;
import au.com.codeka.warworlds.model.ChatConversation;
import au.com.codeka.warworlds.model.ChatManager;

import com.google.protobuf.InvalidProtocolBufferException;

public class ChatPrivateSettingsDialog extends DialogFragment {
  private View view;
  private ChatConversation conversation;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    LayoutInflater inflater = requireActivity().getLayoutInflater();
    view = inflater.inflate(R.layout.chat_private_settings, null);

    Bundle args = requireArguments();
    try {
      Messages.ChatConversation chat_conversation_pb = Messages.ChatConversation.parseFrom(
          args.getByteArray("au.com.codeka.warworlds.ChatConversation"));
      conversation = new ChatConversation(chat_conversation_pb.getId());
      conversation.fromProtocolBuffer(chat_conversation_pb);
    } catch (InvalidProtocolBufferException e) {
    }

    final AutoCompleteTextView inviteEmpireName = view.findViewById(R.id.empire_name);
    inviteEmpireName.setAdapter(new EmpireNameAutoCompleteAdapter(getActivity()));

    final Button inviteBtn = view.findViewById(R.id.invite_btn);
    inviteBtn.setOnClickListener(v -> {
      inviteBtn.setEnabled(false);
      ChatManager.i.addParticipant(conversation, inviteEmpireName.getText().toString());
      dismiss();
    });

    final Button leaveBtn = view.findViewById(R.id.leave_btn);
    leaveBtn.setOnClickListener(v -> {
      ChatManager.i.leaveConversation(conversation);
      dismiss();
    });

    final Button muteBtn = view.findViewById(R.id.mute_btn);
    if (conversation.isMuted()) {
      muteBtn.setText("Unmute Conversation");
    } else {
      muteBtn.setText("Mute Conversation");
    }
    muteBtn.setOnClickListener(v -> {
      if (conversation.isMuted()) {
        ChatManager.i.unmuteConversation(conversation);
      } else {
        ChatManager.i.muteConversation(conversation);
      }
      dismiss();
    });

    return new StyledDialog.Builder(getActivity())
        .setTitle("Chat Settings")
        .setView(view)
        .setNegativeButton("Cancel", null)
        .create();
  }
}
