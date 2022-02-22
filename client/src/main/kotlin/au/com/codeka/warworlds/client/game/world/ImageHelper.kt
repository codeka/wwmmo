package au.com.codeka.warworlds.client.game.world

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.DisplayMetrics
import android.widget.ImageView
import au.com.codeka.warworlds.client.net.ServerUrl.url
import au.com.codeka.warworlds.client.opengl.DimensionResolver
import au.com.codeka.warworlds.client.util.Callback
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Building
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.common.proto.Planet
import au.com.codeka.warworlds.common.proto.Star
import com.squareup.picasso.Picasso
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target
import java.util.*

/**
 * Helper class to get the URL for the various images (stars, planets, empire shields, etc).
 */
object ImageHelper {
  private val log = Log("ImageHelper")
  private fun getDensityName(densityDpi: Int): String {
    return if (densityDpi > DisplayMetrics.DENSITY_XXHIGH) {
      "xxxhdpi"
    } else if (densityDpi > DisplayMetrics.DENSITY_XHIGH) {
      "xxhdpi"
    } else if (densityDpi > DisplayMetrics.DENSITY_HIGH) {
      "xhdpi"
    } else if (densityDpi > DisplayMetrics.DENSITY_MEDIUM) {
      "hdpi"
    } else if (densityDpi > DisplayMetrics.DENSITY_LOW) {
      "mdpi"
    } else {
      "ldpi"
    }
  }

  fun getStarImageUrl(context: Context, star: Star, width: Int, height: Int): String {
    val dpi = getDensityName(context.resources.displayMetrics.densityDpi)
    return String.format(Locale.ENGLISH, "%srender/star/%d/%dx%d/%s.png",
        url, star.id, width, height, dpi)
  }

  fun getPlanetImageUrl(
      context: Context, star: Star, planetIndex: Int, width: Int, height: Int): String {
    val dpi = getDensityName(context.resources.displayMetrics.densityDpi)
    return String.format(Locale.ENGLISH, "%srender/planet/%d/%d/%dx%d/%s.png",
        url, star.id, planetIndex, width, height, dpi)
  }

  fun getEmpireImageUrl(
      context: Context, empire: Empire?, width: Int, height: Int): String {
    val dpi = getDensityName(context.resources.displayMetrics.densityDpi)
    return String.format(Locale.ENGLISH, "%srender/empire/%d/%dx%d/%s.png",
        url, if (empire?.id == null) 0 else empire.id, width, height,
        dpi)
  }

  /** Gets the URL for fetching an empire's logo, with exact pixel dimensions (a.k.a. mdpi).  */
  fun getEmpireImageUrlExactDimens(empire: Empire, width: Int, height: Int): String {
    return String.format(Locale.ENGLISH, "%srender/empire/%d/%dx%d/mdpi.png",
        url, empire.id, width, height)
  }

  /**
   * Bind an image with the given URL to the given [ImageView].
   */
  fun bindImage(view: ImageView?, imageUrl: String?) {
    Picasso.get()
        .load(imageUrl)
        .into(view)
  }

  /**
   * Bind an image with the given star to the given [ImageView].
   */
  fun bindStarIcon(view: ImageView, star: Star) {
    val resolver = DimensionResolver(view.context)
    val width = resolver.px2dp(view.layoutParams.width.toFloat()).toInt()
    val height = resolver.px2dp(view.layoutParams.height.toFloat()).toInt()
    bindImage(view, getStarImageUrl(view.context, star, width, height))
  }

  /** Bind the given star icon to the given span in a [SpannableStringBuilder].  */
  fun bindStarIcon(
      ssb: SpannableStringBuilder, startIndex: Int, endIndex: Int, context: Context,
      star: Star?, size: Int,
      imageLoadedCallback: Callback<SpannableStringBuilder>) {
    if (star == null) {
      return
    }
    Picasso.get()
        .load(getStarImageUrl(context, star, size, size))
        .into(object : Target {
          override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom) {
            ssb.setSpan(
                ImageSpan(context, bitmap), startIndex, endIndex,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            imageLoadedCallback.run(ssb)
          }

          override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
            // TODO: handle
          }

          override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
        })
  }

  /**
   * Bind an empire's shield image to the given [ImageView].
   */
  fun bindEmpireShield(view: ImageView, empire: Empire?) {
    val resolver = DimensionResolver(view.context)
    val width = resolver.px2dp(view.layoutParams.width.toFloat()).toInt()
    val height = resolver.px2dp(view.layoutParams.height.toFloat()).toInt()
    bindImage(view, getEmpireImageUrl(view.context, empire, width, height))
  }

  /**
   * Bind a planet's image to the given [ImageView].
   */
  fun bindPlanetIcon(view: ImageView, star: Star?, planet: Planet?) {
    if (star == null || planet == null) {
      return
    }
    val resolver = DimensionResolver(view.context)
    val width = resolver.px2dp(view.layoutParams.width.toFloat()).toInt()
    val height = resolver.px2dp(view.layoutParams.height.toFloat()).toInt()
    bindImage(view, getPlanetImageUrl(
        view.context, star, star.planets.indexOf(planet), width, height))
  }
}