package au.com.codeka.warworlds.game;

import android.app.Activity;
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
import au.com.codeka.warworlds.model.PurchaseManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.billing.IabHelper;
import au.com.codeka.warworlds.model.billing.IabResult;
import au.com.codeka.warworlds.model.billing.Purchase;

public class StarRenameDialog extends DialogFragment {
    private Purchase mPurchase;
    private View mView;
    private Star mStar;

    public void setPurchaseInfo(Purchase purchase) {
        mPurchase = purchase;
    }
    public void setStar(Star star) {
        mStar = star;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.star_rename_dlg, null);

        EditText starNewName = (EditText) mView.findViewById(R.id.star_newname);
        TextView starName = (TextView) mView.findViewById(R.id.star_name);
        ImageView starIcon = (ImageView) mView.findViewById(R.id.star_icon);

        starName.setText(mStar.getName());
        starNewName.setText(mStar.getName());

        int imageSize = (int)(mStar.getSize() * mStar.getStarType().getImageScale() * 2);
        Sprite starSprite = StarImageManager.getInstance().getSprite(getActivity(), mStar, imageSize, true);
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
        final Activity activity = getActivity();

        PurchaseManager.getInstance().consume(mPurchase, new IabHelper.OnConsumeFinishedListener() {
            @Override
            public void onConsumeFinished(Purchase purchase, IabResult result) {
                if (!result.isSuccess()) {
                    // TODO: error
                    return;
                }

                StarManager.getInstance().renameStar(activity, purchase, mStar, newStarName);
            }
        });
    }
}
