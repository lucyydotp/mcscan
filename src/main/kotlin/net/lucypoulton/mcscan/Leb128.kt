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