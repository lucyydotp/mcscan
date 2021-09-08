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

package net.lucypoulton.mcscan.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import de.m3y.kformat.Table
import de.m3y.kformat.table
import net.lucypoulton.mcscan.NetworkErrorAuth
import net.lucypoulton.mcscan.Scanner
import net.lucypoulton.mcscan.ServerListPingResponse
import net.lucypoulton.mcscan.limitLength
import java.net.SocketException
import kotlin.math.roundToInt

class McscanCommand : CliktCommand() {

    private val target: String by option("-t", "--target", help = "Target IP").required()

    private val portSingle: Int? by option("-p", "--port", help = "Scan a single port")
        .int().restrictTo(0..65535)

    private val portRange: Pair<Int, Int> by option("-r", "--range", help = "Scan a range")
        .int().restrictTo(0..65535).pair().default(Pair(25565, 25700))

    private val verboseFlag: Boolean by option("-v", "--verbose", help = "Verbose output").flag()

    private fun formatVersion(version: ServerListPingResponse.ServerVersion?): String {
        if (version == null) return "Unknown"
        return "${version.name.limitLength(50)} (${version.protocol})"
    }

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
                val progress =  10 * (result.port.toInt() - range.first).toFloat() / (range.last - range.first)
                print("Scanning [${"#".repeat(progress.roundToInt())}${" ".repeat(10 - progress.roundToInt())}]: ${String.format("%.1f", progress * 10)}%  \r")
            }
            println()

            if (!results.targets.any { target -> target.authMode.success }) {
                println("No servers discovered")
                return
            }

            println(table {
                header("Port", "MOTD", "Players", "Version", "Auth")

                for (target in results.targets) {
                    if (!verboseFlag && target.authMode is NetworkErrorAuth) continue
                    row(target.port.toString(),

                        target.status?.description?.
                        replace("\n", "\\n")?.
                        replace(Regex("\u00a7."), "")?.
                        limitLength(100) ?: "null",

                        target.status?.players?.online.toString(),
                        formatVersion(target.status?.version),
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