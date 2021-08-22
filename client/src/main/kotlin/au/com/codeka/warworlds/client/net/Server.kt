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
import au.com.codeka.warworlds.common.sim.DesignDefinitions
import com.google.firebase.messaging.FirebaseMessaging
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import java.util.concurrent.atomic.AtomicLong

/** Represents our connection to the server. */
class Server {
  private val packetDispatcher = PacketDispatcher()

  companion object {
    private val log = Log("Server")
    private const val DEFAULT_RECONNECT_TIME_MS = 1000
    private const val MAX_RECONNECT_TIME_MS = 30000
  }

  var currState = ServerStateEvent("", ServerStateEvent.ConnectionState.DISCONNECTED, null)
    private set

  /** The socket that's connected to the server. Null if we're not connected.  */
  private var gameSocket: Socket? = null

  /** The PacketEncoder we'll use to send packets. Null if we're not connected.  */
  private var packetEncoder: PacketEncoder? = null

  /** The PacketDecoder we'll use to receive packets. Null if we're not connected.  */
  private var packetDecoder: PacketDecoder? = null

  /** A queue for storing packets while we attempt to reconnect. Will be null if we're connected. */
  private var queuedPackets: Queue<Packet>? = null

  /** A list of callbacks waiting for hello to complete.  */
  private var waitingForHello: ArrayList<Runnable>? = ArrayList()

  /** A lock used to guard access to the web socket/queue.  */
  private val lock = Any()

  /** A counter that is incremented every time we send an RPC, to identify the responses */
  private val rpcCounter = AtomicLong()
  private val pendingRpcs: HashMap<Long, PendingRpc> = HashMap()

  private var reconnectPending = false
  private var reconnectTimeMs = DEFAULT_RECONNECT_TIME_MS

