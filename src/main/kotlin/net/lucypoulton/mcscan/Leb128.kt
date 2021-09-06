package net.lucypoulton.mcscan

import java.io.InputStream
import java.io.OutputStream
import kotlin.experimental.and


/**
 * Taken from https://wiki.vg/Protocol#VarInt_and_VarLong
 */
object Leb128 {

    fun readVarInt(stream: InputStream): Int {
        var value = 0
        var bitOffset = 0
        var currentByte: Byte
        do {
            if (bitOffset == 35) throw RuntimeException("VarInt is too big")
            currentByte = stream.read().toByte()
            value = value or ((currentByte and 127).toInt() shl bitOffset)
            bitOffset += 7
        } while ((currentByte and 128.toByte()).toInt() != 0)
        return value
    }


    fun writeVarInt(value: Int, stream: OutputStream) {
        var value = value
        while (true) {
            if (value and -0x80 == 0) {
                stream.write(value)
                return
            }
            stream.write(value and 0x7F or 0x80)
            value = value ushr 7
        }
    }
}