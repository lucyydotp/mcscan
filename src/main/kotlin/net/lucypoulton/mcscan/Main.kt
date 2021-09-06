package net.lucypoulton.mcscan

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import de.m3y.kformat.Table
import de.m3y.kformat.table
import java.net.SocketException

class ScanCommand : CliktCommand() {

    val target: String by option("-t", "--target", help = "Target IP").required()

    val portSingle: Int? by option("-p", "--port", help = "Scan a single port")
        .int().restrictTo(0..65535)

    val portRange: Pair<Int, Int> by option("-r", "--range", help = "Scan a range")
        .int().restrictTo(0..65535).pair().default(Pair(25565, 25700))

    val verboseFlag: Boolean by option("-v", "--verbose", help = "Verbose output").flag()

    override fun run() {
        val range: IntRange
        if (portSingle != null) {
            range = portSingle!!..portSingle!!
            println("Scanning target $target, single port $portSingle")

        } else {
            range = portRange.first..portRange.second
            println("Scanning target $target, ports ${portRange.first}-${portRange.second}")
        }
        try {
             val results = Scanner(target, range).scan { result ->
                 if (!verboseFlag && result.authMode is NetworkErrorAuth) return@scan
                print(result.port.toString() + ": ")
                if (result.status?.description != null) {
                    print("MOTD: ${result.status.description} | ${result.status.players.online}/${result.status.players.max} players | ")
                }
                print(if (result.authMode.success) "Auth mode" else "Error:")
                println(" ${result.authMode.reason}")
            }

            println(table {
                header("Port", "MOTD", "Players", "Version", "Auth")

                for (target in results.targets) {
                    if (!verboseFlag && target.authMode is NetworkErrorAuth) continue
                    row(target.port.toString(),
                        target.status?.description?.replace("\n", "\\n") ?: "null",
                        target.status?.players?.online.toString(),
                        target.status?.version?.name ?: "Unknown",
                        target.authMode.reason
                    )
                }

                hints {
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }
            }.render())
        }
        catch (ex : SocketException) {
            println("ERROR: ${ex.localizedMessage}")
        }
    }
}

fun main(args: Array<String>) = ScanCommand().main(args)