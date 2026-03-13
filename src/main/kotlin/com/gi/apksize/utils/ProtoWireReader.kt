package com.gi.apksize.utils

/**
 * Minimal protobuf wire-format reader that avoids a runtime dependency on
 * protobuf-java. Supports the subset needed for decoding simple proto
 * messages: varints, length-delimited fields, and skipping fixed-width fields.
 *
 * Usage:
 * ```
 * val reader = ProtoWireReader(bytes)
 * while (reader.hasRemaining()) {
 *     val tag = reader.readTag()
 *     when {
 *         tag.fieldNumber == 1 && tag.wireType == WIRE_LENGTH_DELIMITED -> {
 *             val nested = reader.readBytes()
 *             // parse nested message …
 *         }
 *         else -> reader.skip(tag.wireType)
 *     }
 * }
 * ```
 */
class ProtoWireReader(private val data: ByteArray) {
    var pos = 0

    fun hasRemaining(): Boolean = pos < data.size

    fun readTag(): Tag {
        val v = readVarint()
        return Tag(fieldNumber = (v shr 3).toInt(), wireType = (v and 0x7).toInt())
    }

    fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (pos < data.size) {
            val b = data[pos++].toLong() and 0xFF
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0L) break
            shift += 7
        }
        return result
    }

    fun readBytes(): ByteArray {
        val length = readVarint().toInt()
        val result = data.copyOfRange(pos, pos + length)
        pos += length
        return result
    }

    fun readString(): String = String(readBytes(), Charsets.UTF_8)

    fun skip(wireType: Int) {
        when (wireType) {
            WIRE_VARINT -> readVarint()
            WIRE_64BIT -> pos += 8
            WIRE_LENGTH_DELIMITED -> pos += readVarint().toInt()
            WIRE_32BIT -> pos += 4
        }
    }

    data class Tag(val fieldNumber: Int, val wireType: Int)

    companion object {
        const val WIRE_VARINT = 0
        const val WIRE_64BIT = 1
        const val WIRE_LENGTH_DELIMITED = 2
        const val WIRE_32BIT = 5
    }
}
