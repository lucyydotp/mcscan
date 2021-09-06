package net.lucypoulton.mcscan


fun main() {
    val results = Scanner("127.0.0.1").scan()
    for (result in results.targets) {
        println("${result.port}: ${result.authMode.reason} auth, ${result.playerCount} online, MOTD '${result.motd}'")
    }
}