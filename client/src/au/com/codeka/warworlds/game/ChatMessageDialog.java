package au.com.codeka.warworlds.game;

import com.google.protobuf.InvalidProtocolBufferException;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import au.com.codeka.Clipboard;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.ChatConversation;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;

public class ChatMessageDialog extends DialogFragment {
    private View mView;
    private ChatMessage mChatMessage;
    private Empire mEmpire;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.chat_popup_dlg, null);

        Bundle args = getArguments();
        mEmpire = new Empire();
        mChatMessage = new ChatMessage();
        try {
            mEmpire.fromProtocolBuffer(Messages.Empire.parseFrom(args.getByteArray("au.com.codeka.warworlds.Empire")));
            mChatMessage.fromProtocolBuffer(Messages.ChatMessage.parseFrom(args.getByteArray("au.com.codeka.warworlds.ChatMessage")));
        } catch (InvalidProtocolBufferException e) {
        }

        Button copyMessageTextBtn = (Button) mView.findViewById(R.id.copy_text_btn);
        copyMessageTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Clipboard.copyText(getActivity(), "message", mChatMessage.getMessage());
                Toast.makeText(getActivity(), "Message copied to clipboard.", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });

        Button viewEmpireBtn = (Button) mView.findViewById(R.id.view_empire_btn);
        viewEmpireBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEmpire.getKey().equals(EmpireManager.i.getEmpire().getKey())) {
                    Intent intent = new Intent(getActivity(), EmpireActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(getActivity(), EnemyEmpireActivity.class);
                    intent.putExtra("au.com.codeka.warworlds.EmpireKey", mEmpire.getKey());
                    getActivity().startActivity(intent);
                }

                dismiss();
            }
        });

        Button privateMessageBtn = (Button) mView.findViewById(R.id.private_message_btn);
        privateMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatManager.getInstance().startConversation(mEmpire.getKey(), new ChatManager.ConversationStartedListener() {
                    @Override
                    public void onConversationStarted(ChatConversation conversation) {
                        // TODO
                    }
                });

                dismiss();
            }
        });

        return new StyledDialog.Builder(getActivity())
            .setTitle(mEmpire.getDisplayName())
            .setView(mView)
            .create();
    }
}
