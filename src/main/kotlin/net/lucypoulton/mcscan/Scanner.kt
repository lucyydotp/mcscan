package net.lucypoulton.mcscan

import java.util.*

data class ScanResult(val addr: String, val success: Boolean, val targets: List<ScanTarget>)

data class ScanTarget(val port: UShort, val authMode: AuthMode, val status: ServerListPingResponse?)

open class AuthMode(val reason: String, val success: Boolean) {

    override fun hashCode() = Objects.hash(reason, success)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AuthMode
        if (reason != other.reason || success != other.success) return false
        return true
    }

    companion object {
        val ONLINE = AuthMode("Online", true)
        val OFFLINE = AuthMode("Offline", true)
        val IP_FORWARDING = AuthMode("IP forwarding", true)
        val FORGE = AuthMode("Forge", false)
        fun error(reason: String) = AuthMode(reason, false)
    }
}

class NetworkErrorAuth(ex: Exception) : AuthMode(ex.localizedMessage, false)

class Scanner(val address: String, val portRange: IntRange) {

    private var result: ScanResult? = null

    private fun scanPorts(): List<UShort> {
        // TODO actually scan
        return portRange.toList().map { i -> i.toUShort() }
    }

    private fun testPort(port: UShort): ScanTarget {
        val conn: Connection
        try {
            conn = Connection(address, port)
            conn.handshake(Connection.State.STATUS)
            val response = conn.status()

            conn.reopenSocket()
            conn.handshake(Connection.State.LOGIN)
            val authMode = conn.authMode()

            return ScanTarget(port, authMode, response)
        } catch (e: Exception) {
            return ScanTarget(
                port, NetworkErrorAuth(e), null
            )
        }
    }

    fun scan(force: Boolean = false, onDiscover: ((ScanTarget) -> Unit)? = null): ScanResult {
        if (result != null && !force) return result as ScanResult

        val ports = scanPorts()
        val results: MutableList<ScanTarget> = mutableListOf()

        ports.forEach { p ->
            val result = testPort(p)
            results.add(result)
            onDiscover?.invoke(result)
        }

        return ScanResult(address, false, results)
    }
}