  fun setup() {
    GameSettings.addSettingChangedHandler { key: GameSettings.Key ->
      if (key == GameSettings.Key.SERVER) {
        // If you change SERVER, we'll want to clear the cookie.
        GameSettings.edit()
            .setString(GameSettings.Key.COOKIE, "")
            .commit()
      } else if (key == GameSettings.Key.COOKIE) {
        // We got a new cookie, try connecting again (in 100ms).
        reconnectTimeMs = 100
        disconnect()
      }
    }

    connect()
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

  /** Connect to the server.  */
  private fun connect() {
    val cookie = GameSettings.getString(GameSettings.Key.COOKIE)
    if (cookie.isEmpty()) {
      log.warning("No cookie yet, not connecting.")
      return
    }
    updateState(ServerStateEvent.ConnectionState.CONNECTING, null)
    login(cookie)
  }

  private fun populateDeviceInfo(fcmToken: String): DeviceInfo {
    return DeviceInfo(
      device_build = Build.ID,
      device_id = GameSettings.getString(GameSettings.Key.INSTANCE_ID),
      device_manufacturer = Build.MANUFACTURER,
      device_model = Build.MODEL,
      device_version = Build.VERSION.RELEASE,
      fcm_device_info = FcmDeviceInfo(token = fcmToken))
  }

  private fun login(cookie: String) {
    log.info("Fetching firebase instance ID...")
    App.taskRunner.runTask(FirebaseMessaging.getInstance().token)
        .then(object : RunnableTask.RunnableP<String> {
          override fun run(param: String) {
            // Make sure we have the GoogleSignInAccount (if you've signed in) before sending it
            // to the server.
            val googleAccount = App.auth.futureAccount().get()

            log.info("Logging in: %s", ServerUrl.getUrl("/login"))
            val request = HttpRequest.Builder()
                .url(ServerUrl.getUrl("/login"))
                .method(HttpRequest.Method.POST)
                .body(LoginRequest(
                    cookie = cookie,
                    device_info = populateDeviceInfo(param),
                    id_token = googleAccount?.idToken)
                  .encode())
                .build()
            if (request.responseCode != 200) {
              if (request.responseCode in 401..499) {
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
              val loginResponse = request.getBody(LoginResponse::class.java)!!
              if (loginResponse.status != LoginStatus.SUCCESS) {
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

  private fun connectGameSocket(loginResponse: LoginResponse) {
    try {
      // Make sure we update our copy of the design definitions.
      DesignDefinitions.init(loginResponse.designs!!)

      val s = Socket()
      gameSocket = s
      var host = loginResponse.host
      if (host == null) {
        host = ServerUrl.host
      }
      s.connect(InetSocketAddress(host, loginResponse.port!!))
      packetEncoder = PacketEncoder(s.getOutputStream(), packetEncodeHandler)
      packetDecoder = PacketDecoder(s.getInputStream(), packetDecodeHandler)
      val oldQueuedPackets = queuedPackets
      queuedPackets = null
      send(Packet(
          hello = HelloPacket(
              empire_id = loginResponse.empire!!.id,
              our_star_last_simulation = StarManager.lastSimulationOfOurStar,
              last_chat_time = ChatManager.i.lastChatTime)))
      EmpireManager.onHello(loginResponse.empire!!)
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
        try {
          packetEncoder!!.send(pkt)
        } catch (e: IOException) {
          log.warning("Error encoding and sending packet.", e)
          disconnect()
        }
      }
    }
  }

  /**
   * Send an [RpcPacket] and wait for the response. This will block until a response is received,
   * so you should be sure to call this only from a background thread.
   */
  fun sendRpc(rpc: RpcPacket): RpcPacket {
    Threads.checkNotOnThread(Threads.UI)

    val id = rpcCounter.addAndGet(1L)
    val pendingRpc = PendingRpc(id)
    pendingRpcs[id] = pendingRpc

    send(Packet(rpc = rpc.copy(id = id)))

    synchronized(pendingRpc.lock) {
      pendingRpc.lock.wait()
      return pendingRpc.response!!
    }
  }

  /**
   * Handle responses to RPCs. We'll find the PendingRpc and notify it.
   */
  private fun handleRpcResponse(rpc: RpcPacket) {
    val pendingRpc = pendingRpcs[rpc.id] ?: return // TODO: should it be an error when there's none?
    synchronized(pendingRpc.lock) {
      pendingRpc.response = rpc
      pendingRpc.lock.notifyAll()
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
      synchronized(lock) {
        if (!reconnectPending) {
          reconnectPending = true
          App.taskRunner.runTask({
            reconnectTimeMs *= 2
            if (reconnectTimeMs > MAX_RECONNECT_TIME_MS) {
              reconnectTimeMs = MAX_RECONNECT_TIME_MS
            }
            connect()
            reconnectPending = false
          }, Threads.BACKGROUND, reconnectTimeMs.toLong())
        }
      }
    }
  }

  private val packetEncodeHandler = object : PacketEncoder.PacketHandler {
    override fun onPacket(packet: Packet, encodedSize: Int) {
      val packetDebug = PacketDebug.getPacketDebug(packet, encodedSize)
      App.eventBus.publish(ServerPacketEvent(
          packet, encodedSize, ServerPacketEvent.Direction.Sent, packetDebug))
      log.debug(">> %s", packetDebug)
    }
  }

  private val packetDecodeHandler: PacketDecoder.PacketHandler =
      object : PacketDecoder.PacketHandler {
    override fun onPacket(decoder: PacketDecoder, pkt: Packet, encodedSize: Int) {
      val packetDebug = PacketDebug.getPacketDebug(pkt, encodedSize)
      App.eventBus.publish(ServerPacketEvent(
          pkt, encodedSize, ServerPacketEvent.Direction.Received, packetDebug))
      log.debug("<< %s", packetDebug)

      if (pkt.rpc != null) {
        // We do some special-casing for RPCs
        handleRpcResponse(pkt.rpc!!)
      } else {
        packetDispatcher.dispatch(pkt)
      }
    }

    override fun onDisconnect() {
      disconnect()
    }
  }

  private fun updateState(
      state: ServerStateEvent.ConnectionState,
      loginStatus: LoginStatus?) {
    currState = ServerStateEvent(ServerUrl.url, state, loginStatus)
    App.eventBus.publish(currState)
  }

  class PendingRpc(val id: Long) {
    val lock = Object()
    var response: RpcPacket? = null
  }
}