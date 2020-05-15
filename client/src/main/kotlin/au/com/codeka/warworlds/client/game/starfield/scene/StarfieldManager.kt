package au.com.codeka.warworlds.client.game.starfield.scene

import android.content.Context
import androidx.collection.LongSparseArray
import androidx.core.util.Pair
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.concurrency.Threads.Companion.checkOnThread
import au.com.codeka.warworlds.client.game.starfield.StarfieldGestureDetector
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.client.game.world.StarManager
import au.com.codeka.warworlds.client.net.ServerStateEvent
import au.com.codeka.warworlds.client.opengl.*
import au.com.codeka.warworlds.client.opengl.Camera.CameraUpdateListener
import au.com.codeka.warworlds.client.util.eventbus.EventHandler
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.Vector2
import au.com.codeka.warworlds.common.Vector3
import au.com.codeka.warworlds.common.proto.*
import au.com.codeka.warworlds.common.proto.Design.DesignType
import au.com.codeka.warworlds.common.proto.Star.CLASSIFICATION
import au.com.codeka.warworlds.common.sim.StarHelper
import java.util.*
import kotlin.math.roundToInt

/**
 * [StarfieldManager] manages the starfield view that we display in the main activity. You can
 * use it to switch between the normal view (that [StarfieldScreen] cares about) and the
 * move-fleet view, etc.
 */
class StarfieldManager(renderSurfaceView: RenderSurfaceView) {
  companion object {
    /** Number of milliseconds between updates to moving fleets.  */
    private const val UPDATE_MOVING_FLEETS_TIME_MS = 2000L
    private val log = Log("StarfieldManager")
  }

  interface TapListener {
    fun onStarTapped(star: Star)
    fun onFleetTapped(star: Star, fleet: Fleet)
    fun onEmptySpaceTapped()
  }

  private val scene: Scene = renderSurfaceView.createScene()
  private val camera: Camera = renderSurfaceView.camera
  private val gestureDetector: StarfieldGestureDetector
  private val context: Context = renderSurfaceView.context
  private val tapListeners = ArrayList<TapListener>()

  // The selected star/fleet. If selectedFleet is non-null, selectedStar will be the star that
  // fleet is on.
  private var selectedStar: Star? = null
  /** Gets the selected fleet (or null if not fleet is selected).  */
  var selectedFleet: Fleet? = null
    private set
  private var centerSectorX: Long = 0
  private var centerSectorY: Long = 0
  private var sectorRadius = 0

  // The following are the top/left/right/bottom of the rectangle of sectors that we're currently
  // listening to for updates. Generally this will be the same as centreSectorX,centreSectorY +
  // sectorRadius, but not always (in particular, just before you call updateSectorBounds())
  private var sectorLeft: Long = 0
  private var sectorTop: Long = 0
  private var sectorRight: Long = 0
  private var sectorBottom: Long = 0

  // Because we don't want to update the sector bounds over and over, we'll ensure only one pending
  // update is happening at once. This keeps track of whether we need to re-update later on or not.
  private var pendingSectorTop: Long? = null
  private var pendingSectorLeft: Long? = null
  private var pendingSectorRight: Long? = null
  private var pendingSectorBottom: Long? = null
  private var pendingRemoveAllSectors: Boolean? = null
  private var sectorsUpdating: Boolean = false

  /** A mapping of IDs to [SceneObject] representing stars and fleets.  */
  private val sceneObjects = LongSparseArray<SceneObject>()

  /** A mapping of sector x,y coordinates to the list of [SceneObject]s for that sector.  */
  private val sectorSceneObjects: MutableMap<Pair<Long, Long>, ArrayList<SceneObject>> = HashMap()

  /** A mapping of sector x,y coordinates to the [BackgroundSceneObject]s for that sector.  */
  private val backgroundSceneObjects: MutableMap<Pair<Long, Long>, BackgroundSceneObject> = HashMap()

  /**
   * The "selection indicator" which is added to a star or fleet scene object to indicate the
   * current selection. It's never null, but it might not be attached to anything.
   */
  private val selectionIndicatorSceneObject: SelectionIndicatorSceneObject

