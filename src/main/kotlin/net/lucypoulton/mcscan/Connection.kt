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

package net.lucypoulton.mcscan

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.lucypoulton.mcscan.serialiser.ChatSerialiser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.ProtocolException
import java.net.Socket
import java.net.SocketTimeoutException

class Connection @Throws(ConnectException::class) constructor(var socket: Socket, val timeout: Boolean = true)  {

    private val json = Json { ignoreUnknownKeys = true }
    private var protocolVersion = 0

    init {
        if (timeout) socket.soTimeout = 500
    }

    constructor(address: String, port: UShort, timeout: Boolean = true): this(Socket(address, port.toInt()), timeout)

    enum class State(val value: Int) {
        PRELOGIN(0),
        STATUS(1),
        LOGIN(2),
        PLAY(3)
    }

    fun writeString(stream: OutputStream, value: String) {
        val bytes = value.encodeToByteArray()

        Leb128.writeVarInt(bytes.size, stream)
        stream.write(bytes)
    }

    fun readString(stream: InputStream): String {
        val length = Leb128.readVarInt(stream)
        return stream.readNBytes(length).decodeToString()
    }

    fun writePacket(packetId: Int, data: ByteArray) {
        val buffer = ByteArrayOutputStream()
        val stream = socket.getOutputStream()

        Leb128.writeVarInt(packetId, buffer)
        buffer.write(data)

        Leb128.writeVarInt(buffer.size(), stream)
        buffer.writeTo(stream)
    }

    /**
     * @return the length of the packet to read
     */
    @Throws(SocketTimeoutException::class)
    fun readPacket(): InputStream {
        val stream = socket.getInputStream()
        val length = Leb128.readVarInt(stream)
        return ByteArrayInputStream(stream.readNBytes(length))
    }

    fun handshake(nextState: State) {
        val buffer = ByteArrayOutputStream()
        Leb128.writeVarInt(protocolVersion, buffer)
        writeString(buffer, socket.inetAddress.hostAddress)
        buffer.write(socket.port shr 8)
        buffer.write(socket.port and 255)
        buffer.write(nextState.value)

        writePacket(0, buffer.toByteArray())
    }

    fun reopenSocket() {
        socket = Socket(socket.inetAddress.hostAddress, socket.port)
        if (timeout) socket.soTimeout = 500
    }

    @ExperimentalSerializationApi
    fun status(setProtocolVersion: Boolean = true): ServerListPingResponse {
        writePacket(0, ByteArray(0))
        val stream = readPacket()
        val packetId = stream.read()

        if (packetId != 0) throw ProtocolException("Expected response packet id 0 but got $packetId")

        val response: ServerListPingResponse = json.decodeFromString(readString(stream))
        if (setProtocolVersion && response.version.protocol >= 0) protocolVersion = response.version.protocol
        return response
    }

    fun authMode(): AuthMode {
        val buffer = ByteArrayOutputStream()
        writeString(buffer, "mcscan")
        writePacket(0, buffer.toByteArray())
        val response = readPacket()

        return when (response.read()) {
            0 -> {
                val error = readString(response)
                val errorParsed: String = ChatSerialiser.deserializeElement(Json.parseToJsonElement(error))
                return when {
                    errorParsed.contains("Forge") -> AuthMode.FORGE
                    errorParsed.contains("If you wish to use IP forwarding") -> AuthMode.IP_FORWARDING
                    else -> AuthMode.error(errorParsed)
                }
            }
            1 -> AuthMode.ONLINE
            2, 3 -> AuthMode.OFFLINE
            else -> AuthMode.error("Unknown")
        }
    }
}