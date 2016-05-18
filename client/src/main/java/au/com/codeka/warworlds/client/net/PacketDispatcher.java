package au.com.codeka.warworlds.client.net;

import com.squareup.wire.WireField;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.util.eventbus.EventBus;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Packet;

/**
 * Helper class for dispatching packets to the event bus.
 */
public class PacketDispatcher {
  private static final Log log = new Log("PacketDispatcher");
  private final Collection<Field> pktFields;

  public PacketDispatcher() {
    pktFields = new ArrayList<>();
    for (Field field : Packet.class.getFields()) {
      if (field.isAnnotationPresent(WireField.class)) {
        pktFields.add(field);
      }
    }
  }

  public void dispatch(Packet pkt) {
    for (Field field : pktFields) {
      try {
        Object value = field.get(pkt);
        if (value != null) {
          App.i.getEventBus().publish(value);
        }
      } catch (IllegalAccessException e) {
        // Should never happen.
        log.error("Could not inspect packet field: %s", field.getName(), e);
      }
    }
  }
}