  private val gestureListener = object : StarfieldGestureDetector.Callback {
    override fun onScroll(dx: Float, dy: Float) {
      camera.translate(-dx, dy)
    }

    override fun onFling(vx: Float, vy: Float) {
      camera.fling(vx, -vy)
    }

    override fun onScale(factor: Float) {
      camera.zoom(factor)
      synchronized(backgroundSceneObjects) {
        for (backgroundSceneObject in backgroundSceneObjects.values) {
          backgroundSceneObject.setZoomAmount(camera.zoomAmount)
        }
      }
    }

    override fun onTap(x: Float, y: Float) {
      var selected: SceneObjectInfo? = null

      // Work out which object (if any) you tapped on.
      synchronized(scene.lock) {
        var selectedSceneObject: SceneObject? = null
        val outVec = FloatArray(4)
        val pos = Vector3()
        val tap = Vector3(x.toDouble(), y.toDouble(), 0.0)
        for (i in 0 until scene.rootObject.numChildren) {
          val so = scene.rootObject.getChild(i)
          if (so?.tapTargetRadius == null) {
            continue
          }
          so.project(camera.viewProjMatrix, outVec)
          pos.reset(
              (outVec[0] + 1.0f) * 0.5f * camera.screenWidth.toDouble(),
              (-outVec[1] + 1.0f) * 0.5f * camera.screenHeight.toDouble(), 0.0)
          if (Vector3.distanceBetween(pos, tap) < so.tapTargetRadius!!) {
            selectedSceneObject = so
          }
        }
        if (selectedSceneObject != null) {
          selected = selectedSceneObject.tag as SceneObjectInfo?
        }
      }
      if (selectedFleet != null && (selected == null || selected!!.fleet == null)) {
        setSelectedFleet(null, null)
      } else if (selected != null && selected!!.fleet != null) {
        setSelectedFleet(selected!!.star, selected!!.fleet)
      } else if (selectedStar != null && selected == null) {
        setSelectedStar(null)
      } else if (selected != null) {
        setSelectedStar(selected!!.star)
      }
    }
  }

  init {
    gestureDetector = StarfieldGestureDetector(renderSurfaceView, gestureListener)
    renderSurfaceView.setScene(scene)
    selectionIndicatorSceneObject = SelectionIndicatorSceneObject(scene.dimensionResolver)
  }

  fun create() {
    App.eventBus.register(eventListener)
    if (App.server.currState.state === ServerStateEvent.ConnectionState.CONNECTED) {
      // If we're already connected, then call onConnected now.
      onConnected()
    }
    camera.setCameraUpdateListener(cameraUpdateListener)
    gestureDetector.create()
    App.taskRunner.runTask(
        updateMovingFleetsRunnable,
        Threads.UI,
        UPDATE_MOVING_FLEETS_TIME_MS)
  }

  fun destroy() {
    gestureDetector.destroy()
    camera.setCameraUpdateListener(null)
    App.eventBus.unregister(eventListener)
  }

  fun addTapListener(tapListener: TapListener) {
    tapListeners.add(tapListener)
  }

  fun removeTapListener(tapListener: TapListener) {
    tapListeners.remove(tapListener)
  }

  /** Gets the selected star (or null if no star is selected).  */
  fun getSelectedStar(): Star? {
    return if (selectedFleet != null) {
      // If we have a fleet selected, the selectedStar will be the star for that fleet but from
      // the user's point of view, there's no actual star selected...
      null
    } else selectedStar
  }

  /**
   * Gets the [SceneObject] that's being used to render the star with the given ID, or if
   * the given star isn't being rendered by us, returns null.
   */
  fun getStarSceneObject(starId: Long): SceneObject? {
    return sceneObjects[starId]
  }

  /** Sets the star we have selected to the given value (or unselects if star is null).  */
  fun setSelectedStar(star: Star?) {
    if (star != null) {
      selectionIndicatorSceneObject.setSize(60f, 60f)
      val sceneObject = sceneObjects[star.id]
      sceneObject!!.addChild(selectionIndicatorSceneObject)
    } else if (selectionIndicatorSceneObject.parent != null) {
      selectionIndicatorSceneObject.parent!!.removeChild(selectionIndicatorSceneObject)
    }
    selectedFleet = null
    selectedStar = star
    if (star != null) {
      for (tapListener in tapListeners) {
        tapListener.onStarTapped(star)
      }
    } else {
      for (tapListener in tapListeners) {
        tapListener.onEmptySpaceTapped()
      }
    }
  }

  /**
   * Sets the fleet we have selected to the given value (or unselects it if fleet is null).
   *
   * @param star The star the fleet is coming from.
   * @param fleet The fleet.
   */
  fun setSelectedFleet(star: Star?, fleet: Fleet?) {
    checkOnThread(Threads.UI)
    log.debug("setSelectedFleet(%d %s, %d %sx%.1f)",
        if (star == null) 0 else star.id,
        if (star == null) "?" else star.name,
        if (fleet == null) 0 else fleet.id,
        if (fleet == null) "?" else fleet.design_type,
        if (fleet == null) 0 else fleet.num_ships)
    if (fleet != null) {
      selectionIndicatorSceneObject.setSize(60f, 60f)
      val sceneObject = sceneObjects[fleet.id]
      if (sceneObject != null) {
        sceneObject.addChild(selectionIndicatorSceneObject)
      } else {
        log.warning("No SceneObject for fleet #%d", fleet.id)
      }
    } else if (selectionIndicatorSceneObject.parent != null) {
      selectionIndicatorSceneObject.parent!!.removeChild(selectionIndicatorSceneObject)
    }
    selectedStar = star
    selectedFleet = fleet
    if (star != null && fleet != null) {
      for (tapListener in tapListeners) {
        tapListener.onFleetTapped(star, fleet)
      }
    } else {
      for (tapListener in tapListeners) {
        tapListener.onEmptySpaceTapped()
      }
    }
  }

