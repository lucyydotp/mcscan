/*
 * Copyright © 2021 Lucy Poulton
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.lucypoulton.mcscan.serialiser

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