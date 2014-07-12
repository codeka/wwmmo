package au.com.codeka.warworlds.game.wormhole;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;

/** This is the dialog we display when you click to rename a wormhole. Renaming wormholes is actually free. */
public class RenameDialog extends DialogFragment {
    private View mView;
    private Star mStar;

    public void setWormhole(Star star) {
        mStar = star;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.wormhole_rename_dlg, null);

        EditText starNewName = (EditText) mView.findViewById(R.id.star_newname);
        TextView starName = (TextView) mView.findViewById(R.id.star_name);
        ImageView starIcon = (ImageView) mView.findViewById(R.id.star_icon);

        starName.setText(mStar.getName());
        starNewName.setText(mStar.getName());

        int imageSize = (int)(mStar.getSize() * mStar.getStarType().getImageScale() * 2);
        Sprite starSprite = StarImageManager.getInstance().getSprite(mStar, imageSize, true);
        starIcon.setImageDrawable(new SpriteDrawable(starSprite));

        starNewName.requestFocus();

        StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
        b.setView(mView);
        b.setNeutralButton("Rename", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onRenameClicked();
                dialog.dismiss();
            }
        });
        return b.create();
    }

    public void onRenameClicked() {
        EditText starNewName = (EditText) mView.findViewById(R.id.star_newname);
        final String newStarName = starNewName.getText().toString();

        StarManager.i.renameStar(null, mStar, newStarName);
    }
}
