package au.com.codeka.warworlds.game.alliance;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.text.Html;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.ImagePickerHelper;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.WelcomeFragment;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.AllianceShieldManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.ShieldManager;

public class AllianceChangeDetailsActivity extends BaseActivity {
  private ImagePickerHelper imagePickerHelper = new ImagePickerHelper(this);

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.alliance_change_details);

    final Button changeNameBtn = findViewById(R.id.change_name_btn);
    changeNameBtn.setOnClickListener(v -> onChangeNameClick());

    final Button chooseImageBtn = findViewById(R.id.choose_image_btn);
    chooseImageBtn.setOnClickListener(v -> onChooseImageClick());

    final Button changeImageBtn = findViewById(R.id.change_image_btn);
    changeImageBtn.setOnClickListener(v -> onChangeImageClick());
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    imagePickerHelper.onActivityResult(requestCode, resultCode, data);

    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void onResumeFragments() {
    super.onResumeFragments();

    ServerGreeter.waitForHello(this, (success, greeting) -> {
      if (!success) {
        startActivity(new Intent(AllianceChangeDetailsActivity.this, WelcomeFragment.class));
      } else {
        fullRefresh();
      }
    });
  }

  @Override
  public void onStart() {
    super.onStart();
    ShieldManager.eventBus.register(eventHandler);
  }

  @Override
  public void onStop() {
    super.onStop();
    ShieldManager.eventBus.unregister(eventHandler);
  }

  private void fullRefresh() {
    MyEmpire myEmpire = EmpireManager.i.getEmpire();
    Alliance myAlliance = (Alliance) myEmpire.getAlliance();

    TextView allianceName = findViewById(R.id.alliance_name);
    allianceName.setText(myAlliance.getName());

    TextView allianceMembers = findViewById(R.id.alliance_num_members);
    allianceMembers.setText(String.format(Locale.US, "Members: %d • Stars: %d",
        myAlliance.getNumMembers(), myAlliance.getTotalStars()));

    final EditText newNameEdit = findViewById(R.id.new_name);
    newNameEdit.setText(myAlliance.getName());

    final EditText newDescriptionEdit = findViewById(R.id.new_description);
    newDescriptionEdit.setText(
        myAlliance.getDescription() == null ? "" : myAlliance.getDescription());

    TextView txt = findViewById(R.id.change_image_info);
    txt.setText(Html.fromHtml(txt.getText().toString()));

    Bitmap shield = AllianceShieldManager.i.getShield(this, myAlliance);
    ImageView currentShield = findViewById(R.id.current_image);
    currentShield.setImageBitmap(shield);
    ImageView currentShieldSmall = findViewById(R.id.current_image_small);
    currentShieldSmall.setImageBitmap(shield);
    ImageView allianceIcon = findViewById(R.id.alliance_icon);
    allianceIcon.setImageBitmap(shield);

    loadImage();
  }

  private void onChangeNameClick() {
    final EditText newNameEdit = findViewById(R.id.new_name);
    final EditText newDescriptionEdit = findViewById(R.id.new_description);

    final String newName = newNameEdit.getText().toString().trim();
    final String newDescription = newDescriptionEdit.getText().toString().trim();
    final Alliance myAlliance = (Alliance) EmpireManager.i.getEmpire().getAlliance();
    if (newName.equals(myAlliance.getName())
        && newDescription.equals(myAlliance.getDescription())) {
      new StyledDialog.Builder(this)
          .setMessage("Please enter the new name and/or description you want before clicking 'Change'.")
          .setTitle("Rename Alliance")
          .setPositiveButton("OK", null)
          .create().show();
      return;
    }

    if (!newName.equals(myAlliance.getName())) {
      AllianceManager.i.requestChangeName(myAlliance.getID(), "", newName);
    }

    if (!newDescription.equals(myAlliance.getDescription())) {
      AllianceManager.i.requestChangeDescription(myAlliance.getID(), "", newDescription);
    }
  }
  private void onChooseImageClick() {
    //imagePickerHelper.chooseImage();
  }

  private void onChangeImageClick() {
    Bitmap bmp = imagePickerHelper.getImage();
    if (bmp == null) {
      return;
    }

    int allianceID = ((Alliance) EmpireManager.i.getEmpire().getAlliance()).getID();

    ByteArrayOutputStream outs = new ByteArrayOutputStream();
    bmp.compress(CompressFormat.PNG, 90, outs);

    AllianceManager.i.requestChangeImage(allianceID, "", outs.toByteArray());
  }

  private void loadImage() {
    Bitmap bmp = imagePickerHelper.getImage();
    if (bmp == null) {
      return;
    }

    ImageView currentImage = findViewById(R.id.current_image);
    currentImage.setImageBitmap(bmp);
    ImageView currentImageSmall = findViewById(R.id.current_image_small);
    currentImageSmall.setImageBitmap(bmp);

    // and now we can enable the 'Change' button
    (findViewById(R.id.change_image_btn)).setEnabled(true);
  }

  private Object eventHandler = new Object() {
    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      fullRefresh();
    }
  };
}
