package net.lucypoulton.mcscan

import java.io.InputStream
import java.io.OutputStream

/**
 * Taken from https://wiki.vg/Protocol#VarInt_and_VarLong
 */
object Leb128 {
    fun readVarInt(stream: InputStream): Int {
        var value = 0
        var bitOffset = 0
        var currentByte: Int
        do {
            if (bitOffset == 35) throw RuntimeException("VarInt is too big")
            currentByte = stream.read()
            value = value or ((currentByte and 127) shl bitOffset)
            bitOffset += 7
        } while ((currentByte and 128) != 0)

        return value
    }

    fun writeVarInt(value: Int, stream: OutputStream) {
        var valLocal = value
        while (true) {
            if (valLocal and -0x80 == 0) {
                stream.write(value)
                return
            }
            stream.write(valLocal and 0x7F or 0x80)
            valLocal = value ushr 7
        }
    }
}