  /** Warp the view to center on the given star. */
  fun warpTo(star: Star) {
    warpTo(star.sector_x, star.sector_y, star.offset_x - 512.0f, star.offset_y - 512.0f)
  }

  private fun warpTo(sectorX: Long, sectorY: Long, offsetX: Float, offsetY: Float) {
    centerSectorX = sectorX
    centerSectorY = sectorY
    sectorRadius = 1
    log.info("WarpTo: $centerSectorX, $centerSectorY - $offsetX, $offsetY")
    updateSectorBounds(centerSectorX - sectorRadius, centerSectorY - sectorRadius,
        centerSectorX + sectorRadius, centerSectorY + sectorRadius, true)
    camera.warpTo(
        scene.dimensionResolver.dp2px(-offsetX),
        scene.dimensionResolver.dp2px(offsetY))
  }

  /**
   * This is called after we're connected to the server. We'll warp to the connected empire's home
   * star.
   */
  fun onConnected() {
    // Shouldn't be null after we're connected to the server.
    warpTo(EmpireManager.getMyEmpire().home_star)
  }

  /**
   * Update the sector "bounds", that is the area of the universe that we're currently looking at.
   * We'll need to remove stars that have gone out of bounds, add a background for stars that are
   * now in-bounds, and ask the server to keep us updated of the new stars.
   *
   * @param removeAllSectors If true, we'll clear out all the existing sectors and reload them all.
   *        If false, we'll keep sectors that we already have an not re-request those. Only pass
   *        true if you have not modified centerSectorX and centerSectorY
   */
  private fun updateSectorBounds(
      left: Long, top: Long, right: Long, bottom: Long, removeAllSectors: Boolean) {
    // If we're already centered on this bounds, just skip.
    if (left == sectorLeft && top == sectorTop && right == sectorRight && bottom == sectorBottom) {
      return
    }

    if (sectorsUpdating) {
      pendingSectorLeft = left
      pendingSectorTop = top
      pendingSectorRight = right
      pendingSectorBottom = bottom
      pendingRemoveAllSectors = pendingRemoveAllSectors ?: false || removeAllSectors
      return
    }
    sectorsUpdating = true

    log.info("updateSectorBounds($left, $top, $right, $bottom) current: " +
        "$sectorLeft, $sectorTop, $sectorRight, $sectorBottom")


    if (removeAllSectors) {
      val sectorsToRemove = ArrayList(sectorSceneObjects.keys)
      for (coord in sectorsToRemove) {
        removeSector(coord)
      }

      // Create the background for all of the new sectors.
      for (sy in top..bottom) {
        for (sx in left..right) {
          createSectorBackground(sx, sy)
        }
      }
    } else {
      for (sy in sectorTop..sectorBottom) {
        for (sx in sectorLeft..sectorRight) {
          if (sx < left || sy < top || sx > right || sy > bottom) {
            removeSector(sx, sy)
          }
        }
      }

      // Create the background for all of the new sectors.
      for (sy in top..bottom) {
        for (sx in left..right) {
          if (sx < sectorLeft || sy < sectorTop || sx > sectorRight || sy > sectorBottom) {
            createSectorBackground(sx, sy)
          }
        }
      }
    }

    sectorTop = top
    sectorLeft = left
    sectorBottom = bottom
    sectorRight = right

    // Tell the server we want to watch these new sectors, it'll send us back all the stars we
    // don't have yet.
    App.taskRunner.runTask(Runnable {
      App.server.send(Packet.Builder()
          .watch_sectors(WatchSectorsPacket.Builder()
              .top(top).left(left).bottom(bottom).right(right).build())
          .build())
    }, Threads.BACKGROUND)

    // We'll wait at least one second before attempting to update the sector bounds again.
    App.taskRunner.runTask(Runnable {
      sectorsUpdating = false
      if (pendingSectorBottom != null) {
        val left = pendingSectorLeft!! ; pendingSectorLeft = null
        val top = pendingSectorTop!! ; pendingSectorTop = null
        val right = pendingSectorRight!! ; pendingSectorRight = null
        val bottom = pendingSectorBottom!! ; pendingSectorBottom = null
        val removeAllSectors = pendingRemoveAllSectors!! ; pendingRemoveAllSectors = null
        updateSectorBounds(left, top, right, bottom, removeAllSectors)
      }
    }, Threads.UI, 1000)
  }

