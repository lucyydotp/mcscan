package net.lucypoulton.mcscan

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.ProtocolException
import java.net.Socket
import java.net.SocketTimeoutException

class Connection @Throws(ConnectException::class) constructor(private val address: String, private val port: UShort) {

    private var socket: Socket = Socket(address, port.toInt())
    private val json = Json { ignoreUnknownKeys = true }
    private var protocolVersion = 0

    init {
        socket.soTimeout = 500
    }

    enum class State(val value: Int) {
        STATUS(1),
        LOGIN(2)
    }

    private fun writeString(stream: OutputStream, value: String) {
        val bytes = value.encodeToByteArray()

        Leb128.writeVarInt(bytes.size, stream)
        stream.write(bytes)
    }

    private fun readString(stream: InputStream): String {
        val length = Leb128.readVarInt(stream)
        return stream.readNBytes(length).decodeToString()
    }

    private fun writePacket(packetId: Int, data: ByteArray) {
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
    private fun readPacket(): InputStream {
        val stream = socket.getInputStream()
        val length = Leb128.readVarInt(stream)
        return ByteArrayInputStream(stream.readNBytes(length))
    }

    fun handshake(nextState: State) {
        val buffer = ByteArrayOutputStream()
        Leb128.writeVarInt(protocolVersion, buffer)
        writeString(buffer, address)
        buffer.write(port.toInt() shr 8)
        buffer.write((port and 255u).toInt())
        buffer.write(nextState.value)

        writePacket(0, buffer.toByteArray())
    }

    fun reopenSocket() {
        socket = Socket(address, port.toInt())
    }

    @ExperimentalSerializationApi
    fun status(setProtocolVersion: Boolean = true): ServerListPingResponse {
        writePacket(0, ByteArray(0))
        val stream = readPacket()
        val packetId = stream.read()

        if (packetId != 0) throw ProtocolException("Expected response packet id 0 but got $packetId")

        val response: ServerListPingResponse = json.decodeFromString(readString(stream))
        if (setProtocolVersion) protocolVersion = response.version.protocol
        return response
    }

    fun authMode(): AuthMode {
        val buffer = ByteArrayOutputStream()
        writeString(buffer, "username")
        writePacket(0, buffer.toByteArray())
        val response = readPacket()

        return when (response.read()) {
            0 -> {
                val error = readString(response)
                val errorParsed = ChatSerialiser.deserializeElement(Json.parseToJsonElement(error))
                return AuthMode.error(errorParsed)
            }
            1 -> AuthMode.ONLINE
            2 -> AuthMode.OFFLINE
            else -> AuthMode.error("Unknown")
        }
    }
}