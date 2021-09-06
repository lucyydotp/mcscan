package net.lucypoulton.mcscan

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.util.*

@Serializable
data class ServerListPingResponse(
    val version: ServerVersion,
    @Serializable(with = ChatSerialiser::class) val description: String,
    val favicon: String? = null,
    val players: PlayerCount
) {

    @Serializable
    data class ServerVersion(val name: String, val protocol: Int)

    @Serializable
    data class PlayerCount(val max: Int, val online: Int, val sample: Array<Player> = emptyArray()) {
        @Serializable
        data class Player(val name: String, @Serializable(with = UUIDSerializer::class) val id: UUID)
    }
}

