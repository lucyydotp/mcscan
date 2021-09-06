package net.lucypoulton.mcscan

data class ScanResult(val addr: String, val success: Boolean, val targets: List<ScanTarget>)

data class ScanTarget(val port: UShort, val motd: String?, val authMode: AuthMode, val playerCount: Int )

data class AuthMode(val reason : String, val success: Boolean) {
    companion object {
        val ONLINE = AuthMode("Online", true)
        val OFFLINE = AuthMode("Offline", true)
        val IP_FORWARDING = AuthMode("IP forwarding", true)
        val FORGE = AuthMode("Forge", false)
        fun error(reason: String) = AuthMode(reason, false)
    }
}

class Scanner(val address: String) {

    private var result : ScanResult? = null

    private fun scanPorts(): List<UShort> {
        // TODO actually scan
        return listOf(25565.toUShort())
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

            return ScanTarget(port, response.description, authMode, response.players.online)
        } catch (e: Exception) {
            return ScanTarget(port, null, AuthMode.error(e.localizedMessage), 0)
        }
    }

    fun scan(force: Boolean = false): ScanResult {
        if (result != null && !force) return result as ScanResult

        val ports = scanPorts()

        return ScanResult(address, false, ports.map { p -> testPort(p) })
    }
}