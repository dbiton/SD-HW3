package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

class AggregatingStorage (f: SecureStorageFactory) {
    private var factory: SecureStorageFactory? = f
    private var table: SecureStorage? = null
    private var bytesPerEntry: Int? = null
    private val bytesPerChunk = 100

    fun open(name: String){
        table = factory?.open(name.toByteArray())
    }

    fun setBytesPerEntry(n: Int) {
        bytesPerEntry = n
    }

    fun write(key: Int, data: ByteArray) {
        val entriesPerChunk = bytesPerChunk / bytesPerEntry!!
        val aggregateKey = key / entriesPerChunk
        val begin = key - aggregateKey * entriesPerChunk
        val end = begin + data.size // changed bytesPerEntry to data.size
        var dataStored = table!!.read(byteArrayOfInts(aggregateKey))
        if (dataStored == null){
            dataStored = ByteArray(bytesPerChunk)
        }
        for (i in begin until end){
            dataStored[i] = data[i-begin]
        }
        table!!.write(byteArrayOfInts(aggregateKey), dataStored)
    }

    fun read(key: Int) : ByteArray? {
        val entriesPerChunk = bytesPerChunk / bytesPerEntry!!
        val aggregateKey = key / entriesPerChunk
        val begin = key - aggregateKey * entriesPerChunk
        val end = begin + bytesPerEntry!!
        var dataStored = table!!.read(byteArrayOfInts(aggregateKey)) ?: return null
        return dataStored.slice(begin until end).toByteArray()
    }
}
