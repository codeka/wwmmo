package au.com.codeka.warworlds.game;

import com.google.protobuf.InvalidProtocolBufferException;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.ChatConversation;
import au.com.codeka.warworlds.model.ChatManager;

public class ChatPrivateSettingsDialog extends DialogFragment {
    private View mView;
    private ChatConversation mConversation;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.chat_private_settings, null);

        Bundle args = getArguments();
        try {
            Messages.ChatConversation chat_conversation_pb = Messages.ChatConversation.parseFrom(
                    args.getByteArray("au.com.codeka.warworlds.ChatConversation"));
            mConversation = new ChatConversation(chat_conversation_pb.getId());
            mConversation.fromProtocolBuffer(chat_conversation_pb);
        } catch (InvalidProtocolBufferException e) {
        }

        final AutoCompleteTextView inviteEmpireName = (AutoCompleteTextView) mView.findViewById(R.id.empire_name);
        inviteEmpireName.setAdapter(new EmpireNameAutoCompleteAdapter(getActivity()));

        final Button inviteBtn = (Button) mView.findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inviteBtn.setEnabled(false);
                ChatManager.i.addParticipant(getActivity(), mConversation, inviteEmpireName.getText().toString());
                dismiss();
            }
        });

        final Button leaveBtn = (Button) mView.findViewById(R.id.leave_btn);
        leaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatManager.i.leaveConversation(mConversation);
                dismiss();
            }
        });

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
