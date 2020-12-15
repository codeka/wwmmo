package au.com.codeka.warworlds.game;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.android.billingclient.api.Purchase;

import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.PurchaseManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;

public class StarRenameDialog extends DialogFragment {
  private Purchase purchase;
  private View view;
  private Star star;

  public void setPurchaseInfo(Purchase purchase) {
    this.purchase = purchase;
  }

  public void setStar(Star star) {
    this.star = star;
  }

  @SuppressLint("InflateParams")
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    LayoutInflater inflater = getActivity().getLayoutInflater();
    view = inflater.inflate(R.layout.star_rename_dlg, null);

    EditText starNewName = view.findViewById(R.id.star_newname);
    TextView starName = view.findViewById(R.id.star_name);
    ImageView starIcon = view.findViewById(R.id.star_icon);

    starName.setText(star.getName());
    starNewName.setText(star.getName());

    int imageSize = (int) (star.getSize() * star.getStarType().getImageScale() * 2);
    Sprite starSprite = StarImageManager.getInstance().getSprite(star, imageSize, true);
    starIcon.setImageDrawable(new SpriteDrawable(starSprite));

    starNewName.requestFocus();

    StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
    b.setView(view);
    b.setNeutralButton("Rename", (dialog, which) -> {
      onRenameClicked();
      dialog.dismiss();
    });
    return b.create();
  }

  public void onRenameClicked() {
    EditText starNewName = view.findViewById(R.id.star_newname);
    final String newStarName = starNewName.getText().toString();

    StarManager.i.renameStar(purchase, star, newStarName,
        (star, successful, errorMessage) -> PurchaseManager.i.consume(purchase, null));
  }
}
