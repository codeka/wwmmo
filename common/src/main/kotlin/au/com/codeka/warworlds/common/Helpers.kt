package au.com.codeka.warworlds.common

import com.google.common.collect.ImmutableMap
import com.squareup.wire.WireField
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

fun findProperties(kClass: KClass<out Any>): ImmutableMap<String, KProperty<*>> {
  val properties = HashMap<String, KProperty<*>>()
  for (prop in kClass.memberProperties) {
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
