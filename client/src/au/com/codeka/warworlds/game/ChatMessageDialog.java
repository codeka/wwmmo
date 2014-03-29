package au.com.codeka.warworlds.game;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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

import com.google.protobuf.InvalidProtocolBufferException;

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

        Button reportBtn = (Button) mView.findViewById(R.id.report_btn);
        reportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onReportClick();
                dismiss();
            }
        });

        Button privateMessageBtn = (Button) mView.findViewById(R.id.private_message_btn);
        privateMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatManager.i.startConversation(mEmpire.getKey(), new ChatManager.ConversationStartedListener() {
                    @Override
                    public void onConversationStarted(ChatConversation conversation) {
                        ((ChatActivity) getActivity()).startConversation(mEmpire.getKey());
                        dismiss();
                    }
                });
            }
        });

        return new StyledDialog.Builder(getActivity())
            .setTitle(mEmpire.getDisplayName())
            .setView(mView)
            .create();
    }

    private void onReportClick() {
        final Context context = getActivity();
        new StyledDialog.Builder(context)
                .setTitle("Report abuse")
                .setMessage("Reporting an empire for abusive chat may result in that empire being banned from chatting."
                           +" Are you sure you want to report "+mEmpire.getDisplayName()+" for abuse?.")
                .setPositiveButton("Report", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doReport(context);
                        dialog.dismiss();
                    }
                }).setNegativeButton("Cancel", null)
                .create().show();
    }

    private void doReport(Context context) {
        ChatManager.i.reportMessageForAbuse(context, mChatMessage);
    }
}