  /**
   * This is called when the camera moves to a new (x,y) coord. We'll want to check whether we
   * need to re-calculate the bounds and warp the camera back to the center.
   *
   * @param x The distance the camera has translated from the origin in the X direction.
   * @param y The distance the camera has translated from the origin in the Y direction.
   */
  private fun onCameraTranslate(x: Float, y: Float) {
    val newCentreX = centerSectorX + scene.dimensionResolver.px2dp(x / 1024.0f).roundToInt()
    val newCentreY = centerSectorY + scene.dimensionResolver.px2dp(y / 1024.0f).roundToInt()
    val top = newCentreY - sectorRadius
    val left = newCentreX - sectorRadius
    val bottom = newCentreY + sectorRadius
    val right = newCentreX + sectorRadius
    if (top != sectorTop || left != sectorLeft || bottom != sectorBottom || right != sectorRight) {
      log.info("onCameraTranslate: bounds=%d,%d - %d,%d", left, top, right, bottom)
      updateSectorBounds(left, top, right, bottom, false)
    }
  }

  /** Create a [Sprite] to represent the given [Star].  */
  private fun createStarSprite(star: Star): Sprite {
    val uvTopLeft = getStarUvTopLeft(star)
    val sprite = scene.createSprite(SpriteTemplate.Builder()
        .shader(scene.spriteShader)
        .texture(scene.textureManager.loadTexture("stars/stars.png"))
        .uvTopLeft(uvTopLeft)
        .uvBottomRight(Vector2(
            uvTopLeft.x + if (star.classification == CLASSIFICATION.NEUTRON) 0.5f else 0.25f,
            uvTopLeft.y + if (star.classification == CLASSIFICATION.NEUTRON) 0.5f else 0.25f))
        .build(), "Star:${star.id}:${star.name}")
    if (star.classification == CLASSIFICATION.NEUTRON) {
      sprite.setSize(90.0f, 90.0f)
    } else {
      sprite.setSize(40.0f, 40.0f)
    }
    return sprite
  }

  /** Create a [Sprite] to represent the given [Fleet].  */
  fun createFleetSprite(fleet: Fleet): Sprite {
    val sprite = scene.createSprite(SpriteTemplate.Builder()
        .shader(scene.spriteShader)
        .texture(scene.textureManager.loadTexture(getFleetTexture(fleet)))
        .build(), "Fleet:${fleet.id}:${fleet.design_type}:${fleet.num_ships}")
    sprite.setSize(64.0f, 64.0f)
    return sprite
  }

  /** Called when a star is updated, we may need to update the sprite for it.  */
  private fun updateStar(star: Star) {
    var oldStar: Star? = null
    var container = sceneObjects[star.id]
    if (container == null) {
      container = SceneObject(scene.dimensionResolver, "Star:${star.id}:${star.name}")
      container.setClipRadius(80.0f)
      container.setTapTargetRadius(80.0f)
      addSectorSceneObject(Pair.create(star.sector_x, star.sector_y), container)
      val x = (star.sector_x - centerSectorX) * 1024.0f + (star.offset_x - 512.0f)
      val y = (star.sector_y - centerSectorY) * 1024.0f + (star.offset_y - 512.0f)
      container.translate(x, -y)
    } else {
      oldStar = (container.tag as SceneObjectInfo?)!!.star

      // Check to see that stuff we care about has actually changed. If any stuff hasn't changed
      // then don't update the star.
      if (willRenderTheSame(oldStar, star)) {
        return
      }

      // Temporarily remove the container, and clear out it's children. We'll re-add them all.
      synchronized(scene.lock) {
        if (container.parent != null) {
          scene.rootObject.removeChild(container)
        }
      }
      container.removeAllChildren()
    }

    // Be sure to update the container's tag with the new star info.
    container.tag = SceneObjectInfo(star)
    val sprite = createStarSprite(star)
    container.addChild(sprite)
    val text = scene.createText(star.name)
    text.translate(0.0f, -24.0f)
    text.setTextSize(16f)
    text.translate(-text.getTextWidth() / 2.0f, 0.0f)
    container.addChild(text)
    attachEmpireFleetIcons(container, star)
    synchronized(scene.lock) {
      scene.rootObject.addChild(container)
      sceneObjects.put(star.id, container)
    }
    detachNonMovingFleets(oldStar, star)
    attachMovingFleets(star)
  }

