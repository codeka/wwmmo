package au.com.codeka.warworlds.common.net

/** Flags that get applied to each packet, which tell the other side how they can decode it. */
object PacketFlags {
  const val NONE = 0
  const val COMPRESSED = 1
}
