package au.com.codeka.warworlds.client.net

import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Packet
import com.squareup.wire.WireField
import java.lang.reflect.Field
import java.util.*

/** Helper class for dispatching packets to the event bus. */
class PacketDispatcher {
  private val pktFields: MutableCollection<Field>
  fun dispatch(pkt: Packet?) {
    for (field in pktFields) {
      try {
        val value = field[pkt]
        if (value != null) {
          App.eventBus.publish(value)
        }
      } catch (e: IllegalAccessException) {
        // Should never happen.
        log.error("Could not inspect packet field: %s", field.name, e)
      }
    }
  }

  companion object {
    private val log = Log("PacketDispatcher")
  }

  init {
    pktFields = ArrayList()
    for (field in Packet::class.java.fields) {
      if (field.isAnnotationPresent(WireField::class.java)) {
        pktFields.add(field)
      }
    }
  }
}