  /**
   * Quick sanity check to see whether the two stars given will render exactly the same. Presumably
   * they're just different versions of the same star and often stuff that we don't care about
   * rendering here will have changed (e.g. a building has completed building). In the case that
   * they are going to render the same, we can save a lot of time by not changing the star.
   *
   * <p>We make a few assumptions here to simplify:
   *   1. Stars never move (their offset_x,offset_y etc are never checked)
   *   2. Stars never change classification
   *   3. Stars with different IDs will always render differently
   */
  private fun willRenderTheSame(lhs: Star, rhs: Star): Boolean {
    // Obviously a different ID will be rendered differently.
    if (lhs.id != rhs.id) {
      return false
    }

    if (lhs.name != rhs.name) {
      return false
    }

    // If the moving fleets have changed, tha counts as a difference we care about.
    for (lhsFleet in lhs.fleets) {
      // If it's moving in LHS and not in RHS then it's a difference
      if (lhsFleet.state != Fleet.FLEET_STATE.MOVING) {
        continue
      }

      var movingInRhs = false
      for (rhsFleet in rhs.fleets) {
        if (rhsFleet.id == lhsFleet.id && rhsFleet.state == Fleet.FLEET_STATE.MOVING) {
          movingInRhs = true
          break
        }
      }
      if (!movingInRhs) {
        return false
      }
    }

    // And same with moving fleets in RHS that are not moving in LHS
    for (rhsFleet in rhs.fleets) {
      if (rhsFleet.state != Fleet.FLEET_STATE.MOVING) {
        continue
      }

      var movingInLhs = false
      for (lhsFleet in rhs.fleets) {
        if (rhsFleet.id == lhsFleet.id && lhsFleet.state == Fleet.FLEET_STATE.MOVING) {
          movingInLhs = true
          break
        }
      }
      if (!movingInLhs) {
        return false
      }
    }

    // OK it's a difference we don't care about. They would render the same way
    return true
  }

  /** Attach the empire labels and fleet counts to the given sprite container for the given star. */
  private fun attachEmpireFleetIcons(container: SceneObject, star: Star) {
    val empires: MutableMap<Long, EmpireIconInfo> = TreeMap()
    for (planet in star.planets) {
      if (planet.colony == null || planet.colony.empire_id == null) {
        continue
      }
      val empire = EmpireManager.getEmpire(planet.colony.empire_id)
      if (empire != null) {
        var iconInfo = empires[empire.id]
        if (iconInfo == null) {
          iconInfo = EmpireIconInfo(empire)
          empires[empire.id] = iconInfo
        }
        iconInfo.numColonies += 1
      }
    }
    for (fleet in star.fleets) {
      if (fleet.empire_id == null || fleet.state == Fleet.FLEET_STATE.MOVING) {
        // Ignore native fleets, and moving fleets, which we'll draw them separately.
        continue
      }
      val empire = EmpireManager.getEmpire(fleet.empire_id)
      if (empire != null) {
        var iconInfo = empires[empire.id]
        if (iconInfo == null) {
          iconInfo = EmpireIconInfo(empire)
          empires[empire.id] = iconInfo
        }
        if (fleet.design_type == DesignType.FIGHTER) {
          iconInfo.numFighterShips += Math.ceil(fleet.num_ships.toDouble()).toInt()
        } else {
          iconInfo.numNonFighterShips += Math.ceil(fleet.num_ships.toDouble()).toInt()
        }
      }
    }
    var i = 0
    for ((empireId, iconInfo) in empires) {
      val pt = Vector2(0.0, 30.0)
      pt.rotate((-(Math.PI / 4.0)).toFloat() * (i + 1).toDouble())

      // Add the empire's icon
      val sprite = scene.createSprite(SpriteTemplate.Builder()
          .shader(scene.spriteShader)
          .texture(scene.textureManager.loadTextureUrl(
              ImageHelper.getEmpireImageUrlExactDimens(context, iconInfo.empire, 64, 64)))
          .build(), "Empire:$empireId")
      sprite.translate(pt.x.toFloat() + 10.0f, pt.y.toFloat())
      sprite.setSize(20.0f, 20.0f)
      container.addChild(sprite)

      // Add the counts.
      val text = if (iconInfo.numFighterShips == 0 && iconInfo.numNonFighterShips == 0) {
        String.format(Locale.ENGLISH, "%d", iconInfo.numColonies)
      } else if (iconInfo.numColonies == 0) {
        String.format(Locale.ENGLISH, "[%d, %d]",
            iconInfo.numFighterShips, iconInfo.numNonFighterShips)
      } else {
        String.format(Locale.ENGLISH, "%d ● [%d, %d]",
            iconInfo.numColonies, iconInfo.numFighterShips, iconInfo.numNonFighterShips)
      }
      val empireCounts = scene.createText(text)
      val offset = empireCounts.getTextWidth() * 0.666f / 2.0f + 14.0f
      empireCounts.translate(pt.x.toFloat() + offset, pt.y.toFloat())
      container.addChild(empireCounts)
      i++
    }
  }

