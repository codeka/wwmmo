package au.com.codeka.warworlds.game;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;

/**
 * Lets you edit the notes you've written for an object (build request, fleet, etc).
 */
public class NotesDialog extends DialogFragment {
    private NotesChangedHandler mNotesChangedHandler;
    private String mOriginalNotes;
    private View mView;

    public void setup(String notes, NotesChangedHandler handler) {
        mOriginalNotes = notes;
        mNotesChangedHandler = handler;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.notes_dlg, null);

        final EditText notesText = (EditText) mView.findViewById(R.id.notes);
        if (mOriginalNotes != null) {
            notesText.setText(mOriginalNotes);
        }

        StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
        b.setView(mView);
        b.setTitle("Notes");
        b.setNegativeButton("Cancel", null);
        b.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
                if (mNotesChangedHandler != null) {
                    if (mOriginalNotes == null || !mOriginalNotes.equals(notesText.getText().toString())) {
                        mNotesChangedHandler.onNotesChanged(notesText.getText().toString());
                    }
                }
            }
        });

        return b.create();
    }

    public interface NotesChangedHandler {
        void onNotesChanged(String notes);
    }
}
