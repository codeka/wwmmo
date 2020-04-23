package au.com.codeka.warworlds.client.net

import android.os.Build
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.concurrency.RunnableTask
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.game.world.ChatManager
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.game.world.StarManager
import au.com.codeka.warworlds.client.util.GameSettings
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.debug.PacketDebug
import au.com.codeka.warworlds.common.net.PacketDecoder
import au.com.codeka.warworlds.common.net.PacketEncoder
import au.com.codeka.warworlds.common.proto.*
import au.com.codeka.warworlds.common.proto.LoginResponse.LoginStatus
import com.google.common.base.Preconditions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*

/** Represents our connection to the server.  */
class Server {
  private val packetDispatcher = PacketDispatcher()

  var currState = ServerStateEvent("", ServerStateEvent.ConnectionState.DISCONNECTED, null)
    private set

  /** The socket that's connected to the server. Null if we're not connected.  */
  private var gameSocket: Socket? = null

  /** The PacketEncoder we'll use to send packets. Null if we're not connected.  */
  private var packetEncoder: PacketEncoder? = null

  /** The PacketDecoder we'll use to receive packets. Null if we're not connected.  */
  private var packetDecoder: PacketDecoder? = null

  /** A queue for storing packets while we attempt to reconnect. Will be null if we're connected.  */
  private var queuedPackets: Queue<Packet>? = null

  /** A list of callbacks waiting for hello to complete.  */
  private var waitingForHello: ArrayList<Runnable>? = ArrayList()

  /** A lock used to guard access to the web socket/queue.  */
  private val lock = Any()
  private var reconnectTimeMs = DEFAULT_RECONNECT_TIME_MS

  /** Connect to the server.  */
  fun connect() {
    GameSettings.addSettingChangedHandler { key: GameSettings.Key ->
      if (key == GameSettings.Key.SERVER) {
        // If you change SERVER, we'll want to clear the cookie.
        GameSettings.edit()
            .setString(GameSettings.Key.COOKIE, "")
            .commit()
      } else if (key == GameSettings.Key.COOKIE) {
        // We got a new cookie, try connecting again.
        disconnect()
      }
    }
    val cookie = GameSettings.getString(GameSettings.Key.COOKIE)
    if (cookie.isEmpty()) {
      log.warning("No cookie yet, not connecting.")
      return
    }
    updateState(ServerStateEvent.ConnectionState.CONNECTING, null)
    login(cookie)
  }

  /**
   * Queue the given runnable to run once we've completed the server handshake. The running will
   * be executed immediately if we've already get hello.
   */
  fun waitForHello(runnable: Runnable) {
    synchronized(lock) {
      if (waitingForHello == null) {
        runnable.run()
      } else {
        waitingForHello!!.add(runnable)
      }
    }
  }

  private fun login(cookie: String) {
    log.info("Fetching firebase instance ID...")
    App.i.taskRunner.runTask(FirebaseInstanceId.getInstance().instanceId)
        .then(object : RunnableTask.RunnableP<InstanceIdResult> {
          override fun run(instanceIdResult: InstanceIdResult) {
            log.info("Logging in: %s", ServerUrl.getUrl("/login"))
            val request = HttpRequest.Builder()
                .url(ServerUrl.getUrl("/login"))
                .method(HttpRequest.Method.POST)
                .body(LoginRequest.Builder()
                    .cookie(cookie)
                    .device_info(populateDeviceInfo(instanceIdResult))
                    .build().encode())
                .build()
            if (request.responseCode != 200) {
              if (request.responseCode >= 401 && request.responseCode < 500) {
                // Our cookie must not be valid, we'll clear it before trying again.
                GameSettings.edit()
                    .setString(GameSettings.Key.COOKIE, "")
                    .commit()
              }
              log.error(
                  "Error logging in, will try again: %d",
                  request.responseCode,
                  request.exception)
              disconnect()
            } else {
              val loginResponse = request.getBody(LoginResponse::class.java)
              if (loginResponse!!.status != LoginStatus.SUCCESS) {
                updateState(ServerStateEvent.ConnectionState.ERROR, loginResponse.status)
                log.error("Error logging in, got login status: %s", loginResponse.status)
                disconnect()
              } else {
                connectGameSocket(loginResponse)
              }
            }
          }
        }, Threads.BACKGROUND)
  }