  /** Attach moving fleets to the given sprite container for the given star.  */
  private fun attachMovingFleets(star: Star) {
    for (fleet in star.fleets) {
      if (fleet.state == Fleet.FLEET_STATE.MOVING) {
        attachMovingFleet(star, fleet)
      }
    }
  }

  private fun attachMovingFleet(star: Star, fleet: Fleet) {
    val destStar = StarManager.getStar(fleet.destination_star_id)
    if (destStar == null) {
      log.warning("Cannot attach moving fleet, destination star is null.")
      return
    }
    var container = sceneObjects[fleet.id]
    if (container == null) {
      log.info("attaching moving fleet: new container")
      container =
          SceneObject(
              scene.dimensionResolver, "Fleet:${fleet.id}:${fleet.design_type}:${fleet.num_ships}")
      container.setClipRadius(80.0f)
      container.setTapTargetRadius(80.0f)
      container.tag = SceneObjectInfo(star, fleet, destStar)
      addSectorSceneObject(Pair.create(star.sector_x, star.sector_y), container)
      val pos = getMovingFleetPosition(star, destStar, fleet)
      container.translate(pos.x.toFloat(), (-pos.y).toFloat())
    } else {
      log.info("attaching moving fleet: existing container")

      // Temporarily remove the container, and clear out it's children. We'll re-add them all.
      synchronized(scene.lock) {
        scene.rootObject.removeChild(container)
        sceneObjects.remove(fleet.id)
      }
      container.removeAllChildren()
    }
    val sprite = createFleetSprite(fleet)
    container.addChild(sprite)

    // Rotate the sprite
    val dir = StarHelper.directionBetween(star, destStar)
    dir.normalize()
    val angle = Vector2.angleBetween(dir, Vector2(0.0, -1.0))
    sprite.setRotation(angle, 0f, 0f, 1f)
    synchronized(scene.lock) {
      scene.rootObject.addChild(container)
      sceneObjects.put(fleet.id, container)
    }
  }

  /** Detach any non-moving fleets that may have been moving previously.  */
  private fun detachNonMovingFleets(oldStar: Star?, star: Star) {
    // Remove any fleets that are no longer moving.
    for (fleet in star.fleets) {
      if (fleet.state != Fleet.FLEET_STATE.MOVING) {
        val sceneObject = sceneObjects[fleet.id]
        sceneObject?.let { detachNonMovingFleet(fleet, it) }
      }
    }

    // Make sure to also do the same for fleets that are no longer on the star.
    if (oldStar != null) {
      for (oldFleet in oldStar.fleets) {
        val sceneObject = sceneObjects[oldFleet.id]
            ?: // no need to see if we need to remove it if it doesn't exist...
            continue
        var removed = true
        for (fleet in star.fleets) {
          if (fleet.id == oldFleet.id) {
            removed = false
            break
          }
        }
        if (removed) {
          detachNonMovingFleet(oldFleet, sceneObject)
        }
      }
    }
  }

  private fun detachNonMovingFleet(fleet: Fleet, sceneObject: SceneObject) {
    // If you had it selected, we'll need to un-select it.
    if (selectedFleet != null && selectedFleet!!.id == fleet.id) {
      App.taskRunner.runTask(Runnable { setSelectedFleet(null, null) }, Threads.UI)
    }

    val soi: SceneObjectInfo = sceneObject.tag as SceneObjectInfo
    val coord = Pair<Long, Long>(soi.star.sector_x, soi.star.sector_y)

    synchronized(scene.lock) {
      removeSectorSceneObject(coord, sceneObject)
      sceneObject.parent!!.removeChild(sceneObject)
      sceneObjects.remove(fleet.id)
    }
  }

  /** Get the current position of the given moving fleet.  */
  private fun getMovingFleetPosition(star: Star, destStar: Star?, fleet: Fleet?): Vector2 {
    val src = Vector2(
        ((star.sector_x - centerSectorX) * 1024.0f + (star.offset_x - 512.0f)).toDouble(),
        ((star.sector_y - centerSectorY) * 1024.0f + (star.offset_y - 512.0f)).toDouble())
    val dest = Vector2(
        ((destStar!!.sector_x - centerSectorX) * 1024.0f + (destStar.offset_x - 512.0f)).toDouble(),
        ((destStar.sector_y - centerSectorY) * 1024.0f + (destStar.offset_y - 512.0f)).toDouble())
    val totalTime = fleet!!.eta - fleet.state_start_time
    val elapsedTime = System.currentTimeMillis() - fleet.state_start_time
    val timeFraction: Double = elapsedTime.toFloat() / totalTime.toDouble()

    // Subtract 100, we'll add 50 after because we want the fleet to start offset from the star and
    // finish offset as well.
    var distance = src.distanceTo(dest) - 100.0
    distance *= timeFraction
    distance += 50.0
    if (distance < 50.0) {
      distance = 50.0
    }
    dest.subtract(src)
    dest.normalize()
    dest.scale(distance)
    dest.add(src)
    return dest
  }

