package il.ac.technion.cs.softwaredesign

import java.util.*
import kotlin.experimental.and

fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

fun Byte.getBit(position: Int): Int {
    val value = this.toInt()
    return (value shr position) and 1;
}

fun ByteArray.toBitSet() : BitSet {
    val bits = BitSet(8)
    for (i in 0 until this.size * 8) {
        if (this[this.size - i / 8 - 1] and ((1 shl i) % 8).toByte() > 0) {
            bits.set(i)
        }
    }
    return bits
}

fun Boolean.toByte(): Byte {
    if (this){
        return 1
    }
    return 0
}

fun Boolean.toInt(): Int {
    return this.toByte().toInt()
}

fun ByteArray.toInt(): Int {
    var result = 0
    var shift = 0
    for (byte in this) {
        result = result or (byte.toInt() shl shift)
        shift += 8
    }
    return result
}