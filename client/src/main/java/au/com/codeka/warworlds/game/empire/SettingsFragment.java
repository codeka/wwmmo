package au.com.codeka.warworlds.game.empire;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import javax.annotation.Nonnull;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpire;
import au.com.codeka.warworlds.ImagePickerHelper;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.RealmContext;
import au.com.codeka.warworlds.RunnableArg;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.TabManager;
import au.com.codeka.warworlds.WelcomeFragment;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.PurchaseManager;
import au.com.codeka.warworlds.model.ShieldManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.billing.IabException;
import au.com.codeka.warworlds.model.billing.IabHelper;
import au.com.codeka.warworlds.model.billing.IabResult;
import au.com.codeka.warworlds.model.billing.Purchase;
import au.com.codeka.warworlds.model.billing.SkuDetails;


public class SettingsFragment extends BaseFragment implements TabManager.Reloadable {
  private static final Log log = new Log("SettingsFragment");
  private View view;
  private ImagePickerHelper imagePickerHelper;

  private static final String PATREON_CLIENT_ID =
      "IiB6Hob1U-gh26y7kFXOFGXTyrxLjy1WyyAJ7x5R_ToXbhG-8KL-Q5mPfC1j7fED";

  @Override
  public void onStart() {
    super.onStart();
    imagePickerHelper = ((EmpireActivity) getActivity()).getImagePickerHelper();
    ShieldManager.eventBus.register(eventHandler);
  }

  @Override
  public void onStop() {
    super.onStop();
    ShieldManager.eventBus.unregister(eventHandler);
  }