  private fun createSectorBackground(sectorX: Long, sectorY: Long) {
    val backgroundSceneObject = BackgroundSceneObject(scene, sectorX, sectorY)
    backgroundSceneObject.setZoomAmount(camera.zoomAmount)
    backgroundSceneObject.translate(
        -(centerSectorX - sectorX) * 1024.0f,
        (centerSectorY - sectorY) * 1024.0f)
    val xy = Pair.create(sectorX, sectorY)
    addSectorSceneObject(xy, backgroundSceneObject)
    synchronized(backgroundSceneObjects) { backgroundSceneObjects.put(xy, backgroundSceneObject) }
    synchronized(scene.lock) { scene.rootObject.addChild(backgroundSceneObject) }
  }

  /**
   * Goes through all of the moving fleets and updates their position. This is called every now and
   * then to make sure fleets are moving.
   */
  private val updateMovingFleetsRunnable: Runnable = object : Runnable {
    override fun run() {
      synchronized(scene.lock) {
        for (i in 0 until sceneObjects.size()) {
          val sceneObjectInfo = sceneObjects.valueAt(i).tag as SceneObjectInfo?
          if (sceneObjectInfo?.fleet != null) {
            updateMovingFleet(sceneObjects.valueAt(i))
          }
        }
      }
      App.taskRunner.runTask(this, Threads.UI, UPDATE_MOVING_FLEETS_TIME_MS)
    }
  }

  /**
   * Update the given [SceneObject] that represents a moving fleet. Should already be in the
   * scene.lock.
   */
  private fun updateMovingFleet(fleetSceneObject: SceneObject) {
    val sceneObjectInfo = fleetSceneObject.tag as SceneObjectInfo?
    val pos = getMovingFleetPosition(
        sceneObjectInfo!!.star, sceneObjectInfo.destStar, sceneObjectInfo.fleet)
    fleetSceneObject.setTranslation(pos.x.toFloat(), (-pos.y).toFloat())
  }

  private fun addSectorSceneObject(sectorCoord: Pair<Long, Long>, obj: SceneObject) {
    synchronized(sectorSceneObjects) {
      var objects = sectorSceneObjects[sectorCoord]
      if (objects == null) {
        objects = ArrayList()
        sectorSceneObjects[sectorCoord] = objects
      }
      objects.add(obj)
    }
  }

  private fun removeSectorSceneObject(sectorCoord: Pair<Long, Long>, obj: SceneObject) {
    synchronized(sectorSceneObjects) {
      val objects = sectorSceneObjects[sectorCoord]
      objects?.remove(obj)
    }
  }

  /** Remove all objects in the given sector from the scene.  */
  private fun removeSector(sectorCoord: Pair<Long, Long>) {
    var objects: ArrayList<SceneObject>?
    synchronized(sectorSceneObjects) {
      objects = sectorSceneObjects.remove(sectorCoord)
    }
    if (objects == null) {
      return
    }
    synchronized(scene.lock) {
      for (obj in objects!!) {
        val sceneObjectInfo = obj.tag as SceneObjectInfo?
        if (sceneObjectInfo != null) {
          if (sceneObjectInfo.fleet != null) {
            log.info("Removing fleet: ${sceneObjectInfo.fleet.id} ${sceneObjectInfo.fleet.design_type}")
            sceneObjects.remove(sceneObjectInfo.fleet.id)
          } else {
            log.info("Removing star")
            sceneObjects.remove(sceneObjectInfo.star.id)
          }
        }
        scene.rootObject.removeChild(obj)
      }
    }
    synchronized(backgroundSceneObjects) {
      backgroundSceneObjects.remove(sectorCoord)
    }
  }

  private fun removeSector(sx: Long, sy: Long) {
    removeSector(Pair(sx, sy))
  }

