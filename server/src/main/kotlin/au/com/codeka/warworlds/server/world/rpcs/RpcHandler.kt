package au.com.codeka.warworlds.server.world.rpcs

import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.common.proto.RpcPacket
import au.com.codeka.warworlds.server.world.WatchableObject

interface RpcHandler {
  fun handle(empire: WatchableObject<Empire>, rpc: RpcPacket): RpcPacket
}