  /**
   * Called when an empire's shield is updated, we'll have to refresh the list.
   */
  private Object eventHandler = new Object() {
    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      if (!event.kind.equals(ShieldManager.EmpireShield)) {
        return;
      }

      MyEmpire empire = EmpireManager.i.getEmpire();
      if (Integer.parseInt(empire.getKey()) == event.id) {
        ImageView currentShield = view.findViewById(R.id.current_shield);
        currentShield.setImageBitmap(
            EmpireShieldManager.i.getShield(getActivity(), EmpireManager.i.getEmpire()));
        ImageView currentShieldSmall = view.findViewById(R.id.current_shield_small);
        currentShieldSmall.setImageBitmap(
            EmpireShieldManager.i.getShield(getActivity(), EmpireManager.i.getEmpire()));
      }
    }
  };

  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    view = inflater.inflate(R.layout.empire_settings_tab, null);

    MyEmpire myEmpire = EmpireManager.i.getEmpire();

    TextView txt = view.findViewById(R.id.patreon_desc);
    final Button btn = view.findViewById(R.id.patreon_btn);
    if (myEmpire.getPatreonLevel() == BaseEmpire.PatreonLevel.NONE) {
      String html = "Connect your empire to your Patreon account in order to collect your" +
                    " benefits!<br/><br/><a href='https://www.patreon.com/codeka'>And of course," +
                    " click here to support me!</a>";
      txt.setText(Html.fromHtml(html));
      btn.setText("Connect to Patreon");
    } else {
      String html = "Thanks for your support! We'll refresh your support level daily, but if you " +
                    " want to refresh now, you can click below. <br/><br/><a" +
                    " href='https://www.patreon.com/codeka'>And of course, click here adjust" +
                    " your Patreon plan!</a>";
      txt.setText(Html.fromHtml(html));
      btn.setText("Refresh Patreon");
    }

    btn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onPatreonConnectClick();
      }
    });

    try {
      SkuDetails empireRenameSku = PurchaseManager.i.getInventory().getSkuDetails("rename_empire");
      if (empireRenameSku != null) {
        txt = view.findViewById(R.id.rename_desc);
        txt.setText(String.format(Locale.ENGLISH,
            txt.getText().toString(), empireRenameSku.getPrice()));
      }

      SkuDetails decorateEmpireSku =
          PurchaseManager.i.getInventory().getSkuDetails("decorate_empire");
      if (decorateEmpireSku != null) {
        txt = view.findViewById(R.id.custom_shield_desc);
        txt.setText(Html.fromHtml(String.format(Locale.ENGLISH,
            txt.getText().toString(), decorateEmpireSku.getPrice())));
        txt.setMovementMethod(LinkMovementMethod.getInstance());
      }

      SkuDetails resetEmpireSmallSku = PurchaseManager.i.getInventory().getSkuDetails(
          "reset_empire_small");
      SkuDetails resetEmpireBigSku = PurchaseManager.i.getInventory().getSkuDetails(
          "reset_empire_big");
      if (resetEmpireBigSku != null && resetEmpireSmallSku != null) {
        txt = view.findViewById(R.id.reset_desc);
        txt.setText(String.format(Locale.ENGLISH, txt.getText().toString(),
            resetEmpireSmallSku.getPrice(), resetEmpireBigSku.getPrice()));
      }
    } catch (IabException e) {
      log.error("Couldn't get SKU details!", e);
    }

    final EditText renameEdit = view.findViewById(R.id.rename);
    renameEdit.setText(myEmpire.getDisplayName());

    final Button renameBtn = view.findViewById(R.id.rename_btn);
    renameBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onRenameClick();
      }
    });

    ImageView currentShield = view.findViewById(R.id.current_shield);
    currentShield.setImageBitmap(
        EmpireShieldManager.i.getShield(getActivity(), EmpireManager.i.getEmpire()));
    ImageView currentShieldSmall = view.findViewById(R.id.current_shield_small);
    currentShieldSmall.setImageBitmap(
        EmpireShieldManager.i.getShield(getActivity(), EmpireManager.i.getEmpire()));

    final Button shieldChangeBtn = view.findViewById(R.id.shield_change_btn);
    shieldChangeBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onShieldChangeClick();
      }
    });

    final Button shieldSaveBtn = view.findViewById(R.id.save_btn);
    shieldSaveBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onShieldSaveClick();
      }
    });

    final Button empireResetBtn = view.findViewById(R.id.reset_empire_btn);
    empireResetBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        new StyledDialog.Builder(getActivity()).setMessage(Html.fromHtml(
            "Are you sure you want to reset your empire? This operation is <b>permanent and "
                + "non-reversible</b>!<br/><br/>Note: when you reset, your cash will be reset "
                + "as well (and you <em>will not</em> get the extra bonus starting cash)"))
            .setTitle("Reset Empire")
            .setPositiveButton("Reset Empire", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                onResetEmpireClick(dialog);
              }
            }).setNegativeButton("Cancel", null).create().show();
      }
    });

    imagePickerHelper = ((EmpireActivity) getActivity()).getImagePickerHelper();
    loadShieldImage();

    return view;
  }

  private void onRenameClick() {
    final Activity activity = getActivity();
    final EditText renameEdit = view.findViewById(R.id.rename);

    final String newName = renameEdit.getText().toString().trim();
    if (newName.equals(EmpireManager.i.getEmpire().getDisplayName())) {
      new StyledDialog.Builder(getActivity())
          .setMessage("Please enter the new name you want before clicking 'Rename'.")
          .setTitle("Rename Empire").setPositiveButton("OK", null).create().show();
      return;
    }

    try {
      PurchaseManager.i.launchPurchaseFlow(getActivity(), "rename_empire", new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, final Purchase info) {
          boolean isSuccess = result.isSuccess();
          if (result.isFailure() && result.getResponse()
              == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
            // If they've already purchased a rename_empire, but not reclaimed it, then we let them
            // through anyway.
            isSuccess = true;
          }

          if (isSuccess) {
            EmpireManager.i.getEmpire().rename(newName, info, new RunnableArg<Boolean>() {
              @Override
              public void run(Boolean success) {
                if (success) {
                  new StyledDialog.Builder(activity)
                      .setMessage("Empire name successfully changed to: \"" + newName + "\"")
                      .setPositiveButton("Close", null).create().show();

                  PurchaseManager.i.consume(info, new IabHelper.OnConsumeFinishedListener() {
                    @Override
                    public void onConsumeFinished(Purchase purchase, IabResult result) {
                      if (!result.isSuccess()) {
                        // TODO: revert?
                        return;
                      }
                    }
                  });
                } else {
                  new StyledDialog.Builder(activity).setMessage(
                      "An error has occurred changing your name, but you can try again"
                          + " without purchasing again. If it continues to not work, please file a"
                          + " support request with dean@war-worlds.com, and your money will be"
                          + " refunded.").setPositiveButton("OK", null).create().show();
                }
              }
            });
          }
        }
      });
    } catch (IabException e) {
      log.error("Couldn't get SKU details!", e);
      return;
    }
  }

  private void onPatreonConnectClick() {
    String redirectUrl = RealmContext.i.getCurrentRealm().getBaseUrl() + "empires/patreon";
    String stateStr = String.format(Locale.US, "%d", EmpireManager.i.getEmpire().getID());
    final String uri = "https://www.patreon.com/oauth2/authorize?response_type=code"
        + "&client_id=" + PATREON_CLIENT_ID
        + "&redirect_uri=" + Uri.encode(redirectUrl)
        + "&state=" + Uri.encode(stateStr);
    log.info("Opening URL: %s", uri);
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
    startActivity(intent);
  }

  private void onShieldChangeClick() {
    imagePickerHelper.chooseImage();
  }

  private void onShieldSaveClick() {
    final Activity activity = getActivity();
    final Bitmap bmp = imagePickerHelper.getImage();
    if (bmp == null) {
      return;
    }

    try {
      PurchaseManager.i.launchPurchaseFlow(getActivity(), "decorate_empire", new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, final Purchase info) {
          boolean isSuccess = result.isSuccess();
          if (result.isFailure() && result.getResponse()
              == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
            // If they've already purchased a rename_empire, but not reclaimed it, then we let them
            // through anyway.
            isSuccess = true;
          }

          if (isSuccess) {
            EmpireManager.i.getEmpire().changeShieldImage(bmp, info, new RunnableArg<Boolean>() {
              @Override
              public void run(Boolean success) {
                if (success) {
                  new StyledDialog.Builder(activity)
                      .setMessage("Shield has been successfully changed.")
                      .setPositiveButton("Close", null)
                      .create().show();

                  PurchaseManager.i.consume(info, new IabHelper.OnConsumeFinishedListener() {
                    @Override
                    public void onConsumeFinished(Purchase purchase, IabResult result) {
                      if (!result.isSuccess()) {
                        // TODO: revert?
                        return;
                      }
                    }
                  });
                } else {
                  new StyledDialog.Builder(activity)
                      .setMessage("An error has occurred changing your shield, but you can try again"
                          + " without purchasing again. If it continues to not work, please file a"
                          + " support request with dean@war-worlds.com, and your money will be"
                          + " refunded.")
                      .setPositiveButton("OK", null)
                      .create().show();
                }
              }
            });
          }
        }
      });
    } catch (IabException e) {
      log.error("Couldn't get SKU details!", e);
      return;
    }
  }

  private void onResetEmpireClick(final DialogInterface dialog) {
    final Activity activity = new Activity();
    final Button empireResetBtn = view.findViewById(R.id.reset_empire_btn);
    empireResetBtn.setEnabled(false);

    // based on how many stars they have, we'll purchase a different in-app purchase for this
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    myEmpire.requestStars(new MyEmpire.FetchStarsCompleteHandler() {
      @Override
      public void onComplete(List<Star> stars) {
        int numStarsWithColonies = 0;
        for (Star star : stars) {
          for (BaseColony colony : star.getColonies()) {
            if (colony.getEmpireKey() != null && colony.getEmpireKey().equals(myEmpire.getKey())) {
              numStarsWithColonies++;
              break;
            }
          }
        }

        if (numStarsWithColonies < 5) {
          doEmpireReset(null, null, new Runnable() {
            @Override
            public void run() {
              dialog.dismiss();
            }
          });
        } else {
          String skuName = "reset_empire_small";
          if (numStarsWithColonies > 10) {
            skuName = "resetEmpire_big";
          }

          final String finalSkuName = skuName;
          try {
            PurchaseManager.i.launchPurchaseFlow(getActivity(), finalSkuName,
                new IabHelper.OnIabPurchaseFinishedListener() {
                  @Override
                  public void onIabPurchaseFinished(IabResult result, final Purchase info) {
                    boolean isSuccess = result.isSuccess();
                    if (result.isFailure() && result.getResponse()
                        == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
                      // If they've already purchased a reset_empire, but not reclaimed it,
                      // then we let them
                      // through anyway.
                      isSuccess = true;
                    }

                    if (isSuccess) {
                      doEmpireReset(finalSkuName, info, new Runnable() {
                        @Override
                        public void run() {
                          dialog.dismiss();

                          new StyledDialog.Builder(activity)
                              .setMessage("Your empire has been reset.")
                              .setPositiveButton("Close", null).create().show();

                          PurchaseManager.i.consume(info, new IabHelper.OnConsumeFinishedListener() {
                            @Override
                            public void onConsumeFinished(Purchase purchase, IabResult result) {
                              if (!result.isSuccess()) {
                                // TODO: revert?
                                return;
                              }
                            }
                          });
                        }
                      });
                    }
                  }
                });
          } catch (IabException e) {
            log.error("Couldn't get SKU details!", e);
            return;
          }
        }
      }
    });
  }

  private void doEmpireReset(
      String skuName,
      Purchase purchaseInfo,
      @Nonnull final Runnable runnable) {
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    myEmpire.resetEmpire(skuName, purchaseInfo, new MyEmpire.EmpireResetCompleteHandler() {
      @Override
      public void onEmpireReset() {
        runnable.run();
        ServerGreeter.clearHello();

        // redirect you to the main page.
        startActivity(new Intent(getActivity(), WelcomeFragment.class));
      }

      @Override
      public void onResetFail(String reason) {
        runnable.run();

        Activity activity = getActivity();
        if (activity == null) {
          return;
        }
        new StyledDialog.Builder(activity)
            .setTitle("Error")
            .setMessage(reason)
            .setPositiveButton("OK", null)
            .create().show();

      }
    });
  }

  private void loadShieldImage() {
    Bitmap bmp = imagePickerHelper.getImage();
    if (bmp != null) {
      log.info("Got an image from the image picker");
      bmp = combineShieldImage(getActivity(), bmp);

      ImageView currentShield = view.findViewById(R.id.current_shield);
      currentShield.setImageBitmap(bmp);
      ImageView currentShieldSmall = view.findViewById(R.id.current_shield_small);
      currentShieldSmall.setImageBitmap(bmp);

      // and now we can enable the 'save' button
      view.findViewById(R.id.save_btn).setEnabled(true);
      ;
    } else {
      log.info("No image picked, loading as normal.");
    }
  }

  /**
   * Combines the given image with the base shield image.
   */
  public Bitmap combineShieldImage(Context context, Bitmap otherImage) {
    AssetManager assetManager = context.getAssets();
    InputStream ins;
    try {
      ins = assetManager.open("img/shield.png");
    } catch (IOException e) {
      // should never happen!
      return null;
    }

    Bitmap baseShield;
    try {
      BitmapFactory.Options opts = new BitmapFactory.Options();
      opts.inPurgeable = true;
      opts.inInputShareable = true;
      baseShield = BitmapFactory.decodeStream(ins, null, opts);
    } finally {
      try {
        ins.close();
      } catch (IOException e) {
      }
    }

    int width = baseShield.getWidth();
    int height = baseShield.getHeight();
    int[] pixels = new int[width * height];
    baseShield.getPixels(pixels, 0, width, 0, 0, width, height);

    float sx = (float) otherImage.getWidth() / (float) width;
    float sy = (float) otherImage.getHeight() / (float) height;
    for (int i = 0; i < pixels.length; i++) {
      if (pixels[i] == Color.MAGENTA) {
        int y = i / width;
        int x = i % width;
        pixels[i] = otherImage.getPixel((int) (x * sx), (int) (y * sy));
      }
    }

    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
  }

  /**
   * Called when the tab manager wants to reload us for whatever reason.
   */
  @Override
  public void reloadTab() {
    // check if we've got a shield image to load.
    loadShieldImage();
  }

  private interface ApplyPurchaseHandler {
    boolean onApplyPurchase(Purchase purchaseInfo);
  }
}
