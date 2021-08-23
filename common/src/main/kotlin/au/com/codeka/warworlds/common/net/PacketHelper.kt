package au.com.codeka.warworlds.common.net

import au.com.codeka.warworlds.common.proto.Packet
import com.google.common.collect.ImmutableMap
import com.squareup.wire.WireField
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/** Helper for working with packets. Getting their fields and so on. */
object PacketHelper {
  val properties = findProperties()

  private fun findProperties(): ImmutableMap<String, KProperty<*>> {
    val cls: KClass<Packet> = Packet::class
    val properties = HashMap<String, KProperty<*>>()
    for (prop in cls.memberProperties) {
      val field = prop.javaField ?: continue
      for (annotation in field.annotations) {
        if (annotation is WireField) {
          properties[prop.name] = prop
          break
        }
      }
    }

    return ImmutableMap.copyOf(properties)
  }
}
