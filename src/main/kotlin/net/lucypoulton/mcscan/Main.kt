package net.lucypoulton.mcscan

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import java.net.SocketException

class ScanCommand : CliktCommand() {

    val target: String by option("-t", "--target", help = "Target IP").required()

    val portSingle: Int by option("-p", "--port", help = "Scan a single port")
        .int().restrictTo(0..65535).default(25565)

    val portRange: Pair<Int, Int>? by option("-r", "--range", help = "Scan a range")
        .int().restrictTo(0..65535).pair()

    val verboseFlag: Boolean by option("-v", "--verbose", help = "Verbose output").flag()

    override fun run() {
        val range: IntRange
        if (portRange != null) {
            range = portRange!!.first..portRange!!.second
            println("Scanning target $target, ports ${portRange!!.first}-${portRange!!.second}")
        } else {
            range = portSingle..portSingle
            println("Scanning target $target, single port $portSingle")
        }
        try {
            Scanner(target, range).scan { result ->
                if (!verboseFlag && result.authMode is NetworkErrorAuth) return@scan
                print(result.port.toString() + ": ")
                if (result.status?.description != null) {
                    print("MOTD: ${result.status.description} | ${result.status.players.online}/${result.status.players.max} players | ")
                }
                print(if (result.authMode.success) "Auth mode" else "Error:")
                println(" ${result.authMode.reason}")
            }
        }
        catch (ex : SocketException) {
            println("ERROR: ${ex.localizedMessage}")
        }
    }
}

fun main(args: Array<String>) = ScanCommand().main(args)