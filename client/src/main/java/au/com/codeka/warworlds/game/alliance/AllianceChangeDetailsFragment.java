package au.com.codeka.warworlds.game.alliance;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

import au.com.codeka.warworlds.ImagePickerHelper;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.AllianceShieldManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.ShieldManager;
import au.com.codeka.warworlds.ui.BaseFragment;

public class AllianceChangeDetailsFragment extends BaseFragment {
  private static final int CHOOSE_IMAGE_RESULT_ID = 2463;

  private Bitmap pickedImage;
  private View rootView;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.alliance_change_details, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    rootView = view;

    final Button changeNameBtn = view.findViewById(R.id.change_name_btn);
    changeNameBtn.setOnClickListener(v -> onChangeNameClick());

    final Button chooseImageBtn = view.findViewById(R.id.choose_image_btn);
    chooseImageBtn.setOnClickListener(v -> onChooseImageClick());

    final Button changeImageBtn = view.findViewById(R.id.change_image_btn);
    changeImageBtn.setOnClickListener(v -> onChangeImageClick());

    ImagePickerHelper imagePickerHelper = requireMainActivity().getImagePickerHelper();
    imagePickerHelper.registerImagePickedHandler(CHOOSE_IMAGE_RESULT_ID, imagePickedHandler);
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

    TextView allianceName = rootView.findViewById(R.id.alliance_name);
    allianceName.setText(myAlliance.getName());

    TextView allianceMembers = rootView.findViewById(R.id.alliance_num_members);
    allianceMembers.setText(String.format(Locale.US, "Members: %d • Stars: %d",
        myAlliance.getNumMembers(), myAlliance.getTotalStars()));

    final EditText newNameEdit = rootView.findViewById(R.id.new_name);
    newNameEdit.setText(myAlliance.getName());

    final EditText newDescriptionEdit = rootView.findViewById(R.id.new_description);
    newDescriptionEdit.setText(
        myAlliance.getDescription() == null ? "" : myAlliance.getDescription());

    TextView txt = rootView.findViewById(R.id.change_image_info);
    txt.setText(Html.fromHtml(txt.getText().toString()));

    Bitmap shield = AllianceShieldManager.i.getShield(requireContext(), myAlliance);
    ImageView currentShield = rootView.findViewById(R.id.current_image);
    currentShield.setImageBitmap(shield);
    ImageView currentShieldSmall = rootView.findViewById(R.id.current_image_small);
    currentShieldSmall.setImageBitmap(shield);
    ImageView allianceIcon = rootView.findViewById(R.id.alliance_icon);
    allianceIcon.setImageBitmap(shield);

    loadImage();
  }

  private void onChangeNameClick() {
    final EditText newNameEdit = rootView.findViewById(R.id.new_name);
    final EditText newDescriptionEdit = rootView.findViewById(R.id.new_description);

    final String newName = newNameEdit.getText().toString().trim();
    final String newDescription = newDescriptionEdit.getText().toString().trim();
    final Alliance myAlliance = (Alliance) EmpireManager.i.getEmpire().getAlliance();
    if (newName.equals(myAlliance.getName())
        && newDescription.equals(myAlliance.getDescription())) {
      new StyledDialog.Builder(requireContext())
          .setMessage(
              "Please enter the new name and/or description you want before clicking 'Change'.")
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
    requireMainActivity().getImagePickerHelper().chooseImage(CHOOSE_IMAGE_RESULT_ID);
  }

  private void onChangeImageClick() {
    if (pickedImage == null) {
      return;
    }

    int allianceID = ((Alliance) EmpireManager.i.getEmpire().getAlliance()).getID();

    ByteArrayOutputStream outs = new ByteArrayOutputStream();
    pickedImage.compress(CompressFormat.PNG, 90, outs);

    AllianceManager.i.requestChangeImage(allianceID, "", outs.toByteArray());
  }

  private void loadImage() {
    if (pickedImage == null) {
      return;
    }

    ImageView currentImage = rootView.findViewById(R.id.current_image);
    currentImage.setImageBitmap(pickedImage);
    ImageView currentImageSmall = rootView.findViewById(R.id.current_image_small);
    currentImageSmall.setImageBitmap(pickedImage);

    // and now we can enable the 'Change' button
    (rootView.findViewById(R.id.change_image_btn)).setEnabled(true);
  }

  private final ImagePickerHelper.ImagePickedHandler imagePickedHandler = (bitmap) -> {
    pickedImage = bitmap;
    loadImage();
  };

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      fullRefresh();
    }
  };
}
