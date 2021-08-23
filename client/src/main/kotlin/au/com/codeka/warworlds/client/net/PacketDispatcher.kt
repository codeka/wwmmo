package au.com.codeka.warworlds.client.net

import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.net.PacketHelper
import au.com.codeka.warworlds.common.proto.Packet
import com.squareup.wire.WireField
import java.lang.reflect.Field
import java.util.*
import kotlin.reflect.KProperty

/** Helper class for dispatching packets to the event bus. */
class PacketDispatcher {
  fun dispatch(pkt: Packet?) {
    for (prop in PacketHelper.properties.values) {
      try {
        val value = prop.getter.call(pkt)
        if (value != null) {
          App.eventBus.publish(value)
        }
      } catch (e: IllegalAccessException) {
        // Should never happen.
        log.error("Could not inspect packet field: %s", prop.name, e)
      }
    }
  }

  companion object {
    private val log = Log("PacketDispatcher")
  }
}