  /**
   * Move the sector at the given coords by the given offsetX and offsetY.
   *
   * <p>This is useful when move the camera or warping: we can reuse sectors just by moving them
   * over instead of destroying and re-creating them.
   */
  private fun offsetSector(sectorCoord: Pair<Long, Long>, offsetX: Float, offsetY: Float) {
    var objects: ArrayList<SceneObject>?
    synchronized(sectorSceneObjects) {
      objects = sectorSceneObjects[sectorCoord]
    }
    if (objects == null) {
      return
    }
    synchronized(scene.lock) {
      for (obj in objects!!) {
        val sceneObjectInfo = obj.tag as SceneObjectInfo?
        if (sceneObjectInfo != null) {
          val sceneObject = if (sceneObjectInfo.fleet != null) {
            sceneObjects[sceneObjectInfo.fleet.id]
          } else {
            sceneObjects[sceneObjectInfo.star.id]
          }

          sceneObject?.translate(offsetX, offsetY)
        }
      }
    }
    var backgroundSceneObject: BackgroundSceneObject?
    synchronized(backgroundSceneObjects) {
      backgroundSceneObject = backgroundSceneObjects[sectorCoord]
    }
    if (backgroundSceneObject != null) {
      backgroundSceneObject!!.translate(offsetX, offsetY)
    }
  }

  private fun getStarUvTopLeft(star: Star): Vector2 {
    return when (star.classification) {
      CLASSIFICATION.BLACKHOLE -> Vector2(0.0, 0.5)
      CLASSIFICATION.BLUE -> Vector2(0.25, 0.5)
      CLASSIFICATION.NEUTRON -> Vector2(0.0, 0.0)
      CLASSIFICATION.ORANGE -> Vector2(0.0, 0.75)
      CLASSIFICATION.RED -> Vector2(0.25, 0.75)
      CLASSIFICATION.WHITE -> Vector2(0.5, 0.75)
      CLASSIFICATION.WORMHOLE -> Vector2(0.0, 0.0)
      CLASSIFICATION.YELLOW -> Vector2(0.5, 0.5)
      else ->         // Shouldn't happen!
        Vector2(0.5, 0.0)
    }
  }

  private fun getFleetTexture(fleet: Fleet): String {
    // TODO: why have this switch, we already have the sprites?
    return when (fleet.design_type) {
      DesignType.COLONY_SHIP -> "sprites/colony.png"
      DesignType.SCOUT -> "sprites/scout.png"
      DesignType.FIGHTER -> "sprites/fighter.png"
      DesignType.TROOP_CARRIER -> "sprites/troopcarrier.png"
      DesignType.WORMHOLE_GENERATOR -> "sprites/wormhole-generator.png"
      DesignType.TANKER -> "sprites/tanker.png"
      DesignType.UNKNOWN_DESIGN ->         // Shouldn't happen, the rest are reserved for buildings.
        "sprites/hq.png"
      else -> "sprites/hq.png"
    }
  }

  private val eventListener: Any = object : Any() {
    @EventHandler
    fun onServerStateEvent(event: ServerStateEvent) {
      if (event.state === ServerStateEvent.ConnectionState.CONNECTED) {
        onConnected()
      }
    }

    @EventHandler(thread = Threads.BACKGROUND)
    fun onStar(star: Star) {
      // Make sure this star is one that we're tracking.
      if (star.sector_x < sectorLeft || star.sector_x > sectorRight || star.sector_y < sectorTop ||
          star.sector_y > sectorBottom) {
        return
      }
      updateStar(star)
      if (selectedFleet != null && selectedStar != null && selectedStar!!.id == star.id) {
        // We have a fleet selected and it's one on this star. Make sure we update the selected
        // fleet as well.
        var found = false
        for (fleet in star.fleets) {
          if (fleet.id == selectedFleet!!.id) {
            selectedFleet = fleet
            selectedStar = star
            found = true
          }
        }
        if (!found) {
          // The fleet's been removed from star, it's no longer selected!
          setSelectedFleet(null, null)
        }
      } else if (selectedStar != null && selectedStar!!.id == star.id) {
        // The star that we have selected has been updated.
        selectedStar = star
      }
    }
  }

  private val cameraUpdateListener: CameraUpdateListener = object : CameraUpdateListener {
    override fun onCameraTranslate(x: Float, y: Float, dx: Float, dy: Float) {
      App.taskRunner.runTask(
          Runnable { onCameraTranslate(x, y) },
          Threads.BACKGROUND)
    }
  }

  private class EmpireIconInfo internal constructor(val empire: Empire) {
    var numColonies = 0
    var numFighterShips = 0
    var numNonFighterShips = 0

  }

  private class SceneObjectInfo {
    val star: Star
    val fleet: Fleet?
    val destStar: Star?

    internal constructor(star: Star) {
      this.star = star
      fleet = null
      destStar = null
    }

    internal constructor(star: Star, fleet: Fleet?, destStar: Star?) {
      this.star = star
      this.fleet = fleet
      this.destStar = destStar
    }
  }
}