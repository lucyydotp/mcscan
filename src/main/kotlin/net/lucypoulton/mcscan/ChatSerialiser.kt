package net.lucypoulton.mcscan

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * A serialiser for a Minecraft chat component.
 */
object ChatSerialiser : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("Chat", PrimitiveKind.STRING)

    fun deserializeElement(element: JsonElement) : String {
        return when (element) {
            is JsonPrimitive -> element.content
            is JsonArray -> element.joinToString { child -> deserializeElement(child) }
            is JsonObject -> {
                deserializeElement(element["text"] ?: element["translate"]!!) +
                        if (element.containsKey("extra")) deserializeElement(element["extra"]!!) else ""
            }
        }
    }

    override fun deserialize(decoder: Decoder): String = deserializeElement((decoder as JsonDecoder).decodeJsonElement())

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}