  private fun connectGameSocket(loginResponse: LoginResponse?) {
    try {
      gameSocket = Socket()
      var host = loginResponse!!.host
      if (host == null) {
        host = ServerUrl.host
      }
      gameSocket!!.connect(InetSocketAddress(host, loginResponse.port))
      packetEncoder = PacketEncoder(gameSocket!!.getOutputStream(), packetEncodeHandler)
      packetDecoder = PacketDecoder(gameSocket!!.getInputStream(), packetDecodeHandler)
      val oldQueuedPackets = queuedPackets
      queuedPackets = null
      send(Packet.Builder()
          .hello(HelloPacket.Builder()
              .empire_id(loginResponse.empire.id)
              .our_star_last_simulation(StarManager.lastSimulationOfOurStar)
              .last_chat_time(ChatManager.i.lastChatTime)
              .build())
          .build())
      EmpireManager.onHello(loginResponse.empire)
      reconnectTimeMs = DEFAULT_RECONNECT_TIME_MS
      updateState(ServerStateEvent.ConnectionState.CONNECTED, loginResponse.status)
      while (oldQueuedPackets != null && !oldQueuedPackets.isEmpty()) {
        send(oldQueuedPackets.remove())
      }
      var waitingForHello: ArrayList<Runnable>?
      synchronized(lock) {
        waitingForHello = this.waitingForHello
        this.waitingForHello = null
      }
      if (waitingForHello != null) {
        for (r in waitingForHello!!) {
          r.run()
        }
      }
    } catch (e: IOException) {
      gameSocket = null
      log.error("Error connecting game socket, will try again.", e)
      disconnect()
    }
  }

  fun send(pkt: Packet) {
    synchronized(lock) {
      if (queuedPackets != null) {
        queuedPackets!!.add(pkt)
      } else {
        Preconditions.checkNotNull(packetEncoder)
        try {
          packetEncoder!!.send(pkt)
        } catch (e: IOException) {
          log.warning("Error encoding and sending packet.", e)
          disconnect()
        }
      }
    }
  }

  private fun disconnect() {
    synchronized(lock) {
      if (gameSocket != null) {
        try {
          gameSocket!!.close()
        } catch (e: IOException) {
          // ignore.
        }
        gameSocket = null
      }
      packetEncoder = null
      packetDecoder = null
      queuedPackets = ArrayDeque()
    }
    if (currState.state != ServerStateEvent.ConnectionState.ERROR) {
      updateState(ServerStateEvent.ConnectionState.DISCONNECTED, null)
      App.i.taskRunner.runTask(Runnable {
        reconnectTimeMs *= 2
        if (reconnectTimeMs > MAX_RECONNECT_TIME_MS) {
          reconnectTimeMs = MAX_RECONNECT_TIME_MS
        }
        connect()
      }, Threads.BACKGROUND, reconnectTimeMs.toLong())
    }
  }

  private val packetEncodeHandler = PacketEncoder.PacketHandler { packet: Packet?, encodedSize: Int ->
    val packetDebug = PacketDebug.getPacketDebug(packet, encodedSize)
    App.i.eventBus.publish(ServerPacketEvent(
        packet, encodedSize, ServerPacketEvent.Direction.Sent, packetDebug))
    log.debug(">> %s", packetDebug)
  }
  private val packetDecodeHandler: PacketDecoder.PacketHandler = object : PacketDecoder.PacketHandler {
    override fun onPacket(decoder: PacketDecoder, pkt: Packet, encodedSize: Int) {
      val packetDebug = PacketDebug.getPacketDebug(pkt, encodedSize)
      App.i.eventBus.publish(ServerPacketEvent(
          pkt, encodedSize, ServerPacketEvent.Direction.Received, packetDebug))
      log.debug("<< %s", packetDebug)
      packetDispatcher.dispatch(pkt)
    }

    override fun onDisconnect() {
      disconnect()
    }
  }

  private fun updateState(
      state: ServerStateEvent.ConnectionState,
      loginStatus: LoginStatus?) {
    currState = ServerStateEvent(ServerUrl.url, state, loginStatus)
    App.i.eventBus.publish(currState)
  }

  companion object {
    private val log = Log("Server")
    private const val DEFAULT_RECONNECT_TIME_MS = 1000
    private const val MAX_RECONNECT_TIME_MS = 30000
    private fun populateDeviceInfo(instanceIdResult: InstanceIdResult): DeviceInfo {
      return DeviceInfo.Builder()
          .device_build(Build.ID)
          .device_id(GameSettings.getString(GameSettings.Key.INSTANCE_ID))
          .device_manufacturer(Build.MANUFACTURER)
          .device_model(Build.MODEL)
          .device_version(Build.VERSION.RELEASE)
          .fcm_device_info(FcmDeviceInfo.Builder()
              .token(instanceIdResult.token)
              .device_id(instanceIdResult.id)
              .build())
          .build()
    }
  }
}